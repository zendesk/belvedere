package zendesk.belvedere;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import zendesk.belvedere.ui.R;

class ImageStreamItems {

    private final static int PIC_CAMERA = R.drawable.belvedere_ic_camera_black;
    private final static int LAYOUT_GRID = R.layout.stream_list_item_square_static;

    static List<Item> fromUris(List<MediaResult> uris,
                               ImageStreamAdapter.Delegate delegate,
                               Context context, long maxFileSize,
                               String maxFileSizeErrorMessage) {
        final List<Item> items = new ArrayList<>(uris.size());

        for(MediaResult mediaResult : uris) {
            if(mediaResult.getMimeType() != null && mediaResult.getMimeType().startsWith("image")) {
                items.add(new StreamItemImage(delegate, mediaResult, maxFileSize, maxFileSizeErrorMessage));
            } else {
                items.add(new StreamItemFile(delegate, mediaResult, context, maxFileSize, maxFileSizeErrorMessage));
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
        private final long maxFileSize;
        private final String maxFileSizeErrorMessage;

        StreamItemFile(ImageStreamAdapter.Delegate delegate, MediaResult mediaResult, Context context,
                       long maxFileSize, String maxFileSizeErrorMessage) {
            super(R.layout.stream_list_item_genric_file);
            this.maxFileSize = maxFileSize;
            this.maxFileSizeErrorMessage = maxFileSizeErrorMessage;
            this.mediaResult = mediaResult;
            this.resolveInfo = getAppInfoForFile(mediaResult.getName(), context);
            this.delegate = delegate;
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
                label.setText("Unknown");
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            holder.setSelected(isSelected());
            holder.setSelectionListener(new SelectableView.SelectionListener() {
                @Override
                public void onSelectionChanged(boolean selected) {
                    if(mediaResult.getSize() <= maxFileSize || maxFileSize == -1L) {
                        setSelected(!isSelected());
                        delegate.setSelected(mediaResult, isSelected(), position);
                    } else {
                        Toast.makeText(context, maxFileSizeErrorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        MediaResult getMediaResult() {
            return mediaResult;
        }
    }

    static class StreamItemImage extends Item {

        private final MediaResult mediaResult;
        private final ImageStreamAdapter.Delegate delegate;

        private final long maxFileSize;
        private final String maxFileSizeErrorMessage;

        private FixedWidthImageView.CalculatedDimensions dimensions;

        StreamItemImage(ImageStreamAdapter.Delegate delegate, MediaResult uri, long maxFileSize, String maxFileSizeErrorMessage) {
            super(R.layout.stream_list_item);
            this.delegate = delegate;
            this.mediaResult = uri;
            this.maxFileSize = maxFileSize;
            this.maxFileSizeErrorMessage = maxFileSizeErrorMessage;
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
                    if(mediaResult.getSize() <= maxFileSize || maxFileSize == -1L) {
                        setSelected(!isSelected());
                        delegate.setSelected(mediaResult, isSelected(), position);
                    } else {
                        Toast.makeText(context, maxFileSizeErrorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        MediaResult getMediaResult() {
            return mediaResult;
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
