package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
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

    private final static int PIC_CAMERA = R.drawable.belvedere_ic_camera_black;
    private final static int LAYOUT_GRID = R.layout.stream_list_item_square_static;

    private final static float SELECTED_OPACITY = .8F;

    static List<Item> fromUris(List<MediaResult> uris, ImageStreamAdapter.Delegate delegate,
                               Context context, int itemWidth) {
        final List<Item> items = new ArrayList<>(uris.size());

        for(MediaResult uri : uris) {
            if(uri.getMimeType() != null && uri.getMimeType().startsWith("image")) {
                items.add(new StreamItemImage(delegate, uri, context, itemWidth));
            } else {
                items.add(new StreamItemFile(delegate, uri, context));
            }
        }

        return items;
    }

    static StaticItem forCameraSquare(final ImageStreamAdapter.Delegate delegate) {
        return new StaticItem(LAYOUT_GRID, PIC_CAMERA, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delegate.openCamera();
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

        abstract void bind(View view, int position);

        abstract MediaResult getMediaResult();

        void setSelected(boolean selected) {
            this.isSelected = selected;
        }

        boolean isSelected() {
            return isSelected;
        }
    }

    static class StreamItemFile extends Item {

        private final MediaResult mediaResult;
        private final ResolveInfo resolveInfo;
        private final ImageStreamAdapter.Delegate delegate;
        private final int colorPrimary;
        private final int padding;
        private final int paddingSelectedHorizontal;

        private int w = -1, h = -1;

        StreamItemFile(ImageStreamAdapter.Delegate delegate, MediaResult mediaResult, Context context) {
            super(R.layout.stream_list_item_genric_file);
            this.mediaResult = mediaResult;
            this.resolveInfo = getAppInfoForFile(mediaResult.getName(), context);
            this.delegate = delegate;
            this.colorPrimary = Utils.getThemeColor(context, R.attr.colorPrimary);
            this.padding = context.getResources().getDimensionPixelOffset(R.dimen.image_stream_image_padding);
            this.paddingSelectedHorizontal = context.getResources().getDimensionPixelOffset(R.dimen.image_stream_image_padding_selected);
        }

        ResolveInfo getAppInfoForFile(String fileName, Context context) {
            final PackageManager pm = context.getPackageManager();
            final MediaResult file = Belvedere.from(context).getFile("tmp", fileName);

            if(file == null) {
                return null;
            }

            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(file.getUri());

            final List<ResolveInfo> matchingApps =
                    pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if(matchingApps != null && matchingApps.size() > 0) {
                return matchingApps.get(0);
            }

            return null;
        }

        @Override
        void bind(View view, final int position) {
            final Context context = view.getContext();
            final ImageView icon = (ImageView) view.findViewById(R.id.list_item_file_icon);
            final TextView title = (TextView) view.findViewById(R.id.list_item_file_title);
            final TextView label = (TextView) view.findViewById(R.id.list_item_file_label);
            final ImageView selectOverlay = (ImageView) view.findViewById(R.id.list_item_file_overlay);
            final CardView container = (CardView) view.findViewById(R.id.list_item_file_container);
            final View holder = view.findViewById(R.id.list_item_file_holder);

            title.setText(mediaResult.getName());

            if(resolveInfo != null) {
                final PackageManager pm = context.getPackageManager();
                label.setText(resolveInfo.loadLabel(pm));
                icon.setImageDrawable(resolveInfo.loadIcon(pm));
            } else {
                label.setText("Unknown");
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }


            if(isSelected()) {
                Utils.internalSetTint(selectOverlay, colorPrimary);
                container.setAlpha(SELECTED_OPACITY);
                selectOverlay.setVisibility(View.VISIBLE);

            } else {
                selectOverlay.setVisibility(View.GONE);
                container.setAlpha(1.f);

            }

            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelected(!isSelected());
                    delegate.setSelected(mediaResult, isSelected(), position);
                }
            });


            if(w > 0 && h > 0) {
                adjustSize(container, holder, h, w);
            } else {
                container.post(new Runnable() {
                    @Override
                    public void run() {
                        h = container.getHeight();
                        w = container.getWidth();
                        adjustSize(container, holder, h, w);
                    }
                });
            }
        }

        void adjustSize(final CardView cardView, final View container, int h, int w) {
            final CardView.LayoutParams layoutParams = (CardView.LayoutParams) cardView.getLayoutParams();
            final int paddingSelectedVertical = calculateSelectedPadding(h, w, paddingSelectedHorizontal);

            if(isSelected()) {
                layoutParams.width = w - 2 * (paddingSelectedHorizontal - padding);
                layoutParams.height = h - 2 * (paddingSelectedVertical - padding);
            } else {
                layoutParams.width = w;
                layoutParams.height = h;
            }

            cardView.setLayoutParams(layoutParams);

            final StaggeredGridLayoutManager.LayoutParams layoutParams1 = (StaggeredGridLayoutManager.LayoutParams) container.getLayoutParams();
            layoutParams1.width = padding * 2 + w;
            layoutParams1.height = padding * 2 + h;
            container.setLayoutParams(layoutParams1);

            cardView.post(new Runnable() {
                @Override
                public void run() {
                    cardView.requestLayout();
                }
            });
        }

        private int calculateSelectedPadding(int h, int w, int paddingH) {
            return (int)(paddingH * ((float)h / w));
        }

        @Override
        MediaResult getMediaResult() {
            return mediaResult;
        }
    }

    static class StreamItemImage extends Item {

        private static final String LOG_TAG = "StreamItemImage";


        private final MediaResult uri;
        private final ImageStreamAdapter.Delegate delegate;

        private int h = -1, w = -1;

        private final int itemWidth;
        private final int padding;
        private final int paddingSelectedHorizontal;

        private final int colorPrimary;

        StreamItemImage(ImageStreamAdapter.Delegate delegate, MediaResult uri, Context context, int itemWidth) {
            super(R.layout.stream_list_item);
            this.delegate = delegate;
            this.uri = uri;

            this.padding = context.getResources().getDimensionPixelOffset(R.dimen.image_stream_image_padding);
            this.paddingSelectedHorizontal = context.getResources().getDimensionPixelOffset(R.dimen.image_stream_image_padding_selected);
            this.itemWidth = itemWidth;
            this.colorPrimary = Utils.getThemeColor(context, R.attr.colorPrimary);
        }

        @Override
        public void bind(View view, final int position) {
            final Context context = view.getContext();
            final ImageView imageView = (ImageView) view.findViewById(R.id.list_item_image);
            final ImageView selectOverlay = (ImageView) view.findViewById(R.id.list_item_image_overlay);
            final View container = view.findViewById(R.id.list_item_container);

            if(isSelected()) {
                selectOverlay.setVisibility(View.VISIBLE);
                imageView.setAlpha(SELECTED_OPACITY);
                Utils.internalSetTint(selectOverlay, colorPrimary);

            } else {
                selectOverlay.setVisibility(View.GONE);
                imageView.setAlpha(1.0F);
            }

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelected(!isSelected());
                    delegate.setSelected(uri, isSelected(), position);
                }
            });

            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri.getUri());
                    context.startActivity(intent);
                    return true;
                }
            });

            imageView.setTag(getUri().toString());

            if(h == -1 || w == -1) {
                resetItemSizing(imageView, container);
                imageView.addOnLayoutChangeListener(new ItemLayoutListener(imageView, container));
            } else {
                adjustSize(imageView, container, h, w);
            }

            Picasso.with(imageView.getContext())
                    .cancelRequest(imageView);

            Picasso.with(imageView.getContext())
                    .load(uri.getUri())
                    .resize(itemWidth, 0)
                    .transform(Utils.roundTransformation(context, R.dimen.image_stream_item_radius))
                    .onlyScaleDown()
                    .into(imageView);
        }

        @Override
        MediaResult getMediaResult() {
            return uri;
        }

        private class ItemLayoutListener implements View.OnLayoutChangeListener {

            private final WeakReference<ImageView> imageView;
            private final WeakReference<View> container;

            private ItemLayoutListener(ImageView imageView, View container) {
                this.imageView = new WeakReference<>(imageView);
                this.container = new WeakReference<>(container);
            }

            void onGlobalLayout() {
                ImageView imageView = this.imageView.get();

                if(imageView != null && imageView.getTag().equals(getUri().toString())) {
                    if (h > 0 && w > 0) {
                        imageView.removeOnLayoutChangeListener(this);
                        return;
                    }
                }

                if(imageView != null && imageView.getHeight() > 0 && imageView.getHeight() != ViewCompat.getMinimumHeight(imageView)) {
                    imageView.removeOnLayoutChangeListener(this);
                    if(imageView.getTag().equals(getUri().toString())) {
                        h = imageView.getHeight();
                        w = imageView.getWidth();
                        adjustSize(imageView, container.get(), h, w);
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

        void adjustSize(final ImageView imageView, final View container, int h, int w) {
            final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            final int paddingSelectedVertical = calculateSelectedPadding(h, w, paddingSelectedHorizontal);

            if(isSelected()) {
                layoutParams.width = w - 2 * (paddingSelectedHorizontal - padding);
                layoutParams.height = h - 2 * (paddingSelectedVertical - padding);
            } else {
                layoutParams.width = w;
                layoutParams.height = h;
            }

            imageView.setLayoutParams(layoutParams);

            final StaggeredGridLayoutManager.LayoutParams layoutParams1 = (StaggeredGridLayoutManager.LayoutParams) container.getLayoutParams();
            layoutParams1.width = padding * 2 + w;
            layoutParams1.height = padding * 2 + h;
            container.setLayoutParams(layoutParams1);

            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.requestLayout();
                }
            });
        }

        void resetItemSizing(ImageView imageView, View container) {
            final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            final StaggeredGridLayoutManager.LayoutParams layoutParams1 = (StaggeredGridLayoutManager.LayoutParams) container.getLayoutParams();
            layoutParams1.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams1.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            imageView.setLayoutParams(layoutParams);
            container.setLayoutParams(layoutParams1);
        }

        Uri getUri() {
            return uri.getOriginalUri();
        }

        private int calculateSelectedPadding(int h, int w, int paddingH) {
            return (int)(paddingH * ((float)h / w));
        }
    }

    static class StaticItem extends Item {

        private final int iconId;
        private final View.OnClickListener onClickListener;

        private StaticItem(int layoutId, int iconId, View.OnClickListener onClickListener) {
            super(layoutId);
            this.iconId = iconId;
            this.onClickListener = onClickListener;
        }

        @Override
        public void bind(View view, int position) {
            ((ImageView)view.findViewById(R.id.list_item_static_image)).setImageResource(iconId);
            view.findViewById(R.id.list_item_static_click_area).setOnClickListener(onClickListener);
        }

        @Override
        MediaResult getMediaResult() {
            return null;
        }
    }
}
