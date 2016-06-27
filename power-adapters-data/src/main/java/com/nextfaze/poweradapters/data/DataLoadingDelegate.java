package com.nextfaze.poweradapters.data;

import com.nextfaze.poweradapters.DataObserver;
import com.nextfaze.poweradapters.LoadingAdapterBuilder;
import com.nextfaze.poweradapters.SimpleDataObserver;
import lombok.NonNull;

@Deprecated
public final class DataLoadingDelegate extends LoadingAdapterBuilder.Delegate {

    @NonNull
    private final Data<?> mData;

    @NonNull
    private final DataObserver mDataObserver = new SimpleDataObserver() {
        @Override
        public void onChanged() {
            notifyEmptyChanged();
        }
    };

    @NonNull
    private final com.nextfaze.poweradapters.data.LoadingObserver mLoadingObserver = new com.nextfaze.poweradapters.data.LoadingObserver() {
        @Override
        public void onLoadingChange() {
            notifyLoadingChanged();
        }
    };

    public DataLoadingDelegate(@NonNull Data<?> data) {
        mData = data;
    }

    @Override
    protected boolean isLoading() {
        return mData.isLoading();
    }

    @Override
    protected boolean isEmpty() {
        return mData.isEmpty();
    }

    @Override
    protected void onFirstObserverRegistered() {
        mData.registerLoadingObserver(mLoadingObserver);
        mData.registerDataObserver(mDataObserver);
    }

    @Override
    protected void onLastObserverUnregistered() {
        mData.unregisterLoadingObserver(mLoadingObserver);
        mData.unregisterDataObserver(mDataObserver);
    }
}