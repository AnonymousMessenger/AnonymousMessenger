package com.dx.anonymousmessenger.ui.view.notepad;

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

import static androidx.core.content.ContextCompat.getSystemService;

public class NotepadRecycleViewAdapter extends RecyclerView.Adapter<NotepadRecycleViewAdapter.ViewHolder>{
    Context context;
    LayoutInflater mInflater;
    List<Object[]> list;
    public NotepadRecycleViewAdapter(Context context, List<Object[]> list) {
        this.context = context;
        this.list = list;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.notepad_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String string = (String)(list.get(holder.getAdapterPosition())[0]);
        long createdAt = (long)(list.get(holder.getAdapterPosition())[1]);
        holder.note.setText(string);
        holder.time.setText(Utils.formatDateTime(createdAt));
        holder.note.setOnClickListener((v)->{
            PopupMenu popup = new PopupMenu(v.getContext(), holder.itemView);
            popup.inflate(R.menu.note_menu);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.delete_note:
                        new AlertDialog.Builder(holder.itemView.getContext(), R.style.AppAlertDialog)
                                .setTitle(R.string.delete_note_question)
                                .setMessage(R.string.delete_note_details)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                    try {
                                        //delete
                                        new Thread(()->{
                                            DbHelper.deleteNote(createdAt,(DxApplication)((NotepadActivity)context).getApplication());
                                            new Handler(Looper.getMainLooper()).post(()->{
                                                list.remove(holder.getAdapterPosition());
                                                notifyItemRemoved(holder.getAdapterPosition());
                                            });
                                        }).start();
                                    } catch (Exception ignored) {
                                    }
                                })
                                .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {

                                }).show();
                        break;
                    case R.id.copy:
                        try{
                            //copy
                            ClipboardManager clipboard = getSystemService(Objects.requireNonNull(holder.itemView.getContext()), ClipboardManager.class);
                            ClipData clip = ClipData.newPlainText("label", holder.note.getText().toString());
                            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                            Snackbar.make(v, R.string.copied, Snackbar.LENGTH_SHORT).show();
                        }catch (Exception ignored) {}
                        break;
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
        View itemView;
        TextView note,time;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.time = itemView.findViewById(R.id.txt_time);
            this.note = itemView.findViewById(R.id.txt_note);
        }
    }
}
