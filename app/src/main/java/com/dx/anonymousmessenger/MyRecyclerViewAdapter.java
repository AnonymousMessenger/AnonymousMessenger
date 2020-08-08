package com.dx.anonymousmessenger;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.R;

import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_READ = 1;
    private static final int VIEW_TYPE_UNREAD = 2;
    private List<String[]> mData;
    private LayoutInflater mInflater;
    private View.OnClickListener mClickListener;

    // data is passed into the constructor
    MyRecyclerViewAdapter(Context context, List<String[]> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    public int getItemViewType(int position) {
        String[] contact = mData.get(position);

        if (contact[2].equals("unread")) {
            return VIEW_TYPE_UNREAD;
        } else {
            return VIEW_TYPE_READ;
        }
    }

    // inflates the row layout from xml when needed
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_READ) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            view.setOnClickListener(mClickListener);
            return new MyRecyclerViewAdapter.ReadContactHolder(view);
        } else if (viewType == VIEW_TYPE_UNREAD) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            view.setOnClickListener(mClickListener);
            return new MyRecyclerViewAdapter.UnreadContactHolder(view);
        }
        Log.e("finding contact type","something went wrong");
        return null;
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        String[] contact = mData.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_READ:
                ((MyRecyclerViewAdapter.ReadContactHolder) holder).bind(contact[0].equals("")?contact[1]:contact[0]);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getAdapterPosition();
                        Intent intent = new Intent(v.getContext(), MessageListActivity.class);
                        intent.putExtra("nickname",mData.get(position)[0]);
                        intent.putExtra("address",mData.get(position)[1]);
                        v.getContext().startActivity(intent);
                    }
                });
                break;
            case VIEW_TYPE_UNREAD:
                ((MyRecyclerViewAdapter.UnreadContactHolder) holder).bind(contact[0].equals("")?contact[1]:contact[0]);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getAdapterPosition();
                        Intent intent = new Intent(v.getContext(), MessageListActivity.class);
                        intent.putExtra("nickname",mData.get(position)[0]);
                        intent.putExtra("address",mData.get(position)[1]);
                        v.getContext().startActivity(intent);
                    }
                });
                break;
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
//    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
//        TextView myTextView;
//
//        ViewHolder(View itemView) {
//            super(itemView);
//            myTextView = itemView.findViewById(R.id.contact_name);
//            itemView.setOnClickListener(this);
//        }
//
//        @Override
//        public void onClick(View view) {
//            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
//        }
//    }

    // convenience method for getting data at click position
    public String[] getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(View.OnClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    private class ReadContactHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        ImageView imageView;

        ReadContactHolder(View itemView) {
            super(itemView);
            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            imageView = (ImageView) itemView.findViewById(R.id.contact_unread_circle);
        }

        void bind(String title) {
            contactName.setText(title);
            contactName.setTypeface(null, Typeface.NORMAL);
            imageView.setVisibility(View.INVISIBLE);
        }
    }

    private class UnreadContactHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        ImageView imageView;

        UnreadContactHolder(View itemView) {
            super(itemView);
            contactName = (TextView) itemView.findViewById(R.id.contact_name);
            imageView = (ImageView) itemView.findViewById(R.id.contact_unread_circle);
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.dx_night_700,itemView.getResources().newTheme()));
        }

        void bind(String title) {
            contactName.setText(title);
            contactName.setTextColor(itemView.getResources().getColor(R.color.dx_white,itemView.getResources().newTheme()));
            contactName.setTypeface(null, Typeface.BOLD);
            imageView.setVisibility(View.VISIBLE);
        }
    }
}
