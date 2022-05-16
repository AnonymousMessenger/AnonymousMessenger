package com.dx.anonymousmessenger.ui.view.log;

import static androidx.core.content.ContextCompat.getSystemService;

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
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Objects;


public class LogRecycleViewAdapter extends RecyclerView.Adapter<LogRecycleViewAdapter.ViewHolder>{
    final Context context;
    final LayoutInflater mInflater;
    List<Object[]> list;
    public LogRecycleViewAdapter(Context context, List<Object[]> list) {
        this.context = context;
        this.list = list;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.log_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String type = (String)(list.get(holder.getAbsoluteAdapterPosition())[2]);
        String string = (String)(list.get(holder.getAbsoluteAdapterPosition())[0]);
        long createdAt = (long)(list.get(holder.getAbsoluteAdapterPosition())[1]);
        holder.log.setText(String.format("[%s] %s", type,string));
        holder.time.setText(Utils.formatDateTime(createdAt));
        holder.log.setOnClickListener((v)->{
            PopupMenu popup = new PopupMenu(v.getContext(), holder.itemView);
            popup.inflate(R.menu.log_menu);
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.delete_log){
                    new AlertDialog.Builder(holder.itemView.getContext(), R.style.AppAlertDialog)
                            .setTitle(R.string.delete_log_question)
                            .setMessage(R.string.delete_log_details)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                try {
                                    //delete
                                    new Thread(()->{
                                        DbHelper.deleteLog(createdAt,string,(DxApplication)((LogActivity)context).getApplication());
                                        new Handler(Looper.getMainLooper()).post(()->{
                                            list.remove(holder.getAbsoluteAdapterPosition());
                                            notifyItemRemoved(holder.getAbsoluteAdapterPosition());
                                        });
                                    }).start();
                                } catch (Exception ignored) {
                                }
                            })
                            .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
                            }).show();
                } else if(item.getItemId() == R.id.copy){
                    try{
                        //copy
                        ClipboardManager clipboard = getSystemService(Objects.requireNonNull(holder.itemView.getContext()), ClipboardManager.class);
                        ClipData clip = ClipData.newPlainText("label", holder.log.getText().toString());
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
        final TextView log;
        final TextView time;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.time = itemView.findViewById(R.id.txt_time);
            this.log = itemView.findViewById(R.id.txt_log);
        }
    }
}
