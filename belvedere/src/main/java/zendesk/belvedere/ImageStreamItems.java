package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import zendesk.belvedere.ui.R;

class ImageStreamItems {

    private final static int PIC_CAMERA = R.drawable.belvedere_ic_camera_black;
    private final static int LAYOUT_GRID = R.layout.belvedere_stream_list_item_square_static;

    static List<Item> fromMediaResults(List<MediaResult> mediaResults, ImageStreamAdapter.Listener listener, Context context) {

        final List<Item> items = new ArrayList<>(mediaResults.size());

        for(MediaResult mediaResult : mediaResults) {
            if(mediaResult.getMimeType() != null && mediaResult.getMimeType().startsWith("image")) {
                items.add(new StreamItemImage(listener, mediaResult));
            } else {
                items.add(new StreamItemFile(listener, mediaResult, context));
            }
        }

        return items;
    }

    static StaticItem forCameraSquare(final ImageStreamAdapter.Listener listener) {
        return new StaticItem(LAYOUT_GRID, PIC_CAMERA, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onOpenCamera();
            }
        });
    }

    static abstract class Item {

        private final int layoutId;
        private final long id;
        private final MediaResult mediaResult;
        private boolean isSelected;

        Item(int layoutId, MediaResult mediaResult) {
            this.layoutId = layoutId;
            this.mediaResult = mediaResult;
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

        MediaResult getMediaResult() {
            return mediaResult;
        }

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
        private final ImageStreamAdapter.Listener listener;

        StreamItemFile(ImageStreamAdapter.Listener listener, MediaResult mediaResult, Context context) {
            super(R.layout.belvedere_stream_list_item_genric_file, mediaResult);
            this.mediaResult = mediaResult;
            this.resolveInfo = getAppInfoForFile(mediaResult.getName(), context);
            this.listener = listener;
        }

        @Override
        void bind(View view, final int position) {
            final Context context = view.getContext();
            final ImageView icon = view.findViewById(R.id.list_item_file_icon);
            final TextView title = view.findViewById(R.id.list_item_file_title);
            final TextView label = view.findViewById(R.id.list_item_file_label);
            final SelectableView holder = view.findViewById(R.id.list_item_file_holder);

            title.setText(mediaResult.getName());

            if(resolveInfo != null) {
                final PackageManager pm = context.getPackageManager();
                label.setText(resolveInfo.loadLabel(pm));
                icon.setImageDrawable(resolveInfo.loadIcon(pm));
            } else {
                label.setText(R.string.belvedere_image_stream_unknown_app);
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            holder.setSelected(isSelected());
            holder.setSelectionListener(new SelectableView.SelectionListener() {
                @Override
                public void onSelectionChanged(boolean selected) {
                    listener.onSelectionChanged(StreamItemFile.this, position);
                }
            });
        }

        private ResolveInfo getAppInfoForFile(String fileName, Context context) {
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
    }

    static class StreamItemImage extends Item {

        private final MediaResult mediaResult;
        private final ImageStreamAdapter.Listener listener;

        private FixedWidthImageView.CalculatedDimensions dimensions;

        StreamItemImage(ImageStreamAdapter.Listener listener, MediaResult mediaResult) {
            super(R.layout.belvedere_stream_list_item, mediaResult);
            this.listener = listener;
            this.mediaResult = mediaResult;
        }

        @Override
        public void bind(final View view, final int position) {
            final Context context = view.getContext();
            final FixedWidthImageView imageView = view.findViewById(R.id.list_item_image);
            final SelectableView container = view.findViewById(R.id.list_item_selectable);

            if(dimensions != null) {
                imageView.showImage(Picasso.with(context), mediaResult.getOriginalUri(), dimensions);
            } else {
                imageView.showImage(Picasso.with(context), mediaResult.getOriginalUri(), mediaResult.getWidth(), mediaResult.getHeight(), new FixedWidthImageView.DimensionsCallback() {
                    @Override
                    public void onImageDimensionsFound(FixedWidthImageView.CalculatedDimensions dimensions) {
                        StreamItemImage.this.dimensions = dimensions;
                    }
                });
            }

            container.setSelected(isSelected());
            container.setSelectionListener(new SelectableView.SelectionListener() {
                @Override
                public void onSelectionChanged(boolean selected) {
                    listener.onSelectionChanged(StreamItemImage.this, position);
                }
            });
        }
    }

    static class StaticItem extends Item {

        private final int iconId;
        private final View.OnClickListener onClickListener;

        private StaticItem(int layoutId, int iconId, View.OnClickListener onClickListener) {
            super(layoutId, null);
            this.iconId = iconId;
            this.onClickListener = onClickListener;
        }

        @Override
        public void bind(View view, int position) {
            ((ImageView)view.findViewById(R.id.list_item_static_image)).setImageResource(iconId);
            view.findViewById(R.id.list_item_static_click_area).setOnClickListener(onClickListener);
        }
    }
}
