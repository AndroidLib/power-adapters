package com.nextfaze.poweradapters.sample;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.collect.ImmutableList;
import com.nextfaze.poweradapters.Holder;
import com.nextfaze.poweradapters.binding.Binder;
import com.nextfaze.poweradapters.binding.ViewHolder;
import com.nextfaze.poweradapters.binding.ViewHolderBinder;
import com.nextfaze.poweradapters.data.DataBindingAdapter;
import com.nextfaze.poweradapters.data.widget.DataLayout;
import lombok.NonNull;

import java.util.List;
import java.util.Random;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;
import static com.nextfaze.poweradapters.recyclerview.RecyclerPowerAdapters.toRecyclerAdapter;

public final class FilePeekView extends RelativeLayout {

    private static final List<Integer> ICON_RESOURCES = ImmutableList.of(
            R.drawable.file_peek_view_icon_0,
            R.drawable.file_peek_view_icon_1,
            R.drawable.file_peek_view_icon_2
    );

    @NonNull
    private final Binder<File, View> mBinder = new ViewHolderBinder<File, ItemViewHolder>(R.layout.file_peek_view_item) {
        @NonNull
        @Override
        protected ItemViewHolder newViewHolder(@NonNull View v) {
            return new ItemViewHolder(v);
        }

        @Override
        protected void bindViewHolder(@NonNull File file,
                                      @NonNull ItemViewHolder itemViewHolder,
                                      @NonNull Holder holder) {
            itemViewHolder.imageView.setImageResource(randomIconResource(file));
            itemViewHolder.titleView.setText(file.getName());
        }
    };

    @BindView(R.id.data_layout)
    DataLayout mDataLayout;

    @BindView(R.id.recycler)
    RecyclerView mRecyclerView;

    public FilePeekView(Context context) {
        this(context, null);
    }

    public FilePeekView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FilePeekView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.file_peek_view, this);
        ButterKnife.bind(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, HORIZONTAL, false));
    }

    public void setFile(@NonNull File file) {
        DirectoryData data = new DirectoryData(file);
        mRecyclerView.setAdapter(toRecyclerAdapter(new DataBindingAdapter(data, mBinder)));
        mDataLayout.setData(data);
    }

    @DrawableRes
    private static int randomIconResource(@NonNull File file) {
        return ICON_RESOURCES.get(new Random(file.getName().hashCode()).nextInt(ICON_RESOURCES.size()));
    }

    static final class ItemViewHolder extends ViewHolder {

        @BindView(R.id.image)
        ImageView imageView;

        @BindView(R.id.title)
        TextView titleView;

        ItemViewHolder(@NonNull View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
