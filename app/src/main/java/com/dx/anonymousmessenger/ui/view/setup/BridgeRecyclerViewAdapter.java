package com.dx.anonymousmessenger.ui.view.setup;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Objects;

public class BridgeRecyclerViewAdapter extends RecyclerView.Adapter<BridgeRecyclerViewAdapter.ViewHolder>{
    final Context context;
    final LayoutInflater mInflater;
    final List<String> list;
    public BridgeRecyclerViewAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.bridge_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String string = list.get(holder.getAbsoluteAdapterPosition());
        holder.bridge.setText(string);
        holder.bridge.setOnClickListener((v)->{
            PopupMenu popup = new PopupMenu(v.getContext(), holder.itemView);
            popup.inflate(R.menu.bridge_menu);
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.delete_bridge){
                    new AlertDialog.Builder(holder.itemView.getContext(), R.style.AppAlertDialog)
                            .setTitle(R.string.delete_bridge_question)
                            .setMessage(R.string.delete_bridge_details)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                try {
                                    //delete
                                    new Thread(()->{
                                        try{
                                            DbHelper.deleteBridge(string,(DxApplication)((Activity)context).getApplication());
                                            list.remove(holder.getAbsoluteAdapterPosition());
                                        }catch (Exception ignored){
                                            list.remove(holder.getAbsoluteAdapterPosition());

                                        }

                                        new Handler(Looper.getMainLooper()).post(()-> notifyItemRemoved(holder.getAbsoluteAdapterPosition()));
                                    }).start();
                                } catch (Exception ignored) {
                                }
                            })
                            .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
                            }).show();
                }else if(item.getItemId() == R.id.copy){
                    try{
                        //copy
                        ClipboardManager clipboard = getSystemService(Objects.requireNonNull(holder.itemView.getContext()), ClipboardManager.class);
                        ClipData clip = ClipData.newPlainText("label", holder.bridge.getText().toString());
                        Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                        Snackbar.make(v, R.string.copied, Snackbar.LENGTH_SHORT).show();
                    }catch (Exception ignored) {}
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View itemView;
        final TextView bridge;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.bridge = itemView.findViewById(R.id.txt_bridge);
        }
    }
}
