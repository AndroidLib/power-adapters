package com.nextfaze.poweradapters.asyncdata;

import com.nextfaze.poweradapters.EmptyAdapterBuilder;
import com.nextfaze.powerdata.Data;
import lombok.NonNull;

public final class DataEmptyDelegate extends EmptyAdapterBuilder.Delegate {

    @NonNull
    private final Data<?> mData;

    @NonNull
    private final LoadingPolicy mLoadingPolicy;

    @NonNull
    private final com.nextfaze.powerdata.DataObserver mDataObserver = new com.nextfaze.powerdata.SimpleDataObserver() {
        @Override
        public void onChange() {
            notifyEmptyChanged();
        }
    };

    @NonNull
    private final com.nextfaze.powerdata.LoadingObserver mLoadingObserver = new com.nextfaze.powerdata.LoadingObserver() {
        @Override
        public void onLoadingChange() {
            notifyEmptyChanged();
        }
    };

    public DataEmptyDelegate(@NonNull Data<?> data) {
        this(data, LoadingPolicy.HIDE);
    }

    public DataEmptyDelegate(@NonNull Data<?> data, @NonNull LoadingPolicy loadingPolicy) {
        mData = data;
        mLoadingPolicy = loadingPolicy;
    }

    @Override
    protected boolean isEmpty() {
        return mLoadingPolicy.shouldShow(mData);
    }

    @Override
    protected void onFirstObserverRegistered() {
        super.onFirstObserverRegistered();
        mData.registerDataObserver(mDataObserver);
        mData.registerLoadingObserver(mLoadingObserver);
    }

    @Override
    protected void onLastObserverUnregistered() {
        super.onLastObserverUnregistered();
        mData.unregisterDataObserver(mDataObserver);
        mData.unregisterLoadingObserver(mLoadingObserver);
    }

    public enum LoadingPolicy {
        /** Empty item will be shown even while {@link Data} is loading. */
        SHOW {
            @Override
            boolean shouldShow(@NonNull Data<?> data) {
                return data.isEmpty();
            }
        },
        /** Empty item will not be shown while {@link Data} is loading. */
        HIDE {
            @Override
            boolean shouldShow(@NonNull Data<?> data) {
                return !data.isLoading() && data.isEmpty();
            }
        };

        abstract boolean shouldShow(@NonNull Data<?> data);
    }
}