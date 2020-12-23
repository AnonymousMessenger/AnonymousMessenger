package com.dx.anonymousmessenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TipsRecycleViewAdapter extends RecyclerView.Adapter<TipsRecycleViewAdapter.ViewHolder>{
    Context context;
    LayoutInflater mInflater;
    String[] strings;
    public TipsRecycleViewAdapter(Context context, String[] strings) {
        this.context = context;
        this.strings = strings;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.tips_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String string = strings[holder.getAdapterPosition()];
        holder.tip.setText(string);
    }

    @Override
    public int getItemCount() {
        return strings.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView tip;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.tip = itemView.findViewById(R.id.txt_tip);
        }
    }
}
