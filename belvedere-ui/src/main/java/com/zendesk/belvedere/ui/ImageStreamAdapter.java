package com.zendesk.belvedere.ui;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


class ImageStreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Item> items;

    ImageStreamAdapter(Delegate delegate, List<Uri> images, boolean showCamera) {
        setHasStableIds(true);
        final List<Item> items = new ArrayList<>();

        if(showCamera) {
            items.add(StaticItem.forCameraSquare(delegate));
        }

        for (Uri uri : images) {
            items.add(new StreamItemImage(delegate, uri));
        }

        this.items = items;
    }

    ImageStreamAdapter(Delegate delegate) {
        setHasStableIds(true);
        this.items = Arrays.asList(
                StaticItem.forCameraList(delegate),
                StaticItem.forDocumentList(delegate));
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

    void hideCameraOption() {
        List<Item> list = new ArrayList<>();
        for(Item item : items) {
            if (!(item instanceof StaticItem) || ((StaticItem)item).getType() != StaticItem.TYPE_CAMERA) {
                list.add(item);
            }
        }
        this.items = list;
        notifyDataSetChanged();
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
            final int itemWith = view.getContext().getResources().getDimensionPixelSize(R.dimen.image_width);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.imagesSelected(Collections.singletonList(uri));
                }
            });

            Picasso.with(imageView.getContext())
                    .load(uri)
                    .resize(itemWith, 0)
                    .onlyScaleDown()
                    .into(imageView);
        }
    }

    private static class StaticItem implements Item {

        private final static int TYPE_CAMERA = 1;
        private final static int TYPE_GALLERY = 2;

        private final static int PIC_CAMERA = R.drawable.ic_camera_black;
        private final static int PIC_DOCUMENT = R.drawable.ic_image_black;

        private final static int LAYOUT_GRID = R.layout.list_item_grid_static;
        private final static int LAYOUT_LIST = R.layout.list_item_static;

        private final static int TEXT_CAMERA = R.string.belvedere_dialog_camera;
        private final static int TEXT_DOCUMENT = R.string.belvedere_dialog_gallery;

        static Item forCameraSquare(final Delegate delegate) {
            return new StaticItem(LAYOUT_GRID, PIC_CAMERA, -1, TYPE_CAMERA, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.openCamera();
                }
            });
        }

        static Item forCameraList(final Delegate delegate) {
            return new StaticItem(LAYOUT_LIST, PIC_CAMERA, TEXT_CAMERA, TYPE_CAMERA, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.openCamera();
                }
            });
        }

        static Item forDocumentList(final Delegate delegate) {
            return new StaticItem(LAYOUT_LIST, PIC_DOCUMENT, TEXT_DOCUMENT, TYPE_GALLERY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delegate.openGallery();
                }
            });
        }

        private final long id;
        private final int iconId, layoutId, textId, type;
        private final View.OnClickListener onClickListener;

        private StaticItem(int layoutId, int iconId, int textId, int type, View.OnClickListener onClickListener) {
            this.layoutId = layoutId;
            this.iconId = iconId;
            this.textId = textId;
            this.type = type;
            this.onClickListener = onClickListener;
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
            ((ImageView)view.findViewById(R.id.list_item_static_image)).setImageResource(iconId);
            view.findViewById(R.id.list_item_static_click_area).setOnClickListener(onClickListener);

            final View textView = view.findViewById(R.id.list_item_static_text);
            if(textView != null) {
                ((TextView)textView).setText(textId);
            }
        }

        int getType() {
            return type;
        }
    }

    interface Delegate {
        void imagesSelected(List<Uri> uris);
        void openCamera();
        void openGallery();
    }
}
