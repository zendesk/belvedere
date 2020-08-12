package zendesk.belvedere;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ImageStreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ImageStreamItems.Item> staticItems;
    private List<ImageStreamItems.Item> imageStream;

    private List<ImageStreamItems.Item> list;

    ImageStreamAdapter() {
        staticItems = new ArrayList<>();
        imageStream = new ArrayList<>();
        list = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new RecyclerView.ViewHolder(v) {};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        list.get(position).bind(holder.itemView);
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).getLayoutId();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    void initializeWithImages(List<ImageStreamItems.Item> imageStream) {
        updateDataSet(staticItems, imageStream);
    }

    void setItemsSelected(List<MediaResult> mediaResults) {
        final List<ImageStreamItems.Item> streamItemImages = new ArrayList<>(imageStream);
        final Set<Uri> uris = new HashSet<>();

        for(MediaResult mediaResult : mediaResults) {
            uris.add(mediaResult.getOriginalUri());
        }

        for (ImageStreamItems.Item item : streamItemImages) {
            final boolean selected = uris.contains(item.getMediaResult().getOriginalUri());
            item.setSelected(selected);
        }

        updateDataSet(staticItems, streamItemImages);
    }

    void addStaticItem(ImageStreamItems.Item staticItem) {
        updateDataSet(Collections.singletonList(staticItem), imageStream);
    }

    private void updateDataSet(List<ImageStreamItems.Item> newStaticItems,
                               List<ImageStreamItems.Item> newImageStream) {
        List<ImageStreamItems.Item> newList = new ArrayList<>(newStaticItems.size() + newImageStream.size());
        newList.addAll(newStaticItems);
        newList.addAll(newImageStream);

        staticItems = newStaticItems;
        imageStream = newImageStream;
        list = newList;
    }

    interface Listener {

        void onOpenCamera();

        boolean onSelectionChanged(ImageStreamItems.Item item);
    }

}
