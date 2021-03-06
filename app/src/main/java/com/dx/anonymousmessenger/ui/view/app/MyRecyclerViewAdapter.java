package com.dx.anonymousmessenger.ui.view.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.ui.view.message_list.MessageListActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.ContactProfileActivity;
import com.dx.anonymousmessenger.util.Utils;

import java.util.List;
import java.util.Objects;

import static androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class MyRecyclerViewAdapter extends Adapter {
    private static final int VIEW_TYPE_READ = 1;
    private static final int VIEW_TYPE_UNREAD = 2;
    public List<String[]> mData;
//    private LayoutInflater mInflater;
    private View.OnClickListener mClickListener;
    private final DxApplication app;
    private final AppFragment appFragment;

    // data is passed into the constructor
    MyRecyclerViewAdapter(DxApplication app, List<String[]> data, AppFragment appFragment) {
//        this.mInflater = LayoutInflater.from(app);
        this.app = app;
        this.mData = data;
        this.appFragment = appFragment;
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
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        } else{
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            view.setOnClickListener(mClickListener);
            return new MyRecyclerViewAdapter.ReadContactHolder(view);
        }
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] contact = mData.get(position);
        long createdAt = 0;
        try {
            if(contact[5].length()>0){
                createdAt = Long.parseLong(contact[5]);
            }
        }catch (Exception ignored){}

        holder.itemView.setOnLongClickListener(new ListItemOnClickListener(holder.itemView,mData.get(holder.getAdapterPosition())[1],holder.getAdapterPosition()));
        holder.itemView.setOnClickListener(v -> {
            appFragment.stopCheckingMessages();
            int position1 = holder.getAdapterPosition();
            Intent intent = new Intent(v.getContext(), MessageListActivity.class);
            intent.putExtra("nickname",mData.get(position1)[0]);
            intent.putExtra("address",mData.get(position1)[1].substring(0,10));
            v.getContext().startActivity(intent);
        });
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_READ://String msg, String send_to, long createdAt, boolean received
                ((MyRecyclerViewAdapter.ReadContactHolder) holder).bind(contact[0].equals("")?contact[1]:contact[0],
                        contact[3],
                        contact[4],
                        createdAt,
                        contact[6].equals("true"),
                        contact[1]);
                break;
            case VIEW_TYPE_UNREAD:
                ((MyRecyclerViewAdapter.UnreadContactHolder) holder).bind(contact[0].equals("")?contact[1]:contact[0],contact[3],contact[4],createdAt,contact[6].equals("true"),contact[1]);
                break;
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        if(mData==null){
            return 0;
        }
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

    public class ListItemOnClickListener implements View.OnLongClickListener {
        View itemView;
        String address;
        int pos;

        ListItemOnClickListener(View itemView, String address, int pos){
            this.itemView = itemView;
            this.address = address;
            this.pos = pos;
        }

        @Override
        public boolean onLongClick(View v) {
            Objects.requireNonNull(appFragment.getActivity()).findViewById(R.id.top_bar).setVisibility(View.GONE);
            v.setBackgroundColor(v.getResources().getColor(R.color.dx_night_700,null));
            v.startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.contextual_contact_bar, menu);
                    mode.setTitle("1");
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    mode.finish();
                    switch (item.getItemId()) {
                        case R.id.profile_contact:
                            try{
                                Intent intent = new Intent(itemView.getContext(), ContactProfileActivity.class);
                                intent.putExtra("address",address.substring(0,10));
                                v.getContext().startActivity(intent);
                            }catch (Exception ignored) {}
                            break;
                        case R.id.delete_contact:
                            new AlertDialog.Builder(v.getContext(), R.style.AppAlertDialog)
                                    .setTitle(R.string.delete_contact_question)
                                    .setMessage(R.string.delete_contact_details)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                        try {
                                            DbHelper.deleteContact(address, app);
                                            DbHelper.clearConversation(address, app);
                                            mData.remove(pos);
                                            notifyItemRemoved(pos);
                                        } catch (Exception ignored) {
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {

                                    }).show();
                            break;
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    v.setBackground(ResourcesCompat.getDrawable(v.getResources(),R.drawable.contact_background,null));
                    Objects.requireNonNull(appFragment.getActivity()).findViewById(R.id.top_bar).setVisibility(View.VISIBLE);
                }
            });
//            PopupMenu popup = new PopupMenu(v.getContext(), itemView);
//            popup.inflate(R.menu.contact_menu);
//
//            popup.setOnMenuItemClickListener(item -> {
//                switch (item.getItemId()) {
//                    case R.id.profile_contact:
//                        try{
//                            Intent intent = new Intent(itemView.getContext(), ContactProfileActivity.class);
//                            intent.putExtra("address",address.substring(0,10));
//                            itemView.getContext().startActivity(intent);
//                        }catch (Exception ignored) {}
//                        break;
//                    case R.id.delete_contact:
//                        new AlertDialog.Builder(itemView.getContext(), R.style.AppAlertDialog)
//                            .setTitle(R.string.delete_contact_question)
//                            .setMessage(R.string.delete_contact_details)
//                            .setIcon(android.R.drawable.ic_dialog_alert)
//                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
//                                try {
//                                    DbHelper.deleteContact(address, app);
//                                    DbHelper.clearConversation(address, app);
//                                    mData.remove(pos);
//                                    notifyItemRemoved(pos);
//                                } catch (Exception ignored) {
//                                }
//                            })
//                            .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
//
//                            }).show();
//                        break;
//                }
//                return false;
//            });
//            popup.show();
            return true;
        }
    }

    private class ReadContactHolder extends ViewHolder {
        TextView contactName,msgText,timeText;
        ImageView imageView,seen,contactOnline;

        ReadContactHolder(View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            imageView = itemView.findViewById(R.id.contact_unread_circle);
            timeText = itemView.findViewById(R.id.time_text);
            msgText = itemView.findViewById(R.id.message_text);
            seen = itemView.findViewById(R.id.seen);
            contactOnline = itemView.findViewById(R.id.contact_online);
        }

        void bind(String title, String msg, String send_to, long createdAt, boolean received, String address) {
            contactName.setText(title);
            contactName.setTypeface(null, Typeface.NORMAL);
            imageView.setVisibility(View.INVISIBLE);
            msgText.setText(msg);
            timeText.setText(createdAt>0?Utils.formatDateTime(createdAt):"");
            seen.setVisibility(send_to.equals(app.getHostname())?View.GONE:received?View.VISIBLE:View.GONE);
            if(app.onlineList.contains(address)){
                contactOnline.setVisibility(View.VISIBLE);
            }else{
                contactOnline.setVisibility(View.GONE);
            }
        }
    }

    private class UnreadContactHolder extends ReadContactHolder {

        UnreadContactHolder(View itemView) {
            super(itemView);
        }

        void bind(String title, String msg, String send_to, long createdAt, boolean received, String address) {
//            imageView.setVisibility(View.VISIBLE);
            super.bind(title,msg,send_to,createdAt,received,address);
//            itemView.setBackground(ContextCompat.getDrawable(app,R.drawable.rounded_rectangle_steel));
//            itemView.setPadding(15,0,12,2);
            contactName.setTypeface(Typeface.DEFAULT_BOLD);
            msgText.setTypeface(Typeface.DEFAULT_BOLD);
            imageView.setVisibility(View.VISIBLE);
            seen.setVisibility(View.INVISIBLE);
        }
    }
}
