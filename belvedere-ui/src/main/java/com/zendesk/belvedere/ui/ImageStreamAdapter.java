package com.zendesk.belvedere.ui;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


class ImageStreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Delegate delegate;
    private final List<Item> items;

    ImageStreamAdapter(Delegate delegate, List<Uri> images) {
        setHasStableIds(true);
        final List<Item> items = new ArrayList<>();
        items.add(StaticItem.forCamera(delegate));

        for (Uri uri : images) {
            items.add(new StreamItemImage(delegate, uri));
        }

        this.delegate = delegate;
        this.items = items;
    }

    ImageStreamAdapter(Delegate delegate) {
        setHasStableIds(true);
        this.delegate = delegate;
        this.items = Arrays.asList(StaticItem.forCamera(delegate), StaticItem.forGallery(delegate));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new RecyclerView.ViewHolder(v){};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        items.get(position).bind(holder.itemView);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getLayoutId();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    interface Item {
        int getLayoutId();
        long getId();
        void bind(View view);
    }

    private static class StreamItemImage implements Item {

        private final Uri uri;
        private final int layoutId;
        private final long id;
        private final Delegate delegate;

        private StreamItemImage(Delegate delegate, Uri uri) {
            this.delegate = delegate;
            this.uri = uri;
            this.layoutId = R.layout.list_item;
            this.id = UUID.randomUUID().hashCode();
        }

        @Override
        public int getLayoutId() {
            return layoutId;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public void bind(View view) {
            final ImageView imageView = (ImageView) view.findViewById(R.id.list_item_image);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.imagesSelected(Collections.singletonList(uri));
                }
            });

            Picasso.with(imageView.getContext())
                    .load(uri)
                    .resize(250, 0)
                    .onlyScaleDown()
                    .into(imageView);
        }
    }

    private static class StaticItem implements Item {

        static Item forCamera(final Delegate delegate) {
            return new StaticItem(R.drawable.ic_photo_camera_black_24dp, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.openCamera();
                }
            });
        }

        static Item forGallery(final Delegate delegate) {
            return new StaticItem(R.drawable.ic_image, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.openGallery();
                }
            });
        }

        private final long id;
        private final int iconId;
        private final View.OnClickListener onClickListener;

        private StaticItem(int iconId, View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
            this.iconId = iconId;
            this.id = UUID.randomUUID().hashCode();
        }

        @Override
        public int getLayoutId() {
            return R.layout.list_item_camera;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public void bind(View view) {
            ((ImageView)view.findViewById(R.id.list_item_static_image)).setImageResource(iconId);
            view.findViewById(R.id.list_item_static_click_area).setOnClickListener(onClickListener);
        }
    }

    interface Delegate {
        void imagesSelected(List<Uri> uris);
        void openCamera();
        void openGallery();
    }
}
