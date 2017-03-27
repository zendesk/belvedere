package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import zendesk.belvedere.ui.R;

class ImageStreamItems {

    private final static int TYPE_CAMERA = 1;
    private final static int TYPE_GALLERY = 2;

    private final static int PIC_CAMERA = R.drawable.belvedere_ic_camera_black;
    private final static int PIC_DOCUMENT = R.drawable.belvedere_ic_image_black;

    private final static int LAYOUT_GRID = R.layout.stream_list_item_square_static;
    private final static int LAYOUT_LIST = R.layout.stream_list_item_static;

    private final static int TEXT_CAMERA = R.string.belvedere_dialog_camera;
    private final static int TEXT_DOCUMENT = R.string.belvedere_dialog_gallery;

    static List<StreamItemImage> fromUris(List<Uri> uris, ImageStreamAdapter.Delegate delegate, Context context) {
        List<StreamItemImage> items = new ArrayList<>(uris.size());
        for(Uri uri : uris) {
            items.add(new StreamItemImage(delegate, uri, context));
        }
        return items;
    }

    static StaticItem forCameraSquare(final ImageStreamAdapter.Delegate delegate) {
        return new StaticItem(LAYOUT_GRID, PIC_CAMERA, -1, TYPE_CAMERA, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delegate.openCamera();
            }
        });
    }

    static StaticItem forCameraList(final ImageStreamAdapter.Delegate delegate) {
        return new StaticItem(LAYOUT_LIST, PIC_CAMERA, TEXT_CAMERA, TYPE_CAMERA, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delegate.openCamera();
            }
        });
    }

    static StaticItem forDocumentList(final ImageStreamAdapter.Delegate delegate) {
        return new StaticItem(LAYOUT_LIST, PIC_DOCUMENT, TEXT_DOCUMENT, TYPE_GALLERY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delegate.openGallery();
            }
        });
    }

    static abstract class Item {

        private final int layoutId;
        private final long id;
        private boolean isSelected;

        Item(int layoutId) {
            this.layoutId = layoutId;
            this.id = UUID.randomUUID().hashCode();
            this.isSelected = false;
        }

        int getLayoutId() {
            return layoutId;
        }

        long getId() {
            return id;
        }

        abstract void bind(View view);

        void setSelected(boolean selected) {
            this.isSelected = selected;
        }

        boolean isSelected() {
            return isSelected;
        }
    }

    static class StreamItemImage extends Item {

        private static final String LOG_TAG = "StreamItemImage";

        private final Uri uri;
        private final ImageStreamAdapter.Delegate delegate;

        private int h = -1, w = -1;

        private final int itemWidth;
        private final int padding;
        private final int paddingSelectedHorizontal;

        StreamItemImage(ImageStreamAdapter.Delegate delegate, Uri uri, Context context) {
            super(R.layout.stream_list_item);
            this.delegate = delegate;
            this.uri = uri;

            this.padding = context.getResources().getDimensionPixelOffset(R.dimen.image_stream_image_padding);
            this.paddingSelectedHorizontal = context.getResources().getDimensionPixelOffset(R.dimen.image_stream_image_padding_selected);
            this.itemWidth = context.getResources().getDimensionPixelSize(R.dimen.image_stream_image_width);
        }

        @Override
        public void bind(View view) {
            L.d(LOG_TAG, getUri() + " bind " + h + " " + w);

            final ImageView imageView = (ImageView) view.findViewById(R.id.list_item_image);
            final View container = view.findViewById(R.id.list_item_container);

            if(isSelected()) {
                view.findViewById(R.id.list_item_image_overlay).setVisibility(View.VISIBLE);

            } else {
                view.findViewById(R.id.list_item_image_overlay).setVisibility(View.GONE);
            }

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelected(!isSelected());
                    delegate.setSelected(uri);
                }
            });

            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    container.getContext().startActivity(intent);
                    return true;
                }
            });

            imageView.setTag(getUri().toString());

            if(h == -1 || w == -1) {
                resetThings(imageView, container);
                imageView.addOnLayoutChangeListener(new Bla(imageView, container));
            } else {
                doSizingStuff(imageView, container, h, w);
            }

            Picasso.with(imageView.getContext())
                    .cancelRequest(imageView);
            Picasso.with(imageView.getContext())
                    .load(uri)
                    .resize(itemWidth, 0)
                    .onlyScaleDown()
                    .into(imageView);

        }

        class Bla implements View.OnLayoutChangeListener{

            WeakReference<ImageView> imageView;
            WeakReference<View> container;

            Bla(ImageView imageView, View container) {
                this.imageView = new WeakReference<>(imageView);
                this.container = new WeakReference<>(container);
                L.d(LOG_TAG, getUri() + " " + this);
            }

            void onGlobalLayout() {
                ImageView imageView = this.imageView.get();

                if(imageView != null && imageView.getTag().equals(getUri().toString())) {
                    if (h > 0 && w > 0) {
                        L.d(LOG_TAG, getUri() + " " + this + " h + w already set");
                        imageView.removeOnLayoutChangeListener(this);
                        return;
                    }
                }

                if(imageView != null && imageView.getHeight() > 0 && imageView.getHeight() != ViewCompat.getMinimumHeight(imageView)) {
                    imageView.removeOnLayoutChangeListener(this);
                    if(imageView.getTag().equals(getUri().toString())) {
                        h = imageView.getHeight();
                        w = imageView.getWidth();
                        L.d(LOG_TAG, getUri()  + " " + this +" " + h + " " + w);
                        doSizingStuff(imageView, container.get(), h, w);
                    } else {
                        L.d(LOG_TAG, getUri() + " " + this + " wrong view " + imageView.getTag());
                    }
                } else {
                    if(imageView != null) {
                        L.d(LOG_TAG, getUri() + " " + this + " to small " + imageView.getHeight());
                    } else {
                        L.d(LOG_TAG, getUri() + " " + this + " view null");
                    }
                }
            }

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                onGlobalLayout();
            }
        }

        void doSizingStuff(ImageView imageView, View container, int h, int w) {
            final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            final int paddingSelectedVertical = calculateSelectedPadding(h, w, paddingSelectedHorizontal);
            if(isSelected()) {
                layoutParams.width = w - 2 * (paddingSelectedHorizontal - padding);
                layoutParams.height = h - 2 * (paddingSelectedVertical - padding);
            } else {
                layoutParams.width = w;
                layoutParams.height = h;
            }

            imageView.setAdjustViewBounds(false);
            imageView.setLayoutParams(layoutParams);

            final StaggeredGridLayoutManager.LayoutParams layoutParams1 = (StaggeredGridLayoutManager.LayoutParams) container.getLayoutParams();
            layoutParams1.width = padding * 2 + w;
            layoutParams1.height = padding * 2 + h;
            container.setLayoutParams(layoutParams1);
        }

        void resetThings(ImageView imageView, View container) {
            final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            final StaggeredGridLayoutManager.LayoutParams layoutParams1 = (StaggeredGridLayoutManager.LayoutParams) container.getLayoutParams();
            layoutParams1.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams1.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            imageView.setAdjustViewBounds(true);
            imageView.setLayoutParams(layoutParams);
            container.setLayoutParams(layoutParams1);
        }

        public Uri getUri() {
            return uri;
        }

        private int calculateSelectedPadding(int h, int w, int paddingH) {
            return (int)(paddingH * ((double)h / w));
        }
    }

    static class StaticItem extends Item {

        private final int iconId, textId, type;
        private final View.OnClickListener onClickListener;

        private StaticItem(int layoutId, int iconId, int textId, int type, View.OnClickListener onClickListener) {
            super(layoutId);
            this.iconId = iconId;
            this.textId = textId;
            this.type = type;
            this.onClickListener = onClickListener;
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
}
