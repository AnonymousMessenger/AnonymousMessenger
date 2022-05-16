package com.dx.anonymousmessenger.ui.view.app;

import static androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.alexvasilkov.gestures.commons.circle.CircleImageView;
import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.ui.view.message_list.MessageListActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.ContactProfileActivity;
import com.dx.anonymousmessenger.util.Utils;

import java.util.List;

public class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_READ = 1;
    private static final int VIEW_TYPE_UNREAD = 2;
    public List<String[]> mData;
//    private LayoutInflater mInflater;
    private View.OnClickListener mClickListener;
    private final DxApplication app;
    private final AppFragment appFragment;

    // data is passed into the constructor
    ContactListAdapter(DxApplication app, List<String[]> data, AppFragment appFragment) {
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
            return new ContactListAdapter.ReadContactHolder(view);
        } else if (viewType == VIEW_TYPE_UNREAD) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            view.setOnClickListener(mClickListener);
            return new ContactListAdapter.UnreadContactHolder(view);
        } else{
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_view, parent, false);
            view.setOnClickListener(mClickListener);
            return new ContactListAdapter.ReadContactHolder(view);
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

        holder.itemView.setOnLongClickListener(new ListItemOnClickListener(holder.itemView,mData.get(holder.getAbsoluteAdapterPosition())[1],holder.getAbsoluteAdapterPosition()));
//        holder.itemView.setOnClickListener(v -> {
//            appFragment.stopCheckingMessages();
//            int position1 = holder.getAbsoluteAdapterPosition();
//            Intent intent = new Intent(v.getContext(), MessageListActivity.class);
//            intent.putExtra("nickname",mData.get(position1)[0]);
//            intent.putExtra("address",mData.get(position1)[1].substring(0,10));
//            View v = holder.itemView.;
//            ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(this, v, "profile_picture");
//            this.startActivity(intent2,activityOptions.toBundle());
//            v.getContext().startActivity(intent);
//        });
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_READ:
                //String msg, String send_to, long createdAt, boolean received, String path
                ((ContactListAdapter.ReadContactHolder) holder).bind(contact[0].equals("")?contact[1]:contact[0],
                        contact[3],
                        contact[4],
                        createdAt,
                        contact[6].equals("true"),
                        contact[1],
                        contact[7]);
                break;
            case VIEW_TYPE_UNREAD:
                ((ContactListAdapter.UnreadContactHolder) holder).bind(contact[0].equals("")?contact[1]:contact[0],contact[3],contact[4],createdAt,contact[6].equals("true"),contact[1],contact[7]);
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

    // the activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public class ListItemOnClickListener implements View.OnLongClickListener {
        final View itemView;
        final String address;
        final int pos;

        ListItemOnClickListener(View itemView, String address, int pos){
            this.itemView = itemView;
            this.address = address;
            this.pos = pos;
        }

        @SuppressLint("RestrictedApi")
        @Override
        public boolean onLongClick(View v) {
            /*appFragment.requireActivity().findViewById(R.id.top_bar).setVisibility(View.GONE);
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
                    appFragment.requireActivity().findViewById(R.id.top_bar).setVisibility(View.VISIBLE);
                }
            });*/
            PopupMenu popup = new PopupMenu(v.getContext(), itemView);
            popup.inflate(R.menu.contact_menu);

            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.profile_contact){
                    try{
                        Intent intent = new Intent(itemView.getContext(), ContactProfileActivity.class);
                        intent.putExtra("address",address.substring(0,10));
                        itemView.getContext().startActivity(intent);
                    }catch (Exception ignored) {}
                }else if(item.getItemId() == R.id.delete_contact){
                    new AlertDialog.Builder(itemView.getContext(), R.style.AppAlertDialog)
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
                }
                return false;
            });
//            popup.show();
            MenuPopupHelper menuHelper = new MenuPopupHelper(v.getContext(), (MenuBuilder) popup.getMenu(), v);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
            return true;
        }
    }

    private class ReadContactHolder extends ViewHolder {
        final TextView contactName;
        final TextView msgText;
        final TextView timeText;
        final ImageView imageView;
        final ImageView seen;
        final ImageView contactOnline;
        final CircleImageView profileImage;

        ReadContactHolder(View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            imageView = itemView.findViewById(R.id.contact_unread_circle);
            timeText = itemView.findViewById(R.id.time_text);
            msgText = itemView.findViewById(R.id.message_text);
            seen = itemView.findViewById(R.id.seen);
            contactOnline = itemView.findViewById(R.id.contact_online);
            profileImage = itemView.findViewById(R.id.img_contact_profile_image);
        }

        void bind(String title, String msg, String send_to, long createdAt, boolean received, String address, String imagePath) {
            contactName.setText(title);
            contactName.setTypeface(null, Typeface.NORMAL);
            imageView.setVisibility(View.INVISIBLE);
            msgText.setText(msg);
            timeText.setText(createdAt>0?Utils.formatDateTime(createdAt):"");
            seen.setVisibility(send_to.equals(app.getHostname())?View.GONE:received?View.VISIBLE:View.GONE);
            itemView.setOnClickListener((v)->{
                appFragment.stopCheckingMessages();
                Intent intent = new Intent(v.getContext(), MessageListActivity.class);
                intent.putExtra("nickname",title);
                intent.putExtra("address",address.substring(0,10));
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(appFragment.requireActivity(), itemView, "title");
//                new Handler().postDelayed(()->{
                    v.getContext().startActivity(intent,activityOptions.toBundle());
//                },250);
            });
            if(app.onlineList.contains(address)){
                contactOnline.setVisibility(View.VISIBLE);
            }else{
                contactOnline.setVisibility(View.GONE);
            }
            if(profileImage!=null){
                profileImage.setOnClickListener((v)->{
                    Intent intent2 = new Intent(app, ContactProfileActivity.class);
                    intent2.putExtra("address", address.substring(0,10));
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(appFragment.requireActivity(), v, "profile_picture");
                    appFragment.requireActivity().startActivity(intent2,activityOptions.toBundle());
                });
            }
            if(imagePath!=null && !imagePath.equals("")){
                super.setIsRecyclable(false);
                new Thread(()->{
                    try{
                        byte[] image = FileHelper.getFile(imagePath, app);

                        if (image == null) {
                            return;
                        }
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length,options);
                        if(bitmap == null){
                            return;
                        }
                        new Handler(Looper.getMainLooper()).post(()->{
                            if(profileImage!=null){
                                profileImage.setImageBitmap(bitmap);
                            }
                            super.setIsRecyclable(true);
                        });
                    }catch (Exception ignored){
                        super.setIsRecyclable(true);
//                    e.printStackTrace();
                    }
                }).start();
            }
            else{
                Drawable drawable = AppCompatResources.getDrawable(app,R.drawable.circle);
                int width;
                int height;
                if (drawable != null) {
                    width = drawable.getIntrinsicWidth();
                    width = width > 0 ? width : 1;
                    height = drawable.getIntrinsicHeight();
                    height = height > 0 ? height : 1;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);
                    //profileImage.setBackground(AppCompatResources.getDrawable(app,R.drawable.circle));
                    if (profileImage != null) {
                        profileImage.setImageBitmap(bitmap);
                    }
                    //profileImage.setImageBitmap(BitmapFactory.decodeResource(app.getResources(),R.drawable.circle));
                }
            }
        }
    }

    private class UnreadContactHolder extends ReadContactHolder {

        UnreadContactHolder(View itemView) {
            super(itemView);
        }

        void bind(String title, String msg, String send_to, long createdAt, boolean received, String address, String imagePath) {
//            imageView.setVisibility(View.VISIBLE);
            super.bind(title,msg,send_to,createdAt,received,address,imagePath);
//            itemView.setBackground(ContextCompat.getDrawable(app,R.drawable.rounded_rectangle_steel));
//            itemView.setPadding(15,0,12,2);
            contactName.setTypeface(Typeface.DEFAULT_BOLD);
            msgText.setTypeface(Typeface.DEFAULT_BOLD);
            imageView.setVisibility(View.VISIBLE);
            seen.setVisibility(View.INVISIBLE);
        }
    }
}
