package com.nextfaze.databind;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.max;
import static java.lang.Thread.currentThread;

/**
 * Mutable {@link Data} implementation backed by an {@link ArrayList}, which is loaded incrementally until the source
 * has no more data. Cannot contain {@code null} elements. Not thread-safe.
 * @param <T> The type of element this data contains.
 */
@Accessors(prefix = "m")
public abstract class IncrementalArrayData<T> extends AbstractData<T> implements MutableData<T> {

    private static final Logger log = LoggerFactory.getLogger(IncrementalArrayData.class);

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("Incremental Array Data Thread %d");

    @NonNull
    private final ArrayList<T> mData = new ArrayList<>();

    @NonNull
    private final ThreadFactory mThreadFactory;

    @NonNull
    private final Lock mLock = new ReentrantLock();

    @NonNull
    private final Condition mLoad = mLock.newCondition();

    /** The number of rows to look ahead before loading. */
    @Getter
    private volatile int mLookAheadRowCount = 5;

    @Nullable
    private Thread mThread;

    /** Automatically invalidate contents if data is hidden for the specified duration. */
    @Getter
    @Setter
    private long mAutoInvalidateDelay = Long.MAX_VALUE;

    /** Indicates the last attempt to load a page failed. */
    private volatile boolean mError;

    private boolean mLoading;
    private boolean mDirty = true;

    protected IncrementalArrayData() {
        this(DEFAULT_THREAD_FACTORY);
    }

    protected IncrementalArrayData(@NonNull ThreadFactory threadFactory) {
        mThreadFactory = threadFactory;
    }

    /** Subclasses must call through to super. */
    @Override
    protected void onClose() throws Exception {
        stopThread();
    }

    @Override
    public final int size() {
        return mData.size();
    }

    @Override
    public final boolean isEmpty() {
        return mData.isEmpty();
    }

    @Override
    public final boolean contains(Object object) {
        return mData.contains(object);
    }

    @Override
    public final int indexOf(Object object) {
        return mData.indexOf(object);
    }

    @Override
    public final int lastIndexOf(Object object) {
        return mData.lastIndexOf(object);
    }

    @Override
    public final T remove(int index) {
        T removed = mData.remove(index);
        notifyDataChanged();
        return removed;
    }

    @Override
    public final boolean add(@NonNull T t) {
        if (mData.add(t)) {
            notifyDataChanged();
            return true;
        }
        return false;
    }

    @Override
    public final void add(int index, T object) {
        mData.add(index, object);
        notifyDataChanged();
    }

    @Override
    public final boolean addAll(Collection<? extends T> collection) {
        boolean changed = mData.addAll(collection);
        if (changed) {
            notifyDataChanged();
        }
        return changed;
    }

    @Override
    public final boolean addAll(int index, Collection<? extends T> collection) {
        boolean changed = mData.addAll(index, collection);
        if (changed) {
            notifyDataChanged();
        }
        return changed;
    }

    @Override
    public final boolean remove(@NonNull Object obj) {
        if (mData.remove(obj)) {
            notifyDataChanged();
            return true;
        }
        return false;
    }

    // TODO: Notify of change if modified from iterator.

    @NonNull
    @Override
    public final ListIterator<T> listIterator() {
        return mData.listIterator();
    }

    @NonNull
    @Override
    public final ListIterator<T> listIterator(int location) {
        return mData.listIterator(location);
    }

    @NonNull
    @Override
    public final List<T> subList(int start, int end) {
        return mData.subList(start, end);
    }

    @Override
    public final boolean containsAll(@NonNull Collection<?> collection) {
        return mData.containsAll(collection);
    }

    @Override
    public final boolean removeAll(@NonNull Collection<?> collection) {
        boolean removed = mData.removeAll(collection);
        if (removed) {
            notifyDataChanged();
        }
        return removed;
    }

    @Override
    public final boolean retainAll(@NonNull Collection<?> collection) {
        boolean changed = mData.retainAll(collection);
        if (changed) {
            notifyDataChanged();
        }
        return changed;
    }

    @Override
    public final T set(int index, T object) {
        T t = mData.set(index, object);
        notifyDataChanged();
        return t;
    }

    @NonNull
    @Override
    public final Object[] toArray() {
        return mData.toArray();
    }

    @NonNull
    @Override
    public final <T> T[] toArray(@NonNull T[] contents) {
        return mData.toArray(contents);
    }

    @NonNull
    @Override
    public final T get(int position, int flags) {
        // Requested end of data? Time to load more.
        if ((flags & FLAG_PRESENTATION) != 0 && position >= size() - mLookAheadRowCount) {
            proceed();
        }
        return mData.get(position);
    }

    /** Clears the contents, and starts loading again if data is currently shown. */
    @Override
    public final void clear() {
        clearDataAndNotify();
        stopThread();
        startThreadIfNeeded();
    }

    @Override
    public final boolean isLoading() {
        return mLoading;
    }

    /** Flags the data to be cleared and reloaded next time it is "shown". */
    @Deprecated
    public final void invalidateDeferred() {
        mDirty = true;
    }

    /** Flags the data to be cleared and reloaded next time it is "shown". */
    @Override
    public void invalidate() {
        invalidateDeferred();
    }

    public final void loadNext() {
        proceed();
    }

    public final void setLookAheadRowCount(int lookAheadRowCount) {
        mLookAheadRowCount = max(0, lookAheadRowCount);
    }

    @Override
    protected final void onShown(long millisHidden) {
        log.trace("Shown after being hidden for {} ms", millisHidden);
        if (mError) {
            // Last attempt to load a page failed, so try again now we've become visible again.
            proceed();
        }
        if (millisHidden >= mAutoInvalidateDelay) {
            log.trace("Automatically invalidating due to auto-invalidate delay being reached or exceeded");
            mDirty = true;
        }
        if (mDirty) {
            // Data is dirty, so reload everything.
            clearDataAndNotify();
            stopThread();
        }
        startThreadIfNeeded();
    }

    /**
     * Load the next set of items.
     * @return A list containing the next set of items to be appended, or {@code null} if there are no more items.
     * @throws Throwable If any error occurs while trying to load.
     */
    @Nullable
    protected abstract List<? extends T> load() throws Throwable;

    /** Called when data is invalidated or cleared. May happen on any thread. */
    protected void onInvalidate() {
    }

    private void appendNonNullElements(@NonNull List<? extends T> list) {
        for (T t : list) {
            if (t != null) {
                mData.add(t);
            }
        }
    }

    private void clearDataAndNotify() {
        if (size() > 0) {
            mData.clear();
            notifyDataChanged();
        }
        onInvalidate();
    }

    private void startThreadIfNeeded() {
        if (mThread == null && isShown()) {
            log.trace("Starting thread");
            mDirty = false;
            setLoading(true);
            mThread = mThreadFactory.newThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadLoop();
                    } catch (InterruptedException e) {
                        // Normal thread termination.
                        log.trace("Thread terminated normally with an InterruptedException");
                    }
                }
            });
            mThread.start();
        }
    }

    private void stopThread() {
        if (mThread != null) {
            log.trace("Stopping thread");
            mThread.interrupt();
            mThread = null;
        }
    }

    /** Loads each page until full range has been loading, halting in between pages until instructed to proceed. */
    private void loadLoop() throws InterruptedException {
        log.trace("Start load loop");
        boolean hasMore = true;

        // Loop until all loaded.
        while (hasMore) {
            // Thread interruptions terminate the loop.
            if (currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            try {
                setLoading(true);

                log.trace("Loading next chunk of items");

                // Load next items.
                final List<? extends T> items = load();
                hasMore = items != null;

                // Store items and notify of change.
                if (items != null && !items.isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendNonNullElements(items);
                            notifyDataChanged();
                        }
                    });
                }
            } catch (InterruptedException | InterruptedIOException e) {
                throw new InterruptedException();
            } catch (Throwable e) {
                log.error("Error loading", e);
                notifyError(e);
                mError = true;
            } finally {
                setLoading(false);
            }

            // Block until instructed to continue, even if an error occurred.
            // In this case, loading must be explicitly resumed.
            block();
        }
    }

    private void setLoading(final boolean loading) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLoading != loading) {
                    mLoading = loading;
                    notifyLoadingChanged();
                }
            }
        });
    }

    private void block() throws InterruptedException {
        mLock.lock();
        try {
            mLoad.await();
        } finally {
            mLock.unlock();
        }
    }

    private void proceed() {
        mError = false;
        mLock.lock();
        try {
            mLoad.signal();
        } finally {
            mLock.unlock();
        }
    }
}