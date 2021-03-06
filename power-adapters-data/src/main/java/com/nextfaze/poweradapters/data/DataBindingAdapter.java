package com.nextfaze.poweradapters.data;

import com.nextfaze.poweradapters.DataObserver;
import com.nextfaze.poweradapters.binding.Binder;
import com.nextfaze.poweradapters.binding.BindingAdapter;
import com.nextfaze.poweradapters.binding.Mapper;
import lombok.NonNull;

import static com.nextfaze.poweradapters.binding.Mappers.singletonMapper;

public final class DataBindingAdapter extends BindingAdapter {

    @NonNull
    private final DataObserver mDataObserver = new DataObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            notifyItemRangeMoved(fromPosition, toPosition, itemCount);
        }
    };

    @NonNull
    private final Data<?> mData;

    public DataBindingAdapter(@NonNull Data<?> data, @NonNull Binder<?, ?> binder) {
        this(data, singletonMapper(binder));
    }

    public DataBindingAdapter(@NonNull Data<?> data, @NonNull Mapper mapper) {
        super(mapper);
        mData = data;
    }

    @NonNull
    @Override
    protected Object getItem(int position) {
        return mData.get(position, Data.FLAG_PRESENTATION);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    protected void onFirstObserverRegistered() {
        super.onFirstObserverRegistered();
        mData.registerDataObserver(mDataObserver);
    }

    @Override
    protected void onLastObserverUnregistered() {
        super.onLastObserverUnregistered();
        mData.unregisterDataObserver(mDataObserver);
    }
}
