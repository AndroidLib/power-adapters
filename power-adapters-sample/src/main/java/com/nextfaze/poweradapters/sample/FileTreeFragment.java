package com.nextfaze.poweradapters.sample;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.nextfaze.asyncdata.Data;
import com.nextfaze.asyncdata.widget.DataLayout;
import com.nextfaze.poweradapters.DividerAdapterBuilder;
import com.nextfaze.poweradapters.EmptyAdapterBuilder;
import com.nextfaze.poweradapters.HeaderAdapterBuilder;
import com.nextfaze.poweradapters.Holder;
import com.nextfaze.poweradapters.LoadingAdapterBuilder;
import com.nextfaze.poweradapters.PowerAdapter;
import com.nextfaze.poweradapters.TreeAdapter;
import com.nextfaze.poweradapters.asyncdata.DataBindingAdapter;
import com.nextfaze.poweradapters.asyncdata.DataEmptyDelegate;
import com.nextfaze.poweradapters.asyncdata.DataLoadingDelegate;
import com.nextfaze.poweradapters.binding.Binder;
import com.nextfaze.poweradapters.binding.TypedBinder;
import lombok.NonNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static android.graphics.Typeface.DEFAULT;
import static android.graphics.Typeface.DEFAULT_BOLD;
import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static com.google.common.base.Strings.repeat;
import static com.nextfaze.poweradapters.DividerAdapterBuilder.EmptyPolicy.SHOW_NOTHING;
import static com.nextfaze.poweradapters.LoadingAdapterBuilder.EmptyPolicy.SHOW_ALWAYS;
import static com.nextfaze.poweradapters.LoadingAdapterBuilder.EmptyPolicy.SHOW_ONLY_IF_NON_EMPTY;
import static com.nextfaze.poweradapters.binding.Mappers.singletonMapper;
import static com.nextfaze.poweradapters.recyclerview.RecyclerPowerAdapters.toRecyclerAdapter;

public class FileTreeFragment extends BaseFragment {

    private static final int MAX_DISPLAYED_FILES_PER_DIR = Integer.MAX_VALUE;

    @NonNull
    private final File mRootFile = new File("/");

    @Bind(R.id.data_layout)
    DataLayout mDataLayout;

    @Bind(R.id.recycler)
    RecyclerView mRecyclerView;

    @Nullable
    private Data<File> mRootData;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        PowerAdapter adapter = concat(
//                createFilesAdapterSimple(mRootFile, 0, true),
//                createFilesAdapter(mRootData, mRootFile, 0)
//        );
        PowerAdapter adapter = createFilesAdapter(mRootFile, 0);
        adapter = new DividerAdapterBuilder()
                .leadingResource(R.layout.list_divider_item)
                .trailingResource(R.layout.list_divider_item)
                .innerResource(R.layout.list_divider_item)
                .emptyPolicy(SHOW_NOTHING)
                .build(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), VERTICAL, false));
        mRecyclerView.setAdapter(toRecyclerAdapter(adapter));
        showCollectionView(CollectionView.RECYCLER_VIEW);
    }

    @NonNull
    private PowerAdapter createFilesAdapterSimple(@NonNull final File file, final int depth, final boolean tree) {
        final AtomicReference<TreeAdapter> treeAdapterRef = new AtomicReference<>();
        Binder binder = new TypedBinder<File, TextView>(android.R.layout.simple_list_item_1) {
            @Override
            protected void bind(@NonNull final File file, @NonNull TextView v, @NonNull final Holder holder) {
                String label = file.isDirectory() ? file.getName() + "/" : file.getName();
                v.setText(repeat("    ", depth) + label);
                v.setTypeface(file.isDirectory() ? DEFAULT_BOLD : DEFAULT);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tree && file.isDirectory()) {
                            treeAdapterRef.get().toggleExpanded(holder.getPosition());
                        }
                    }
                });
            }
        };
        File[] filesArray = file.listFiles();
        final List<File> files = FluentIterable.from(filesArray != null ? Lists.newArrayList(filesArray) : Collections.<File>emptyList())
                .limit(MAX_DISPLAYED_FILES_PER_DIR)
                .toList();
        PowerAdapter adapter = new FileAdapter(files, singletonMapper(binder));
        treeAdapterRef.set(new TreeAdapter(adapter) {
            @NonNull
            @Override
            protected PowerAdapter getChildAdapter(int position) {
                File file = files.get(position);
                return createFilesAdapterSimple(file, depth + 1, true);
            }
        });
        if (tree) {
            adapter = treeAdapterRef.get();
        }
        return adapter;
    }

    @NonNull
    private PowerAdapter createFilesAdapter(@NonNull final File file, final int depth) {
        final Data<File> data = new DirectoryData(file, MAX_DISPLAYED_FILES_PER_DIR);
        if (mRootData == null) {
            mRootData = data;
            mDataLayout.setData(data);
        }
        final AtomicReference<TreeAdapter> treeAdapterRef = new AtomicReference<>();
        Binder binder = new TypedBinder<File, TextView>(android.R.layout.simple_list_item_1) {
            @Override
            protected void bind(@NonNull final File file, @NonNull TextView v, @NonNull final Holder holder) {
                String label = file.isDirectory() ? file.getName() + "/" : file.getName();
                v.setText(repeat("    ", depth) + label);
                v.setTypeface(file.isDirectory() ? DEFAULT_BOLD : DEFAULT);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (file.isDirectory()) {
                            treeAdapterRef.get().toggleExpanded(holder.getPosition());
                        }
                    }
                });
            }
        };
        PowerAdapter adapter = new DataBindingAdapter(data, singletonMapper(binder));
        treeAdapterRef.set(new TreeAdapter(adapter) {
            @NonNull
            @Override
            protected PowerAdapter getChildAdapter(int position) {
                File file = data.get(position);
                // TODO: Call Show/hide from an adapter, based on registered observers?
                data.notifyShown();
                return createFilesAdapter(file, depth + 1);
            }
        });
        adapter = treeAdapterRef.get();

        adapter = new LoadingAdapterBuilder()
                .resource(R.layout.list_loading_item)
                .emptyPolicy(depth == 0 ? SHOW_ONLY_IF_NON_EMPTY : SHOW_ALWAYS)
                .build(adapter, new DataLoadingDelegate(data));

        adapter = new EmptyAdapterBuilder()
                .resource(R.layout.file_list_empty_item)
                .build(adapter, new DataEmptyDelegate(data));

        TextView headerView = (TextView) LayoutInflater.from(getActivity())
                .inflate(android.R.layout.simple_list_item_1, mRecyclerView, false);
        headerView.setBackgroundColor(0x20FFFFFF);
        headerView.setText(repeat("    ", depth) + file.getName() + ":");
        headerView.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

        adapter = new HeaderAdapterBuilder()
                .addView(headerView)
                .build(adapter);

        return adapter;
    }
}