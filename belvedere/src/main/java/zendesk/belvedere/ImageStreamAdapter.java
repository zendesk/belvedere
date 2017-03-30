package zendesk.belvedere;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

class ImageStreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ImageStreamDataSource imageStreamDataSource;

    ImageStreamAdapter(ImageStreamDataSource dataSource) {
        setHasStableIds(true);
        this.imageStreamDataSource = dataSource;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new RecyclerView.ViewHolder(v){};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        imageStreamDataSource.getItemForPos(position).bind(holder.itemView);
    }

    @Override
    public long getItemId(int position) {
        return imageStreamDataSource.getItemForPos(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return imageStreamDataSource.getItemForPos(position).getLayoutId();
    }

    @Override
    public int getItemCount() {
        return imageStreamDataSource.getItemCount();
    }

    interface Delegate {
        void openCamera();
        void openGallery();
        void setSelected(MediaResult uri, boolean selected);
    }
}
