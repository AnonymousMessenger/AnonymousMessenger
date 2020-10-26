package com.dx.anonymousmessenger.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.R;

import java.util.List;

public class MediaRecycleViewAdapter extends RecyclerView.Adapter<MediaRecycleViewAdapter.ViewHolder> {

    public List<String> paths;
    public List<String> types;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public MediaRecycleViewAdapter(Context context, List<String> paths, List<String> types) {
        this.mInflater = LayoutInflater.from(context);
        this.paths = paths;
        this.types = types;
    }

    // inflates the row layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.media_rv_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the view
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String path = paths.get(position);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        new Thread(()->{
            Bitmap image;
            if(types.get(position).equals(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO+"")){
                image = ThumbnailUtils.createVideoThumbnail(path,
                        MediaStore.Images.Thumbnails.MINI_KIND);
            }else{
                image = BitmapFactory.decodeFile(path,options);
            }
            new Handler(Looper.getMainLooper()).post(()->{
                try{
                    holder.myView.setImageBitmap(image);
                }catch (Exception ignored) {}
            });
        }).start();
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return paths.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView myView;

        ViewHolder(View itemView) {
            super(itemView);
            myView = itemView.findViewById(R.id.view_image);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    public String getItem(int id) {
        return paths.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
