package com.dx.anonymousmessenger.ui.view.message_list;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
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
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import static androidx.core.content.ContextCompat.getDrawable;
import static androidx.core.content.ContextCompat.getMainExecutor;
import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.startActivity;

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

    private final Context mContext;
    private final RecyclerView mMessageRecycler;
    private List<QuotedUserMessage> mMessageList;
    private final DxApplication app;
    private String nowPlaying;
    public AudioPlayer ap;
    private final CallBack permissionCallback;

    public MessageListAdapter(Context context, List<QuotedUserMessage> messageList, DxApplication app, RecyclerView mMessageRecycler, CallBack permissionCallback) {
        this.app = app;
        mContext = context;
        mMessageList = messageList;
        this.mMessageRecycler = mMessageRecycler;
        this.permissionCallback = permissionCallback;
    }

    public void setMessageList(List<QuotedUserMessage> mMessageList) {
        this.mMessageList = mMessageList;
    }

    public void removeData(int position) {
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
        if (message.getAddress().equals(app.getHostname())) {
            if(message.isReceived()){
                if(message.getType()!=null && message.getType().equals("file")){
                    return VIEW_TYPE_FILE_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals("image")){
                    return VIEW_TYPE_MEDIA_MESSAGE_SENT_OK;
                }else if(message.getType()!=null && message.getType().equals(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO+"")){
                    return VIEW_TYPE_VIDEO_MESSAGE_SENT_OK;
                }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                    return VIEW_TYPE_MESSAGE_SENT_OK;
                }else if(message.getQuotedMessage()!=null){
                    return VIEW_TYPE_MESSAGE_SENT_OK_QUOTE;
                }
                return VIEW_TYPE_MESSAGE_SENT_OK;
            }else{
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
            return new SentOkMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_SENT_OK_QUOTE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_ok_quote, parent, false);
            return new QuoteSentOkMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_RECEIVED_QUOTE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received_quote, parent, false);
            return new QuoteReceivedMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MESSAGE_SENT_QUOTE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent_quote, parent, false);
            return new QuoteSentMessageHolder(view);
        }else if(viewType == VIEW_TYPE_AUDIO_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_message_sent, parent, false);
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
                    .inflate(R.layout.item_media_message_sent, parent, false);
            return new MediaMessageHolder(view);
        }else if(viewType == VIEW_TYPE_MEDIA_MESSAGE_SENT_OK){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_message_sent_ok, parent, false);
            return new MediaMessageHolder(view);
        }else if(viewType == VIEW_TYPE_VIDEO_MESSAGE_RECEIVED){
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
        }else if(viewType == VIEW_TYPE_FILE_MESSAGE_RECEIVED){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_message_received, parent, false);
            return new FileMessageHolder(view);
        }else if(viewType == VIEW_TYPE_FILE_MESSAGE_SENT){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_message_sent, parent, false);
            return new FileMessageHolder(view);
        }else if(viewType == VIEW_TYPE_FILE_MESSAGE_SENT_OK){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file_message_sent_ok, parent, false);
            return new FileMessageHolder(view);
        }
        Log.e("finding message type","something went wrong");
        view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_sent, parent, false);
        return new SentMessageHolder(view);
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        QuotedUserMessage message = mMessageList.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_SENT_OK:
                ((SentOkMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_SENT_OK_QUOTE:
                ((QuoteSentOkMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_SENT_QUOTE:
                ((QuoteSentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED_QUOTE:
                ((QuoteReceivedMessageHolder) holder).bind(message);
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
            case VIEW_TYPE_VIDEO_MESSAGE_RECEIVED:
            case VIEW_TYPE_VIDEO_MESSAGE_SENT:
            case VIEW_TYPE_VIDEO_MESSAGE_SENT_OK:
                ((VideoMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_FILE_MESSAGE_RECEIVED:
            case VIEW_TYPE_FILE_MESSAGE_SENT:
            case VIEW_TYPE_FILE_MESSAGE_SENT_OK:
                ((FileMessageHolder) holder).bind(message,app,permissionCallback);
        }
    }

    public class ListItemOnClickListener implements View.OnClickListener {
        QuotedUserMessage message;
        View itemView;
        TextView messageText;

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
                switch (item.getItemId()) {
                    case R.id.navigation_drawer_item1:
                        //handle reply click
                        TextView quoteTextTyping = ((MessageListActivity) mContext).findViewById(R.id.quote_text_typing);
                        TextView quoteSenderTyping = ((MessageListActivity) mContext).findViewById(R.id.quote_sender_typing);
                        quoteTextTyping.setText(message.getMessage());
                        quoteSenderTyping.setText(message.getTo().equals(app.getHostname())?message.getSender():app.getString(R.string.you));
                        quoteTextTyping.setVisibility(View.VISIBLE);
                        quoteSenderTyping.setVisibility(View.VISIBLE);
//                        RecyclerView rv = ((MessageListActivity) mContext).findViewById(R.id.reyclerview_message_list);
//                        rv.smoothScrollToPosition(mMessageList.size() - 1);
                        return true;
                    case R.id.navigation_drawer_item2:
                        //handle pin click
                        try{
                            if(message.isPinned()){
                                DbHelper.unPinMessage(message,app);
                                message.setPinned(false);
                                Snackbar.make(rv, R.string.unpinned_message, Snackbar.LENGTH_SHORT).show();
                            }else{
                                DbHelper.pinMessage(message,app);
                                message.setPinned(true);
                                Snackbar.make(rv, R.string.pinned_message, Snackbar.LENGTH_SHORT).show();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        return true;
                    case R.id.navigation_drawer_item3:
                        //handle copy click
                        ClipboardManager clipboard = getSystemService(Objects.requireNonNull(mContext), ClipboardManager.class);
                        ClipData clip = ClipData.newPlainText("label", messageText.getText().toString());
                        Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                        Snackbar.make(rv, R.string.copied, Snackbar.LENGTH_SHORT).show();
                        return true;
                    default:
                        return false;
                }
            });
            //displaying the popup
            popup.show();
        }
    }

    public class QuoteItemOnClickListener implements View.OnClickListener {
        QuotedUserMessage message;
        View itemView;
        TextView messageText;

        QuoteItemOnClickListener(QuotedUserMessage message,View itemView,TextView messageText){
            this.itemView = itemView;
            this.message = message;
            this.messageText = messageText;
        }

        @Override
        public void onClick(View v) {
            new Thread(()->{
                for (QuotedUserMessage msg : mMessageList) {
                    if(msg.getMessage().equals(message.getQuotedMessage())
                            && msg.getSender().equals(message.getQuoteSender())){
                        Handler h = new Handler(Looper.getMainLooper());
                        h.post(()-> {
                            mMessageRecycler.smoothScrollToPosition(mMessageList.indexOf(msg));
                            notifyItemChanged(mMessageList.indexOf(msg));
                        });
//                        try{
//                            Thread.sleep(350);
//                        }catch (Exception ignored) {}
//                        h.post(()-> notifyItemChanged(mMessageList.indexOf(msg)));
                        return;
                    }
                }
                Handler h = new Handler(Looper.getMainLooper());
                h.post(()-> Toast.makeText(app, "can't find original message", Toast.LENGTH_SHORT).show());
            }).start();
        }
    }

    public class AudioItemOnClickListener implements View.OnClickListener, CallBack {
        QuotedUserMessage message;
        View itemView;
        ImageView playPauseButton;

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

    private static class FileMessageHolder extends RecyclerView.ViewHolder{
        TextView timeText,nameText,filenameText;
        FloatingActionButton imageHolder;
        ProgressBar fileProgress;

        FileMessageHolder(View itemView){
            super(itemView);
            timeText = itemView.findViewById(R.id.text_message_time);
            nameText = itemView.findViewById(R.id.text_message_name);
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

                    Snackbar.make(itemView,R.string.saved_to_storage,Snackbar.LENGTH_SHORT).show();

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
                        uri = FileProvider.getUriForFile(app, "com.dx.anonymousmessenger.fileprovider", new File(tmp.getAbsolutePath()));
                    }else{
                        return;
                    }
                    app.grantUriPermission(app.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    //open tmp file with third party
                    Intent objIntent = new Intent(Intent.ACTION_VIEW);
                    objIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    objIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                            Snackbar.make(v, R.string.unpinned_message, Snackbar.LENGTH_SHORT).show();
                        } else {
                            DbHelper.pinMessage(message, app);
                            message.setPinned(true);
                            Snackbar.make(v, R.string.pinned_message, Snackbar.LENGTH_SHORT).show();
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
            if(nameText!=null){
                nameText.setText(message.getSender());
            }

            filenameText.setText(message.getFilename());
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
//            itemView.setOnClickListener(v -> onClick(v,message,app,permissionCallback));
            imageHolder.setOnClickListener(v -> onClick(v,message,app,permissionCallback));
            filenameText.setOnClickListener(v -> onClick(v,message,app,permissionCallback));
//            itemView.setOnLongClickListener(v -> {
//                saveFile(message,app, permissionCallback);
//                return false;
//            });
        }
    }

    private class VideoMessageHolder extends MediaMessageHolder {

        VideoMessageHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(QuotedUserMessage message) {
            if(nameText!=null){
                nameText.setText(message.getSender());
            }
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            if(message.getMessage()!=null && !message.getMessage().equals("")){
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getMessage());
                messageText.setOnClickListener(new ListItemOnClickListener(message,itemView,messageText));
            }else{
                messageText.setVisibility(View.GONE);
            }
            imageHolder.setOnClickListener(v -> {
                Intent intent = new Intent(app, PictureViewerActivity.class);
                intent.putExtra("address",message.getAddress().substring(0,10));
                intent.putExtra("nickname",message.getSender());
                intent.putExtra("time",message.getCreatedAt());
                intent.putExtra("appData",true);
                intent.putExtra("type",MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
                intent.putExtra("path",message.getPath());
                intent.putExtra("message",message.getMessage());
                app.startActivity(intent);
            });
            new Thread(()->{
                try{
                    final byte[] img_bin = FileHelper.getFile(message.getPath(), app);
                    if(img_bin == null){
                        System.out.println("no img_bin!!!");
                        return;
                    }
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(new MediaDataSource() {
                        @Override
                        public int readAt(long position, byte[] buffer, int offset, int size) {
                            ByteArrayInputStream bais = new ByteArrayInputStream(img_bin);
                            bais.skip(position-1);
                            return bais.read(buffer,offset,size);
                        }

                        @Override
                        public long getSize() {
                            return img_bin.length;
                        }

                        @Override
                        public void close() {

                        }
                    });
                    byte[] thumb = mmr.getEmbeddedPicture();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(thumb,0,thumb.length);
                    if(bitmap==null){
                        System.out.println("no bitmap!!!");
                        return;
                    }
                    new Handler(Looper.getMainLooper()).post(()->{
                        if(imageHolder==null){
                            return;
                        }
                        imageHolder.setImageBitmap(bitmap);
                    });
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private class MediaMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText,nameText,messageText;
        ImageView imageHolder;

        MediaMessageHolder(View itemView){
            super(itemView);
            timeText = itemView.findViewById(R.id.text_message_time);
            nameText = itemView.findViewById(R.id.text_message_name);
            messageText = itemView.findViewById(R.id.text_message_body);
            imageHolder = itemView.findViewById(R.id.img_holder);
        }

        void bind(QuotedUserMessage message) {
            if(nameText!=null){
                nameText.setText(message.getSender());
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
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(img_bin, 0, img_bin.length, options);
                if(bitmap==null){
                    return;
                }
                new Handler(Looper.getMainLooper()).post(()->{
                    if(imageHolder==null){
                        return;
                    }
                    imageHolder.setImageBitmap(bitmap);
                    imageHolder.setOnClickListener(v -> {
                        Intent intent = new Intent(app, PictureViewerActivity.class);
                        intent.putExtra("address",message.getAddress().substring(0,10));
                        intent.putExtra("nickname",message.getSender());
                        intent.putExtra("time",message.getCreatedAt());
                        intent.putExtra("appData",true);
                        intent.putExtra("path",message.getPath());
                        intent.putExtra("message",message.getMessage());
                        v.getContext().startActivity(intent);
                    });
                });
            }).start();
        }
    }

    private class AudioMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText,nameText;
        FloatingActionButton playPauseButton;

        AudioMessageHolder(View itemView){
            super(itemView);
            timeText = itemView.findViewById(R.id.text_message_time);
            playPauseButton = itemView.findViewById(R.id.btn_play_pause);
            nameText = itemView.findViewById(R.id.text_message_name);
        }

        void bind(QuotedUserMessage message) {
            if(nameText!=null){
                nameText.setText(message.getSender());
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

    private class MessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        MessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }

        void bind(QuotedUserMessage message) {
            //temp text
            Spannable span = new SpannableString(message.getMessage());
            //detect start-finish of emoji and span it bigger
            int charStart = -1;
            for (int i = 0; i < span.length(); i++) {
                int type = Character.getType(span.charAt(i));
                if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                    if(i==(span.length()-1)){
                        if(charStart>=0){
                            span.setSpan(new RelativeSizeSpan(3f), charStart, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            charStart = -1;
                        }else{
                            span.setSpan(new RelativeSizeSpan(3f), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            charStart = -1;
                        }
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

    private class SentMessageHolder extends MessageHolder {

        SentMessageHolder(View itemView) {
            super(itemView);
        }
    }

    private class SentOkMessageHolder extends SentMessageHolder {

        SentOkMessageHolder(View itemView) {
            super(itemView);
        }
    }

    private class ReceivedMessageHolder extends MessageHolder {
        TextView nameText;
        ImageView profileImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_message_name);
            //todo add profile image
            profileImage = itemView.findViewById(R.id.image_message_profile);
        }

        @Override
        void bind(QuotedUserMessage message) {
            super.bind(message);
            nameText.setText(message.getSender());
            // Insert the profile image from the URL into the ImageView.util
            // .displayRoundImageFromUrl(mContext, util.Utils.getInitials(message.getSender()), profileImage);
        }
    }

    private class QuoteSentMessageHolder extends SentMessageHolder{
        TextView quote;
        TextView quoteSender;
        QuoteSentMessageHolder(View itemView) {
            super(itemView);
            quote = itemView.findViewById(R.id.quote_text);
            quoteSender = itemView.findViewById(R.id.quote_sender);
        }

        @Override
        void bind(QuotedUserMessage message) {
            super.bind(message);
            quote.setText(message.getQuotedMessage());
            quoteSender.setText(message.getQuoteSender().equals(app.getAccount().getNickname())?"You":message.getQuoteSender());
            quote.setOnClickListener(new QuoteItemOnClickListener(message,itemView,messageText));
            quoteSender.setOnClickListener(new QuoteItemOnClickListener(message,itemView,messageText));
        }
    }

    private class QuoteSentOkMessageHolder extends SentOkMessageHolder{
        TextView quote;
        TextView quoteSender;
        QuoteSentOkMessageHolder(View itemView) {
            super(itemView);
            quote = itemView.findViewById(R.id.quote_text);
            quoteSender = itemView.findViewById(R.id.quote_sender);
        }

        @Override
        void bind(QuotedUserMessage message) {
            super.bind(message);
            quote.setText(message.getQuotedMessage());
            quoteSender.setText(message.getQuoteSender().equals(app.getAccount().getNickname())?"You":message.getQuoteSender());
            quote.setOnClickListener(new QuoteItemOnClickListener(message,itemView,messageText));
            quoteSender.setOnClickListener(new QuoteItemOnClickListener(message,itemView,messageText));
        }
    }

    private class QuoteReceivedMessageHolder extends ReceivedMessageHolder{
        TextView quote;
        TextView quoteSender;
        QuoteReceivedMessageHolder(View itemView) {
            super(itemView);
            quote = itemView.findViewById(R.id.quote_text);
            quoteSender = itemView.findViewById(R.id.quote_sender);
        }

        @Override
        void bind(QuotedUserMessage message) {
            super.bind(message);
            quote.setText(message.getQuotedMessage());
            quoteSender.setText(message.getQuoteSender().equals(app.getAccount().getNickname())?"You":message.getQuoteSender());
            quote.setOnClickListener(new QuoteItemOnClickListener(message,itemView,messageText));
            quoteSender.setOnClickListener(new QuoteItemOnClickListener(message,itemView,messageText));
        }
    }

}