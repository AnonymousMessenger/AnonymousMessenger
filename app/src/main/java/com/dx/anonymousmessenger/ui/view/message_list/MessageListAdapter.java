package com.dx.anonymousmessenger.ui.view.message_list;

import static androidx.core.content.ContextCompat.getColor;
import static androidx.core.content.ContextCompat.getDrawable;
import static androidx.core.content.ContextCompat.getMainExecutor;
import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.startActivity;
import static com.dx.anonymousmessenger.file.FileHelper.getAudioFileLengthInSeconds;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RecoverySystem;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.BuildConfig;
import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.media.AudioPlayer;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.ui.custom.InternalLinkMovementMethod;
import com.dx.anonymousmessenger.ui.view.single_activity.PictureViewerActivity;
import com.dx.anonymousmessenger.util.CallBack;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public class MessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_MESSAGE_SENT_OK = 3;
    private static final int VIEW_TYPE_MESSAGE_SENT_QUOTE = 4;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED_QUOTE = 5;
    private static final int VIEW_TYPE_MESSAGE_SENT_OK_QUOTE = 6;
    private static final int VIEW_TYPE_AUDIO_MESSAGE_SENT = 7;
    private static final int VIEW_TYPE_AUDIO_MESSAGE_SENT_OK = 8;
    private static final int VIEW_TYPE_AUDIO_MESSAGE_RECEIVED = 9;
    private static final int VIEW_TYPE_MEDIA_MESSAGE_SENT = 10;
    private static final int VIEW_TYPE_MEDIA_MESSAGE_SENT_OK = 11;
    private static final int VIEW_TYPE_MEDIA_MESSAGE_RECEIVED = 12;
    private static final int VIEW_TYPE_VIDEO_MESSAGE_SENT = 13;
    private static final int VIEW_TYPE_VIDEO_MESSAGE_SENT_OK = 14;
    private static final int VIEW_TYPE_VIDEO_MESSAGE_RECEIVED = 15;
    private static final int VIEW_TYPE_FILE_MESSAGE_SENT = 16;
    private static final int VIEW_TYPE_FILE_MESSAGE_SENT_OK = 17;
    private static final int VIEW_TYPE_FILE_MESSAGE_RECEIVED = 18;
    private static final int VIEW_TYPE_CALL_INCOMING = 19;
    private static final int VIEW_TYPE_CALL_OUTGOING = 20;
    private static final int PENDING_REMOVAL_TIMEOUT = 3000;

    private final Context mContext;
    private final RecyclerView mMessageRecycler;
    private List<QuotedUserMessage> mMessageList;
    private final DxApplication app;
    private String nowPlaying;
    public AudioPlayer ap;
    private final CallBack permissionCallback;
    private final List<QuotedUserMessage> itemsPendingRemoval = new ArrayList<>();
    private boolean undoOn = true;
    private final Handler handler = new Handler(); // handler for running delayed runnables
    final HashMap<QuotedUserMessage, Runnable> pendingRunnables = new HashMap<>(); // map of items to pending runnables, so we can cancel a removal if need be

    public MessageListAdapter(Context context, List<QuotedUserMessage> messageList, DxApplication app, RecyclerView mMessageRecycler, CallBack permissionCallback) {
        this.app = app;
        mContext = context;
        mMessageList = messageList;
        this.mMessageRecycler = mMessageRecycler;
        this.permissionCallback = permissionCallback;
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(mContext.getApplicationContext()));
        mMessageRecycler.setAdapter(this);
        setUpItemTouchHelper();
        setUpAnimationDecoratorHelper();
    }

    public void setMessageList(List<QuotedUserMessage> mMessageList) {
        this.mMessageList = mMessageList;
    }

    public List<QuotedUserMessage> getMessageList(){
        return mMessageList;
    }

    public void removeData(int position) {
        if(position>=mMessageList.size()){
            return;
        }
        mMessageList.remove(position);
        notifyItemRemoved(position);
//        notifyItemChanged(position);
        notifyItemRangeChanged(position,getItemCount());
//        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        QuotedUserMessage message = mMessageList.get(position);
        // If the current user is the sender of the message
        if(message.getAddress()==null){
            DbHelper.deleteMessage(message,app);
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
        //if we sent the message
        if (message.getAddress().equals(app.getHostname())) {
            //if the message was delivered
            if(message.isReceived()){
                if(message.getType()!=null && message.getType().equals("file")){
                    return VIEW_TYPE_FILE_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals("image")){
                    return VIEW_TYPE_MEDIA_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO+"")){
                    return VIEW_TYPE_VIDEO_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals("call")){
                    return VIEW_TYPE_CALL_OUTGOING;
                }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                    return VIEW_TYPE_MESSAGE_SENT_OK;
                }else if(message.getQuotedMessage()!=null){
                    return VIEW_TYPE_MESSAGE_SENT_OK_QUOTE;
                }
                return VIEW_TYPE_MESSAGE_SENT_OK;
            }
            //if the message was not delivered
            else{
                if(message.getType()!=null && message.getType().equals("file")) {
                    return VIEW_TYPE_FILE_MESSAGE_SENT;
                }else if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_SENT;
                }else if(message.getType()!=null && message.getType().equals("image")){
                    return VIEW_TYPE_MEDIA_MESSAGE_SENT;
                }else if(message.getType()!=null && message.getType().equals(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO+"")){
                    return VIEW_TYPE_VIDEO_MESSAGE_SENT;
                }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                    return VIEW_TYPE_MESSAGE_SENT;
                }else if(message.getQuotedMessage()!=null){
                    return VIEW_TYPE_MESSAGE_SENT_QUOTE;
                }
                return VIEW_TYPE_MESSAGE_SENT;
            }
        } else {
            // If some other user sent the message
            if(message.getType()!=null && message.getType().equals("file")){
                return VIEW_TYPE_FILE_MESSAGE_RECEIVED;
            }else if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_RECEIVED;
            }else if(message.getType()!=null && message.getType().equals("image")){
                return VIEW_TYPE_MEDIA_MESSAGE_RECEIVED;
            }else if(message.getType()!=null && message.getType().equals(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO+"")){
                return VIEW_TYPE_VIDEO_MESSAGE_RECEIVED;
            }else if(message.getType()!=null && message.getType().equals("call")){
                return VIEW_TYPE_CALL_INCOMING;
            }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }else if(message.getQuotedMessage()!=null){
                return VIEW_TYPE_MESSAGE_RECEIVED_QUOTE;
            }
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT_OK) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_ok, parent, false);
            return new MessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_ok, parent, false);
            return new MessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_SENT_OK_QUOTE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_ok_quote, parent, false);
            return new QuoteMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED_QUOTE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received_quote, parent, false);
            return new QuoteMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_SENT_QUOTE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_ok_quote, parent, false);
            return new QuoteMessageHolder(view);
        }else if(viewType == VIEW_TYPE_AUDIO_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_message_sent_ok, parent, false);
            return new AudioMessageHolder(view);
        }else if(viewType == VIEW_TYPE_AUDIO_MESSAGE_SENT_OK){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_message_sent_ok, parent, false);
            return new AudioMessageHolder(view);
        }else if(viewType == VIEW_TYPE_AUDIO_MESSAGE_RECEIVED){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_message_received, parent, false);
            return new AudioMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MEDIA_MESSAGE_RECEIVED){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_received, parent, false);
            return new MediaMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MEDIA_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_sent_ok, parent, false);
            return new MediaMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MEDIA_MESSAGE_SENT_OK){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_sent_ok, parent, false);
            return new MediaMessageHolder(view);
        }
        /*else if(viewType == VIEW_TYPE_VIDEO_MESSAGE_RECEIVED){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_sent_ok, parent, false);
            return new VideoMessageHolder(view);
        }else if(viewType == VIEW_TYPE_VIDEO_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_sent_ok, parent, false);
            return new VideoMessageHolder(view);
        }else if(viewType == VIEW_TYPE_VIDEO_MESSAGE_SENT_OK){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_sent_ok, parent, false);
            return new VideoMessageHolder(view);
        }*/
        else if(viewType == VIEW_TYPE_FILE_MESSAGE_RECEIVED){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_message_received, parent, false);
            return new FileMessageHolder(view);
        }else if(viewType == VIEW_TYPE_FILE_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_message_sent_ok, parent, false);
            return new FileMessageHolder(view);
        }else if(viewType == VIEW_TYPE_FILE_MESSAGE_SENT_OK){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_message_sent_ok, parent, false);
            return new FileMessageHolder(view);
        }else if(viewType == VIEW_TYPE_CALL_INCOMING){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_call_message_received, parent, false);
            return new CallMessageHolder(view);
        }else if(viewType == VIEW_TYPE_CALL_OUTGOING){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_call_message_sent, parent, false);
            return new CallMessageHolder(view);
        }
        Log.e("finding message type","something went wrong");
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_sent_ok, parent, false);
        return new MessageHolder(view);
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        QuotedUserMessage message = mMessageList.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
            case VIEW_TYPE_MESSAGE_SENT_OK:
                ((MessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_SENT_OK_QUOTE:
            case VIEW_TYPE_MESSAGE_SENT_QUOTE:
            case VIEW_TYPE_MESSAGE_RECEIVED_QUOTE:
                ((QuoteMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_AUDIO_MESSAGE_SENT:
            case VIEW_TYPE_AUDIO_MESSAGE_SENT_OK:
            case VIEW_TYPE_AUDIO_MESSAGE_RECEIVED:
                ((AudioMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MEDIA_MESSAGE_RECEIVED:
            case VIEW_TYPE_MEDIA_MESSAGE_SENT:
            case VIEW_TYPE_MEDIA_MESSAGE_SENT_OK:
                ((MediaMessageHolder) holder).bind(message);
                break;
            /*case VIEW_TYPE_VIDEO_MESSAGE_RECEIVED:
            case VIEW_TYPE_VIDEO_MESSAGE_SENT:
            case VIEW_TYPE_VIDEO_MESSAGE_SENT_OK:
                ((VideoMessageHolder) holder).bind(message);
                break;*/
            case VIEW_TYPE_FILE_MESSAGE_RECEIVED:
            case VIEW_TYPE_FILE_MESSAGE_SENT:
            case VIEW_TYPE_FILE_MESSAGE_SENT_OK:
                ((FileMessageHolder) holder).bind(message,app,permissionCallback);
                break;
            case VIEW_TYPE_CALL_INCOMING:
            case VIEW_TYPE_CALL_OUTGOING:
                 ((CallMessageHolder) holder).bind(message);
        }
        if (itemsPendingRemoval.contains(message)) {
            // we need to show the "undo" state of the row
            holder.itemView.setBackgroundColor(Color.RED);
            ((MessageHolder)holder).undoButton.setVisibility(View.VISIBLE);
            ((MessageHolder)holder).undoButton.setElevation(R.dimen.margin_large);
            ((MessageHolder)holder).undoButton.setOnClickListener(v -> {
                // user wants to undo the removal, let's cancel the pending task
                Runnable pendingRemovalRunnable = pendingRunnables.get(message);
                pendingRunnables.remove(message);
                if (pendingRemovalRunnable != null) {
                    handler.removeCallbacks(pendingRemovalRunnable);
                }
                itemsPendingRemoval.remove(message);
                // this will rebind the row in "normal" state
                notifyItemChanged(mMessageList.indexOf(message));
            });
        } else {
            // we need to show the "normal" state
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            if (holder instanceof MessageHolder) {
                ((MessageHolder)holder).undoButton.setVisibility(View.GONE);
                ((MessageHolder)holder).undoButton.setOnClickListener(null);
            }
        }
    }

    //on click listeners
    public class ListItemOnClickListener implements View.OnClickListener {
        final QuotedUserMessage message;
        final View itemView;
        final TextView messageText;

        ListItemOnClickListener(QuotedUserMessage message,View itemView,TextView messageText){
            this.itemView = itemView;
            this.message = message;
            this.messageText = messageText;
        }

        @Override
        public void onClick(View v) {
            RecyclerView rv = ((MessageListActivity) mContext).findViewById(R.id.reyclerview_message_list);
            rv.scrollToPosition(mMessageList.indexOf(message));
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.options_menu);
            if(message.isPinned()){
                MenuItem pinButton = popup.getMenu().findItem(R.id.navigation_drawer_item2);
                pinButton.setTitle(R.string.unpin);
            }
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.navigation_drawer_item1){
                    //handle reply click
                    replyTo((MessageListActivity) mContext,message);
//                        RecyclerView rv = ((MessageListActivity) mContext).findViewById(R.id.reyclerview_message_list);
//                        rv.smoothScrollToPosition(mMessageList.size() - 1);
                    return true;
                } else if(item.getItemId() == R.id.navigation_drawer_item2){
                    //handle pin click
                    handlePin(app,(MessageListActivity)mContext,message);
                    return true;
                } else if(item.getItemId() == R.id.navigation_drawer_item3){
                    //handle copy click
                    ClipboardManager clipboard = getSystemService(Objects.requireNonNull(mContext), ClipboardManager.class);
                    ClipData clip = ClipData.newPlainText("label", messageText.getText().toString());
                    Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                    @SuppressLint("ShowToast") Snackbar sb = Snackbar.make(rv, R.string.copied, Snackbar.LENGTH_SHORT).setAnchorView(((MessageListActivity) mContext).findViewById(R.id.layout_chatbox));
                    sb.show();
                    return true;
                }
                return false;
            });
            //displaying the popup
            popup.show();
        }




    }

    public class QuoteItemOnClickListener implements View.OnClickListener {
        final QuotedUserMessage message;
        final View itemView;

        QuoteItemOnClickListener(QuotedUserMessage message,View itemView){
            this.itemView = itemView;
            this.message = message;
        }

        @Override
        public void onClick(View v) {
            new Thread(()->{
                String type = "";
                String createdAt;
                long time = 0;
                if(message.getQuotedMessage().split(":").length>1){
                    type = message.getQuotedMessage().split(":")[0];
                    if(!QuotedUserMessage.isValidType(type)){
                        type = "";
                    }
                    createdAt = message.getQuotedMessage().split(":")[1];
                    try{
                        time = Long.parseLong(createdAt);
                    }catch (Exception ignored){

                    }
                }

                for (QuotedUserMessage msg : mMessageList) {
                    if(!type.equals("")){
                        if(msg.getType()!=null && msg.getType().equals(type) && msg.getCreatedAt()==time
                                && msg.getSender().equals(message.getQuoteSender())){
                            if(!mMessageList.contains(msg)){
                                return;
                            }
                            Handler h = new Handler(Looper.getMainLooper());
                            h.post(()-> {
                                mMessageRecycler.scrollToPosition(mMessageList.indexOf(msg));
                                notifyItemChanged(mMessageList.indexOf(msg));
                            });
                            return;
                        }
                    }else{
                        if(msg.getMessage().equals(message.getQuotedMessage())
                                && msg.getSender().equals(message.getQuoteSender())){
                            if(!mMessageList.contains(msg)){
                                return;
                            }
                            Handler h = new Handler(Looper.getMainLooper());
                            h.post(()-> {
                                mMessageRecycler.scrollToPosition(mMessageList.indexOf(msg));
                                notifyItemChanged(mMessageList.indexOf(msg));
                            });
                            return;
                        }
                    }
                }
                Handler h = new Handler(Looper.getMainLooper());
                h.post(()-> Toast.makeText(app, "can't find original message", Toast.LENGTH_SHORT).show());
            }).start();
        }
    }

    public class AudioItemOnClickListener implements View.OnClickListener, CallBack {
        final QuotedUserMessage message;
        final View itemView;
        final ImageView playPauseButton;

        AudioItemOnClickListener(QuotedUserMessage message,View itemView,ImageView playPauseButton){
            this.itemView = itemView;
            this.message = message;
            this.playPauseButton = playPauseButton;
        }

        @Override
        public void onClick(View v) {
            if(nowPlaying!=null && nowPlaying.equals(message.getPath())){
                playPauseButton.setImageDrawable(getDrawable(mContext,R.drawable.ic_baseline_play_arrow_24));
                nowPlaying = null;
                //stop playing
                if(ap!=null){
                    ap.stop();
                    ap = null;
                }
                notifyItemChanged(mMessageList.indexOf(message));
                return;
            }
            // if np not null stop first one
            if(ap!=null){
                ap.stop();
                ap = null;
            }
            if(nowPlaying!=null){
                //stop playing
                for (QuotedUserMessage msg : mMessageList) {
                    if(msg.getPath()!=null && msg.getPath().equals(nowPlaying)){
                        nowPlaying = null;
                        notifyItemChanged(mMessageList.indexOf(msg));
                        break;
                    }
                }
            }

            nowPlaying = message.getPath();
            playPauseButton.setImageDrawable(getDrawable(mContext,R.drawable.ic_baseline_pause_24));
            notifyItemChanged(mMessageList.indexOf(message));
            //start playing
            ap = new AudioPlayer(app,message.getPath());
            ap.registerCallBack(this);
            new Thread(ap::play).start();
        }

        @Override
        public void doStuff() {
            try{
                Handler mainHandler = new Handler(app.getMainLooper());
                Runnable myRunnable = () -> {
                    try{
                        //stop playing
                        if(ap!=null){
                            ap = null;
                        }
                        nowPlaying = null;
                        notifyDataSetChanged();
                        notifyItemChanged(mMessageList.indexOf(message));
                        playPauseButton.setImageDrawable(getDrawable(mContext,R.drawable.ic_baseline_play_arrow_24));
                        //todo if screen on : play the next one
                    }catch (Exception e) {e.printStackTrace();}
                };
                mainHandler.post(myRunnable);
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    //message holders
    private class FileMessageHolder extends MessageHolder{
        final TextView filenameText;
        final FloatingActionButton imageHolder;
        final ProgressBar fileProgress;

        FileMessageHolder(View itemView){
            super(itemView);
            imageHolder = itemView.findViewById(R.id.img_holder);
            filenameText = itemView.findViewById(R.id.txt_filename);
            fileProgress = itemView.findViewById(R.id.progress_file);
        }

        void saveFile(QuotedUserMessage message, DxApplication app, CallBack permissionCallback){
            new AlertDialog.Builder(itemView.getContext(),R.style.AppAlertDialog)
                .setTitle(R.string.save_to_storage)
                .setMessage(app.getString(R.string.save_to_storage_explain))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(() -> {
                try{
                    RecoverySystem.ProgressListener progressListener = progress -> {
                        Executor exe = getMainExecutor(itemView.getContext());
                        exe.execute(()->{
                            if(progress>=100){
                                filenameText.setVisibility(View.VISIBLE);
                                imageHolder.setVisibility(View.VISIBLE);
                                fileProgress.setVisibility(View.GONE);
                            }else{
                                filenameText.setVisibility(View.INVISIBLE);
                                imageHolder.setVisibility(View.INVISIBLE);
                                fileProgress.setVisibility(View.VISIBLE);
                                fileProgress.setProgress(progress);
                            }
                        });
                    };
                    if (ContextCompat.checkSelfPermission(itemView.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        //no external storage permission
                        permissionCallback.doStuff();
                        return;
                    }
                    FileHelper.saveToStorageWithProgress(message.getPath(),message.getFilename(),app,progressListener);

                    @SuppressLint("ShowToast") Snackbar sb = Snackbar.make(itemView,R.string.saved_to_storage,Snackbar.LENGTH_SHORT).setAnchorView(itemView.findViewById(R.id.layout_chatbox));
                    sb.show();

                }catch (Exception e){
                    e.printStackTrace();
                    Executor exe = getMainExecutor(itemView.getContext());
                    exe.execute(()->{
                        filenameText.setText(message.getFilename());
                        Toast.makeText(itemView.getContext(),"Something went wrong",Toast.LENGTH_SHORT).show();
                    });
                }
            }).start())
            .setNegativeButton(android.R.string.no, null)
            .show();
        }

        void openFile(QuotedUserMessage message, DxApplication app){
            new AlertDialog.Builder(itemView.getContext(),R.style.AppAlertDialog)
                .setTitle(R.string.open_file_question)
                .setMessage(app.getString(R.string.open_file_describe))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(() -> {
                try{
                    RecoverySystem.ProgressListener progressListener = progress -> {
                        Executor exe = getMainExecutor(itemView.getContext());
                        exe.execute(()->{
                            if(progress>=100){
                                filenameText.setVisibility(View.VISIBLE);
                                imageHolder.setVisibility(View.VISIBLE);
                                fileProgress.setVisibility(View.GONE);
                            }else{
                                filenameText.setVisibility(View.INVISIBLE);
                                imageHolder.setVisibility(View.INVISIBLE);
                                fileProgress.setVisibility(View.VISIBLE);
                                fileProgress.setProgress(progress);
                            }
                        });
                    };
                    String suffix = "."+message.getFilename().split("\\.")[message.getFilename().split("\\.").length-1];
                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix.replace(".",""));
                    if(mime==null){
                        mime = "*/*";
                    }
                    File tmp = FileHelper.getTempFileWithProgress(message.getPath(),message.getFilename(),app,progressListener);
                    Uri uri;
                    if (tmp != null) {
                        uri = FileProvider.getUriForFile(app, BuildConfig.APPLICATION_ID+".fileprovider", new File(tmp.getAbsolutePath()));
                    }else{
                        return;
                    }
                    app.grantUriPermission(app.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    //open tmp file with third party
                    Intent objIntent = new Intent(Intent.ACTION_VIEW);
                    objIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    objIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    objIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    objIntent.setDataAndType(uri, mime);
                    startActivity(app,objIntent,null);
                }catch (Exception e){
                    e.printStackTrace();
                    Executor exe = getMainExecutor(itemView.getContext());
                    exe.execute(()->{
                        filenameText.setText(message.getFilename());
                        Toast.makeText(itemView.getContext(),"No default program found for this type of file",Toast.LENGTH_SHORT).show();
                    });
                }
            }).start())
            .setNegativeButton(android.R.string.no, null)
            .show();
        }

        void onClick(View v, QuotedUserMessage message, DxApplication app, CallBack permissionCallback){
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.file_menu);
            if(message.isPinned()){
                MenuItem pinButton = popup.getMenu().findItem(R.id.navigation_drawer_item2);
                pinButton.setTitle(R.string.unpin);
            }
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.navigation_drawer_item2) {
                    //handle pin click
                    try {
                        if (message.isPinned()) {
                            DbHelper.unPinMessage(message, app);
                            message.setPinned(false);
                            @SuppressLint("ShowToast") Snackbar sb = Snackbar.make(v, R.string.unpinned_message, Snackbar.LENGTH_SHORT).setAnchorView(v.findViewById(R.id.layout_chatbox));
                            sb.show();
                        } else {
                            DbHelper.pinMessage(message, app);
                            message.setPinned(true);
                            @SuppressLint("ShowToast") Snackbar sb = Snackbar.make(v, R.string.pinned_message, Snackbar.LENGTH_SHORT).setAnchorView(v.findViewById(R.id.layout_chatbox));
                            sb.show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }else if(item.getItemId() == R.id.open_file){
                    //handle open file
                    openFile(message,app);
                }else if(item.getItemId() == R.id.save_file){
                    //handle save file
                    saveFile(message,app,permissionCallback);
                }
                return false;
            });
            popup.show();
        }

        void bind(QuotedUserMessage message, DxApplication app, CallBack permissionCallback) {
            super.bind(message);

            filenameText.setText(message.getFilename());
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            imageHolder.setOnClickListener(v -> onClick(v,message,app,permissionCallback));
            filenameText.setOnClickListener(v -> onClick(v,message,app,permissionCallback));
        }
    }

//    private class VideoMessageHolder extends MediaMessageHolder {
//
//        VideoMessageHolder(View itemView) {
//            super(itemView);
//        }
//
//        @Override
//        void bind(QuotedUserMessage message) {
//            if(nameText!=null){
//                nameText.setText(message.getSender());
//            }
//            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
//            if(message.getMessage()!=null && !message.getMessage().equals("")){
//                messageText.setVisibility(View.VISIBLE);
//                messageText.setText(message.getMessage());
//                messageText.setOnClickListener(new ListItemOnClickListener(message,itemView,messageText));
//            }else{
//                messageText.setVisibility(View.GONE);
//            }
//            imageHolder.setOnClickListener(v -> {
//                Intent intent = new Intent(app, PictureViewerActivity.class);
//                intent.putExtra("address",message.getAddress().substring(0,10));
//                intent.putExtra("nickname",message.getSender());
//                intent.putExtra("time",message.getCreatedAt());
//                intent.putExtra("appData",true);
//                intent.putExtra("type",MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
//                intent.putExtra("path",message.getPath());
//                intent.putExtra("message",message.getMessage());
//                app.startActivity(intent);
//            });
//            new Thread(()->{
//                try{
//                    final byte[] img_bin = FileHelper.getFile(message.getPath(), app);
//                    if(img_bin == null){
//                        Log.d("ANONYMOUSMESSENGER","no img_bin!!!");
//                        return;
//                    }
//                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//                    mmr.setDataSource(new MediaDataSource() {
//                        @Override
//                        public int readAt(long position, byte[] buffer, int offset, int size) {
//                            ByteArrayInputStream bais = new ByteArrayInputStream(img_bin);
//                            bais.skip(position-1);
//                            return bais.read(buffer,offset,size);
//                        }
//
//                        @Override
//                        public long getSize() {
//                            return img_bin.length;
//                        }
//
//                        @Override
//                        public void close() {
//
//                        }
//                    });
//                    byte[] thumb = mmr.getEmbeddedPicture();
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(thumb,0,thumb.length);
//                    if(bitmap==null){
//                        Log.d("ANONYMOUSMESSENGER","no bitmap!!!");
//                        return;
//                    }
//                    new Handler(Looper.getMainLooper()).post(()->{
//                        if(imageHolder==null){
//                            return;
//                        }
//                        imageHolder.setImageBitmap(bitmap);
//                    });
//                }catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }).start();
//        }
//    }

    private class MediaMessageHolder extends MessageHolder {
        final ImageView imageHolder;
        final ImageView sent;
        final ProgressBar progress;

        MediaMessageHolder(View itemView){
            super(itemView);
            imageHolder = itemView.findViewById(R.id.img_holder);
            sent = itemView.findViewById(R.id.img_sent);
            progress = itemView.findViewById(R.id.progress_image);
        }

        void bind(QuotedUserMessage message) {
            super.bind(message);
            if(sent != null && message.isReceived()){
                sent.setVisibility(View.VISIBLE);
            }
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            if(message.getMessage()!=null && !message.getMessage().equals("")){
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getMessage());
                messageText.setOnClickListener(new ListItemOnClickListener(message,itemView,messageText));
            }else{
                messageText.setVisibility(View.GONE);
            }
            new Thread(()->{
                byte[] img_bin = FileHelper.getFile(message.getPath(), app);
                if(img_bin == null){
                    return;
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                if(img_bin.length>(512*1024)){
                    options.inSampleSize = 4;
                }
                Bitmap bitmap = BitmapFactory.decodeByteArray(img_bin, 0, img_bin.length, options);
                if(bitmap==null){
                    return;
                }
                new Handler(Looper.getMainLooper()).post(()->{
                    if(imageHolder==null){
                        return;
                    }
                    progress.setVisibility(View.GONE);
                    imageHolder.setImageBitmap(bitmap);
                    imageHolder.setOnClickListener(v -> {
                        Intent intent = new Intent(app, PictureViewerActivity.class);
                        intent.putExtra("address",message.getAddress().substring(0,10));
                        intent.putExtra("nickname",message.getSender());
                        intent.putExtra("time",message.getCreatedAt());
                        intent.putExtra("appData",true);
                        intent.putExtra("path",message.getPath());
                        intent.putExtra("message",message.getMessage());
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation((MessageListActivity) mContext, v, "picture");
                        v.getContext().startActivity(intent,activityOptions.toBundle());
                    });
                });
            }).start();
        }
    }

    private class AudioMessageHolder extends MessageHolder {
        final TextView sizeText;
        final FloatingActionButton playPauseButton;

        AudioMessageHolder(View itemView){
            super(itemView);
            playPauseButton = itemView.findViewById(R.id.btn_play_pause);
            sizeText = itemView.findViewById(R.id.txt_audio_size);
        }

        @SuppressLint("DefaultLocale")
        void bind(QuotedUserMessage message) {
            super.bind(message);
            if(sizeText!=null){
                sizeText.setText(String.format("%ds", getAudioFileLengthInSeconds(message.getPath(), mContext)));
            }
            if(nowPlaying!=null && nowPlaying.equals(message.getPath())){
                playPauseButton.setImageDrawable(getDrawable(mContext,R.drawable.ic_baseline_pause_24));
            }else {
                playPauseButton.setImageDrawable(getDrawable(mContext,R.drawable.ic_baseline_play_arrow_24));
            }
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            playPauseButton.setOnClickListener(new AudioItemOnClickListener(message,itemView,playPauseButton));
        }
    }

    private class CallMessageHolder extends MessageHolder {
        final TextView nameText;
        final TextView timeText;
        final Button undoButton;

        CallMessageHolder(View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.text_message_time);
            undoButton = itemView.findViewById(R.id.undo_button);
            nameText = itemView.findViewById(R.id.text_message_name);
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(QuotedUserMessage message) {
            if(nameText!=null){
                nameText.setText(message.getSender());
            }
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
        }
    }

    private class MessageHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView messageText;
        final TextView timeText;
        final Button undoButton;
        final ImageView sent;
        final ImageView profileImage;
        final FloatingActionButton pin;

        MessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            undoButton = itemView.findViewById(R.id.undo_button);
            nameText = itemView.findViewById(R.id.text_message_name);
            sent = itemView.findViewById(R.id.img_sent);
            pin = itemView.findViewById(R.id.fab_pin);
            profileImage = itemView.findViewById(R.id.image_message_profile);
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(QuotedUserMessage message) {
            if(sent != null && message.isReceived()){
                sent.setVisibility(View.VISIBLE);
            }
            //if msg before this one was also received or has the same sender then don't display extra crap
            if(nameText!=null && this.getAbsoluteAdapterPosition()>0 && (mMessageList.get(getAbsoluteAdapterPosition()-1).getSender().equals(message.getSender()))){
                nameText.setVisibility(View.GONE);
                profileImage.setVisibility(View.GONE);
            }else if(nameText!=null){
                nameText.setText(message.getSender());
                if(profileImage!=null){
                    setIsRecyclable(false);
                    new Thread(()->{
                        try{
                            String path = DbHelper.getContactProfileImagePath(message.getAddress(),app);
                            if(path == null){
                                throw new Resources.NotFoundException("");
                            }
                            if(path.equals("")){
                                throw new Resources.NotFoundException("");
                            }
                            byte[] image = FileHelper.getFile(path, app);
                            if (image == null) {
                                throw new Resources.NotFoundException("");
                            }
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 8;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length, options);
                            new Handler(Looper.getMainLooper()).post(()->{
                                profileImage.setImageBitmap(bitmap);
                                setIsRecyclable(true);
                            });
                        }catch (Exception ignored){
                            setIsRecyclable(true);
                        }
                    }).start();
                }
            }

            if(pin != null){
                if(message.isPinned()){
                    pin.setAlpha(1.0f);
                }else{
                    pin.setAlpha(0.2f);
                }
                pin.setOnClickListener(v -> handlePin(app,(MessageListActivity) mContext, message));
            }

            if(messageText == null){
                return;
            }
            Spannable span = new SpannableString(message.getMessage());
            //detect start-finish of emoji and span it bigger
            int charStart = -1;
            for (int i = 0; i < span.length(); i++) {
                int type = Character.getType(span.charAt(i));
                if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                    if(i==(span.length()-1)){
                        if(charStart>=0){
                            span.setSpan(new RelativeSizeSpan(3f), charStart, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }else{
                            span.setSpan(new RelativeSizeSpan(3f), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        charStart = -1;
                    }else if (charStart < 0) {
                        charStart = i;
                    }
                }else{
                    if(charStart>=0){
                        span.setSpan(new RelativeSizeSpan(3f), charStart, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        charStart = -1;
                    }
                }
            }
            messageText.setText(span);
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            messageText.setOnClickListener(new ListItemOnClickListener(message,itemView,messageText));
            InternalLinkMovementMethod.OnLinkClickedListener linkClickedListener = linkText -> {
                new AlertDialog.Builder(mContext,R.style.AppAlertDialog)
                    .setTitle(R.string.open_link_question)
                    .setMessage(R.string.open_link_describe)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        Intent defaultIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkText));
                        mContext.startActivity(defaultIntent);
                    })
                    .setNegativeButton(android.R.string.no, (dialog, whichButton) ->{}).show();
                // return true if handled, false otherwise
                return true;
            };
            messageText.setMovementMethod(new InternalLinkMovementMethod(linkClickedListener));
        }
    }

    private class ReceivedMessageHolder extends MessageHolder {
        final TextView nameText;
        final ImageView profileImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_message_name);
            profileImage = itemView.findViewById(R.id.image_message_profile);
        }

        @Override
        void bind(QuotedUserMessage message) {
            super.bind(message);
            nameText.setText(message.getSender());
            new Thread(()->{
                try{
                    byte[] image = FileHelper.getFile(DbHelper.getContactProfileImagePath(message.getAddress(),app), app);

                    if (image == null) {
                        return;
                    }
                    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                    new Handler(Looper.getMainLooper()).post(()-> profileImage.setImageBitmap(bitmap));
                }catch (Exception ignored){
//                    e.printStackTrace();
                }
            }).start();
            // Insert the profile image from the URL into the ImageView.util
            // .displayRoundImageFromUrl(mContext, util.Utils.getInitials(message.getSender()), profileImage);
        }
    }

    private class QuoteMessageHolder extends MessageHolder{
        final TextView quote;
        final TextView quoteSender;
        QuoteMessageHolder(View itemView) {
            super(itemView);
            quote = itemView.findViewById(R.id.quote_text);
            quoteSender = itemView.findViewById(R.id.quote_sender);
        }

        @Override
        void bind(QuotedUserMessage message) {
            super.bind(message);
            //todo: change text to something like message type in language
            quote.setText(message.getQuotedMessage());
            quoteSender.setText(message.getQuoteSender().equals(app.getAccount().getNickname())?"You":message.getQuoteSender());
            quote.setOnClickListener(new QuoteItemOnClickListener(message,itemView));
            quoteSender.setOnClickListener(new QuoteItemOnClickListener(message,itemView));
        }
    }
    /**
     * This is the standard support library way of implementing "swipe to delete" feature. You can do custom drawing in onChildDraw method
     * but whatever you draw will disappear once the swipe is over, and while the items are animating to their new position the recycler view
     * background will be visible. That is rarely an desired effect.
     */
    private void setUpItemTouchHelper() {

        //swipe left to delete
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                xMark = ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_delete_24);
                Objects.requireNonNull(xMark).setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) mContext.getResources().getDimension(R.dimen.ic_clear_margin);
                initiated = true;
            }

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAbsoluteAdapterPosition();
                if (isUndoOn() && isPendingRemoval(position)) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int swipedPosition = viewHolder.getAbsoluteAdapterPosition();
                if (isUndoOn()) {
                    pendingRemoval(swipedPosition);
                } else {
                    remove(swipedPosition);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAbsoluteAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                if (!initiated) {
                    init();
                }

                // draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // draw x mark
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = xMark.getIntrinsicWidth();
                int intrinsicHeight = xMark.getIntrinsicWidth();

                int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
                int xMarkRight = itemView.getRight() - xMarkMargin;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                xMark.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };

        //swipe right to reply
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback2 = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            // we want to cache these and not allocate anything repeatedly in the onChildDraw method
            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(getColor(mContext,R.color.dx_night_700));
                xMark = ContextCompat.getDrawable(mContext, R.drawable.ic_baseline_reply_24);
                Objects.requireNonNull(xMark).setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) mContext.getResources().getDimension(R.dimen.ic_clear_margin);
                initiated = true;
            }

            // not important, we don't want drag & drop
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int swipedPosition = viewHolder.getAbsoluteAdapterPosition();
                replyTo((MessageListActivity) mContext,mMessageList.get(swipedPosition));
                notifyItemChanged(mMessageList.indexOf(mMessageList.get(swipedPosition)));
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAbsoluteAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                if (!initiated) {
                    init();
                }

                // draw red background
                background.setBounds(itemView.getLeft() + (int) dX, itemView.getTop(), itemView.getLeft(), itemView.getBottom());
                background.draw(c);

                // draw x mark
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = xMark.getIntrinsicWidth();
                int intrinsicHeight = xMark.getIntrinsicWidth();

                int xMarkLeft = itemView.getLeft() + xMarkMargin;
                int xMarkRight = itemView.getLeft() + xMarkMargin + intrinsicWidth;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                xMark.draw(c);

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mMessageRecycler);
        ItemTouchHelper mItemTouchHelper2 = new ItemTouchHelper(simpleItemTouchCallback2);
        mItemTouchHelper2.attachToRecyclerView(mMessageRecycler);
    }

    /**
     * We're gonna setup another ItemDecorator that will draw the red background in the empty space while the items are animating to thier new positions
     * after an item is removed.
     */
    private void setUpAnimationDecoratorHelper() {
        mMessageRecycler.addItemDecoration(new RecyclerView.ItemDecoration() {

            // we want to cache this and not allocate anything repeatedly in the onDraw method
            Drawable background;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(Color.RED);
                initiated = true;
            }

            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

                if (!initiated) {
                    init();
                }

                // only if animation is in progress
                if (Objects.requireNonNull(parent.getItemAnimator()).isRunning()) {

                    // some items might be animating down and some items might be animating up to close the gap left by the removed item
                    // this is not exclusive, both movement can be happening at the same time
                    // to reproduce this leave just enough items so the first one and the last one would be just a little off screen
                    // then remove one from the middle

                    // find first child with translationY > 0
                    // and last one with translationY < 0
                    // we're after a rect that is not covered in recycler-view views at this point in time
                    View lastViewComingDown = null;
                    View firstViewComingUp = null;

                    // this is fixed
                    int left = 0;
                    int right = parent.getWidth();

                    // this we need to find out
                    int top = 0;
                    int bottom = 0;

                    // find relevant translating views
                    int childCount = Objects.requireNonNull(parent.getLayoutManager()).getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (Objects.requireNonNull(child).getTranslationY() < 0) {
                            // view is coming down
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            // view is coming up
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }

                    if (lastViewComingDown != null && firstViewComingUp != null) {
                        // views are coming down AND going up to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    } else if (lastViewComingDown != null) {
                        // views are going down to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = lastViewComingDown.getBottom();
                    } else if (firstViewComingUp != null) {
                        // views are coming up to fill the void
                        top = firstViewComingUp.getTop();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    }

                    background.setBounds(left, top, right, bottom);
                    background.draw(c);

                }
                super.onDraw(c, parent, state);
            }

        });
    }

    public void pendingRemoval(int position) {
        try{
            final QuotedUserMessage item = mMessageList.get(position);
            if (!itemsPendingRemoval.contains(item)) {
                itemsPendingRemoval.add(item);
                // this will redraw row in "undo" state
                notifyItemChanged(position);
                // let's create, store and post a runnable to remove the item
                Runnable pendingRemovalRunnable = () -> remove(item);
                handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
                pendingRunnables.put(item, pendingRemovalRunnable);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void remove(QuotedUserMessage item) {
        itemsPendingRemoval.remove(item);
        if (mMessageList.contains(item)) {
            try{
                int position = mMessageList.indexOf(item);
                notifyItemRemoved(position);
                mMessageList.remove(item);
                notifyItemRangeChanged(position,getItemCount());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        new Thread(()->{
            DbHelper.deleteMessage(item,app);
            Intent gcm_rec = new Intent("your_action");
            gcm_rec.putExtra("delete",item.getCreatedAt());
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
        }).start();
    }

    public void remove(int position) {
        try{
            if(position>=mMessageList.size()){
                return;
            }
            QuotedUserMessage item = mMessageList.get(position);
            itemsPendingRemoval.remove(item);
            if (mMessageList.contains(item)) {
                mMessageList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position,getItemCount());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isPendingRemoval(int position) {
        try {
            QuotedUserMessage item = mMessageList.get(position);
            return itemsPendingRemoval.contains(item);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public void setUndoOn(boolean undoOn) {
            this.undoOn = undoOn;
        }

    public boolean isUndoOn() {
        return undoOn;
    }

    private static void replyTo(MessageListActivity activity, QuotedUserMessage message) {
        DxApplication app = (DxApplication) activity.getApplication();
        TextView quoteTextTyping = activity.findViewById(R.id.quote_text_typing);
        TextView quoteSenderTyping = activity.findViewById(R.id.quote_sender_typing);
        if(message.getMessage()!=null && message.getMessage().equals("") && message.getType()!=null && !message.getType().equals("")){
            String ph = message.getType() + ":" + message.getCreatedAt();
            quoteTextTyping.setText(ph);
        }else{
            quoteTextTyping.setText(message.getMessage());
        }
        quoteSenderTyping.setText(message.getTo().equals(app.getHostname())?message.getSender():app.getString(R.string.you));
        quoteTextTyping.setVisibility(View.VISIBLE);
        quoteSenderTyping.setVisibility(View.VISIBLE);
    }

    private void handlePin(DxApplication app, MessageListActivity activity, QuotedUserMessage message) {
        RecyclerView rv = activity.findViewById(R.id.reyclerview_message_list);
        try{
            if(message.isPinned()){
                DbHelper.unPinMessage(message,app);
                message.setPinned(false);
                if(rv != null){
                    @SuppressLint("ShowToast") Snackbar sb = Snackbar.make(rv, R.string.unpinned_message, Snackbar.LENGTH_SHORT).setAnchorView(activity.findViewById(R.id.layout_chatbox));
                    sb.show();
                }
            }else{
                DbHelper.pinMessage(message,app);
                message.setPinned(true);
                if(rv != null){
                    @SuppressLint("ShowToast") Snackbar sb = Snackbar.make(rv, R.string.pinned_message, Snackbar.LENGTH_SHORT).setAnchorView(activity.findViewById(R.id.layout_chatbox));
                    sb.show();
                }
            }
            notifyItemChanged(mMessageList.indexOf(message));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
}