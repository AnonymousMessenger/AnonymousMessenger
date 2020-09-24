package com.dx.anonymousmessenger.messages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.MessageListActivity;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.media.AudioPlayer;
import com.dx.anonymousmessenger.util.CallBack;
import com.dx.anonymousmessenger.util.Utils;

import java.util.List;
import java.util.Objects;

import static androidx.core.content.ContextCompat.getDrawable;
import static androidx.core.content.ContextCompat.getSystemService;

public class MessageListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_MESSAGE_SENT_OK = 3;
    private static final int VIEW_TYPE_MESSAGE_SENT_QUOTE = 4;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED_QUOTE = 5;
    private static final int VIEW_TYPE_MESSAGE_SENT_OK_QUOTE = 6;
    private static final int VIEW_TYPE_AUDIO_MESSAGE_SENT = 7;
    private static final int VIEW_TYPE_AUDIO_MESSAGE_SENT_OK = 8;
    private static final int VIEW_TYPE_AUDIO_MESSAGE_RECEIVED = 9;

    private Context mContext;
    private List<QuotedUserMessage> mMessageList;
    private DxApplication app;
    private String nowPlaying;
    public AudioPlayer ap;

    public MessageListAdapter(Context context, List<QuotedUserMessage> messageList, DxApplication app) {
        this.app = app;
        mContext = context;
        mMessageList = messageList;
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
                if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_SENT_OK;
                }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                    return VIEW_TYPE_MESSAGE_SENT_OK;
                }else if(message.getQuotedMessage()!=null){
                    return VIEW_TYPE_MESSAGE_SENT_OK_QUOTE;
                }
                return VIEW_TYPE_MESSAGE_SENT_OK;
            }else{
                if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_SENT;
                }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                    return VIEW_TYPE_MESSAGE_SENT;
                }else if(message.getQuotedMessage()!=null){
                    return VIEW_TYPE_MESSAGE_SENT_QUOTE;
                }
                return VIEW_TYPE_MESSAGE_SENT;
            }
        } else {
            // If some other user sent the message
            if(message.getType()!=null && message.getType().equals("audio")){
                    return VIEW_TYPE_AUDIO_MESSAGE_RECEIVED;
            }else if(message.getQuotedMessage()!= null && message.getQuotedMessage().equals("")){
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }else if(message.getQuotedMessage()!=null){
                return VIEW_TYPE_MESSAGE_RECEIVED_QUOTE;
            }
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
        }
        Log.e("finding message type","something went wrong");
        return null;
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
            PopupMenu popup = new PopupMenu(mContext, messageText);
            popup.inflate(R.menu.options_menu);
            if(message.isPinned()){
                MenuItem pinButton = popup.getMenu().findItem(R.id.navigation_drawer_item2);
                pinButton.setTitle("Unpin");
            }
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_drawer_item1:
                        //handle reply click
                        TextView quoteTextTyping = ((MessageListActivity) mContext).findViewById(R.id.quote_text_typing);
                        TextView quoteSenderTyping = ((MessageListActivity) mContext).findViewById(R.id.quote_sender_typing);
                        quoteTextTyping.setText(message.getMessage());
                        quoteSenderTyping.setText(message.getTo().equals(app.getHostname())?message.getSender():"You");
                        quoteTextTyping.setVisibility(View.VISIBLE);
                        quoteSenderTyping.setVisibility(View.VISIBLE);
                        RecyclerView rv = ((MessageListActivity) mContext).findViewById(R.id.reyclerview_message_list);
                        rv.scrollToPosition(mMessageList.size() - 1);
                        return true;
                    case R.id.navigation_drawer_item2:
                        //handle pin click
                        try{
                            if(message.isPinned()){
                                MessageSender.unPinMessage(message,app);
                                message.setPinned(false);
                            }else{
                                MessageSender.pinMessage(message,app);
                                message.setPinned(true);
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
                        return true;
                    default:
                        return false;
                }
            });
            //displaying the popup
            popup.show();
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
                    playPauseButton.setImageDrawable(getDrawable(mContext,R.drawable.ic_baseline_play_arrow_24));
                    notifyItemChanged(mMessageList.indexOf(message));
                    nowPlaying = null;
                    //stop playing
                    if(ap!=null){
                        ap = null;
                    }
                };
                mainHandler.post(myRunnable);
            }catch (Exception ignored) {}
        }

    }

    private class AudioMessageHolder extends RecyclerView.ViewHolder {
        TextView timeText,nameText;
        ImageView playPauseButton;

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
            messageText.setText(message.getMessage());
            timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            messageText.setOnClickListener(new ListItemOnClickListener(message,itemView,messageText));
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
        }
    }

}