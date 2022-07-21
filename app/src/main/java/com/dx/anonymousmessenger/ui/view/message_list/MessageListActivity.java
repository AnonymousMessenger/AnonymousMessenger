package com.dx.anonymousmessenger.ui.view.message_list;

import static java.util.Objects.requireNonNull;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.call.DxCallService;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.media.AudioRecordingService;
import com.dx.anonymousmessenger.media.MediaRecycleViewAdapter;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.tor.TorClient;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.app.ContactListAdapter;
import com.dx.anonymousmessenger.ui.view.call.CallActivity;
import com.dx.anonymousmessenger.ui.view.notepad.NotepadActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.ContactProfileActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.FileViewerActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.PictureViewerActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.VerifyIdentityActivity;
import com.dx.anonymousmessenger.util.CallBack;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MessageListActivity extends DxActivity implements ActivityCompat.OnRequestPermissionsResultCallback, ComponentCallbacks2, ContactListAdapter.ItemClickListener, CallBack {

    private static final int READ_STORAGE_REQUEST_CODE = 1;
//    public static final int REQUEST_PICK_FILE = 2;
    private static final int RECORD_AUDIO_REQUEST_CODE = 3;
    public TextView quoteTextTyping;
    public TextView quoteSenderTyping;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private List<QuotedUserMessage> messageList = new ArrayList<>();
    private BroadcastReceiver mMyBroadcastReceiver;
    private Thread messageChecker = null;
    private Button send = null;
    private EditText txt = null;
    private TextView txtAudioTimer = null;
    private FloatingActionButton audio, file;
    private LinearLayout audioLayout;
    private FloatingActionButton scrollDownFab;
    private BroadcastReceiver timeBroadcastReceiver;
    private FrameLayout frameOnline;
    private RecyclerView mediaRecyclerView;
    private LinearLayout picsHelp;
    private ConstraintLayout chatbox;
    private String address;
    private String nickname;
    private AtomicReference<Float> x = null;
    private boolean started = false;
    private boolean online = false;
    private boolean pinging = false;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(new
                    ActivityResultContracts.GetContent(),
            uri -> new Thread(() -> {
                try{
                    if(uri == null){
                        return;
                    }
                    Intent intent = new Intent(this, FileViewerActivity.class);
                    intent.putExtra("uri",uri);
                    intent.putExtra("filename", FileHelper.getFileName(uri,this));
                    intent.putExtra("size",FileHelper.getFileSize(uri,this));
                    intent.putExtra("address", Objects.requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
                    startActivity(intent);
                }catch (Exception e){
//                        e.printStackTrace();
                }
            }).start());

    @SuppressLint({"ClickableViewAccessibility", "ShowToast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setExitTransition(getWindow().getSharedElementExitTransition());
        setContentView(R.layout.activity_message_list);
        try{
            setTitle(getIntent().getStringExtra("nickname"));
            setSubtitle(getIntent().getStringExtra("address"));
            setBackEnabled(true);
            findViewById(R.id.toolbar).setTransitionName("title");
        }catch (Exception ignored){}


        nickname = getIntent().getStringExtra("nickname");
        quoteTextTyping = findViewById(R.id.quote_text_typing);
        quoteSenderTyping = findViewById(R.id.quote_sender_typing);
        quoteTextTyping.setVisibility(View.GONE);
        quoteTextTyping.setText("");
        quoteSenderTyping.setVisibility(View.GONE);
        quoteSenderTyping.setText("");
        send = findViewById(R.id.button_chatbox_send);
        txt = findViewById(R.id.edittext_chatbox);
        audio = findViewById(R.id.fab_audio);
        file = findViewById(R.id.fab_file);
        mediaRecyclerView = findViewById(R.id.rv_media);
        picsHelp = findViewById(R.id.layout_pics);
        audioLayout = findViewById(R.id.layout_audio);
        txtAudioTimer = findViewById(R.id.txt_audio_timer);
        frameOnline = findViewById(R.id.frame_online);
        scrollDownFab = findViewById(R.id.fab_scroll_down);
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
//        mMessageRecycler.setHasFixedSize(true);
        mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication(), mMessageRecycler, this);

        mMessageRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if(messageList==null){
                    scrollDownFab.setVisibility(View.GONE);
                    return;
                }
                if (dy > 0
                        && recyclerView.getLayoutManager() !=null
                        && ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition()!=(messageList.size()-1)) { // scrolling down
                    if(scrollDownFab.getVisibility()==View.VISIBLE){
                        return;
                    }
                    scrollDownFab.clearAnimation();
                    final Animation animation = new AlphaAnimation(0f, 1f);
                    animation.setInterpolator(new FastOutSlowInInterpolator());
                    animation.setDuration(150);
                    animation.reset();
                    animation.setStartTime(0);
                    scrollDownFab.setVisibility(View.VISIBLE);
                    scrollDownFab.startAnimation(animation);
                } else if (dy < 0
                        && recyclerView.getLayoutManager() !=null
                        && ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition()!=(messageList.size()-1)) { // scrolling up
                    if(scrollDownFab.getVisibility()==View.VISIBLE){
                        return;
                    }
                    scrollDownFab.clearAnimation();
                    final Animation animation = new AlphaAnimation(0f, 1f);
                    animation.setInterpolator(new FastOutSlowInInterpolator());
                    animation.setDuration(150);
                    animation.reset();
                    animation.setStartTime(0);
                    scrollDownFab.setVisibility(View.VISIBLE);
                    scrollDownFab.startAnimation(animation);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if(messageList==null){
                    scrollDownFab.setVisibility(View.GONE);
                    return;
                }
                if(scrollDownFab.getVisibility()==View.GONE){
                    return;
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && recyclerView.getLayoutManager() !=null
                        && ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition()==(messageList.size()-1)) { // No scrolling
                    scrollDownFab.clearAnimation();
                    final Animation animation = new AlphaAnimation(1f, 0f);
                    animation.setInterpolator(new FastOutSlowInInterpolator());
                    animation.setDuration(150);
                    animation.reset();
                    animation.setStartTime(0);
                    scrollDownFab.setVisibility(View.GONE);
                    scrollDownFab.startAnimation(animation);
                }
            }
        });
        scrollDownFab.setOnClickListener(v -> {
            try{
                mMessageRecycler.smoothScrollToPosition(messageList.size()-1);
                scrollDownFab.setVisibility(View.GONE);
            }catch (Exception ignored){
                scrollDownFab.setVisibility(View.GONE);
            }
        });
        chatbox = findViewById(R.id.layout_chatbox);

        ((MaterialToolbar)findViewById(R.id.toolbar)).inflateMenu(R.menu.message_list_menu);
        if(!((DxApplication)getApplication()).isAcceptingCallsAllowed()){
            ((MaterialToolbar)findViewById(R.id.toolbar)).getMenu().removeItem(R.id.action_call);
        }
//        chatbox.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
//            mMessageRecycler.scrollBy(0,-10);
//            mMessageRecycler.scrollBy(0,20);
//        });


/*
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorLayout);

        LinearLayout contentLayout = coordinatorLayout.findViewById(R.id.contentLayout);

//        BottomSheetBehavior sheetBehavior = BottomSheetBehavior.from(contentLayout);
//        BottomSheetBehavior.from(contentLayout).setDraggable(true);
        BottomSheetBehavior.from(contentLayout).setFitToContents(false);
        BottomSheetBehavior.from(contentLayout).setHideable(true);//prevents the bottom sheet from completely hiding off the screen
        BottomSheetBehavior.from(contentLayout).setState(BottomSheetBehavior.STATE_HIDDEN);

//        BottomSheetBehavior.from(contentLayout).setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
*/

        //run db message checker to delete any old messages then tell us to update ui
//        checkMessages();
    }

    private void openGallery() {
//        View v = this.;
        InputMethodManager imm = requireNonNull(
                ContextCompat.getSystemService(this, InputMethodManager.class));
        imm.hideSoftInputFromWindow(chatbox.getWindowToken(), 0);

        chatbox = findViewById(R.id.layout_chatbox);
//            cl.setVisibility(View.GONE);

        send.setVisibility(View.GONE);
        audio.setVisibility(View.GONE);
        file.setVisibility(View.GONE);
        txt.setVisibility(View.GONE);
        quoteTextTyping.setVisibility(View.GONE);
        quoteSenderTyping.setVisibility(View.GONE);

        Animation bottomUp = AnimationUtils.loadAnimation(this,
                R.anim.bottom_up);
        chatbox.startAnimation(bottomUp);
        chatbox.setVisibility(View.VISIBLE);

//            mediaRecyclerView.startAnimation(bottomUp);
        mediaRecyclerView.setVisibility(View.VISIBLE);

//            mediaRecyclerView.setVisibility(View.VISIBLE);
        picsHelp.setVisibility(View.VISIBLE);

        new Thread(()->{
            ArrayList<String> paths = new ArrayList<>();
            ArrayList<String> types = new ArrayList<>();
            //add file option in the beginning of the list
            paths.add("0");
            types.add("0");
            // Get relevant columns for use later.
            String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE
            };

            // Return only video and image metadata.
            String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                             + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
//                                 + " OR "
//                                 + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
//                                 + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
            Uri queryUri = MediaStore.Files.getContentUri("external");
            Cursor cursor = this.getContentResolver().query(queryUri, projection, selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

            try {
                if (cursor != null) {
                    cursor.moveToFirst();
                    do{
                        try{
                            String type = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                            paths.add(path);
                            types.add(type);
                        }catch (Exception ignored){}
                    }while(cursor.moveToNext());
                    cursor.close();
                }
                // set up the RecyclerView
                new Handler(Looper.getMainLooper()).post(()->{
                    LinearLayoutManager horizontalLayoutManager
                            = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                    mediaRecyclerView.setLayoutManager(horizontalLayoutManager);
                    MediaRecycleViewAdapter adapter = new MediaRecycleViewAdapter(this, paths, types);
                    adapter.setClickListener(this::onItemClick);
                    mediaRecyclerView.setAdapter(adapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    //fires when clicking on media/file sending items
    @Override
    public void onItemClick(View view, int position) {
        if(position == 0){
            mGetContent.launch("*/*");
            return;
        }
        Intent intent = new Intent(this, PictureViewerActivity.class);
        intent.putExtra("address",address.substring(0,10));
        intent.putExtra("nickname",nickname);
        if(mediaRecyclerView!=null && mediaRecyclerView.getAdapter()!=null){
            String path = ((MediaRecycleViewAdapter)mediaRecyclerView.getAdapter()).paths.get(position);
            String type = ((MediaRecycleViewAdapter)mediaRecyclerView.getAdapter()).types.get(position);
            intent.putExtra("path",path);
            intent.putExtra("type",type);
            startActivity(intent);
        }
    }

    public void ping(){
        if(pinging){
            return;
        }
        new Thread(()->{
            pinging = true;
//            try {
//                Thread.sleep(500);
//            } catch (Exception ignored) {}
            Handler h = new Handler(Looper.getMainLooper());
            h.post(()->{
                try{
                    ColorDrawable[] color = {new ColorDrawable(ContextCompat.getColor(this, R.color.startGradientColor)), new ColorDrawable(ContextCompat.getColor(this, R.color.endGradientColor))};
                    TransitionDrawable trans = new TransitionDrawable(color);
                    frameOnline.setBackground(trans);
                    trans.startTransition(1000);
                    frameOnline.setVisibility(View.VISIBLE);
                    Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                    frameOnline.startAnimation(slideUp);
                }catch (Exception ignored) {}
            });
            boolean b = TorClient.testAddress(((DxApplication) getApplication()), address);
            online = b;
            h.post(()->{
                try{
                    if(b){
                        ColorDrawable[] color = {new ColorDrawable(ContextCompat.getColor(this, R.color.endGradientColor)), new ColorDrawable(ContextCompat.getColor(this, R.color.green_tor))};
                        TransitionDrawable trans = new TransitionDrawable(color);
                        frameOnline.setBackground(trans);
                        trans.startTransition(1000);
                    }else{
                        ColorDrawable[] color = {new ColorDrawable(ContextCompat.getColor(this, R.color.endGradientColor)), new ColorDrawable(ContextCompat.getColor(this, R.color.red_500))};
                        TransitionDrawable trans = new TransitionDrawable(color);
                        frameOnline.setBackground(trans);
                        trans.startTransition(1000);
                    }
//                    Animation shock = AnimationUtils.loadAnimation(this, R.anim.rotate_line);
//                    frameOnline.startAnimation(shock);
                }catch (Exception ignored) {}
            });
            if(b){
                ((DxApplication)getApplication()).addToOnlineList(address);
                ((DxApplication)getApplication()).queueUnsentMessages(address);
            }else{
                ((DxApplication)getApplication()).onlineList.remove(address);
            }
//            try {
//                Thread.sleep(6000);
//            } catch (Exception ignored) {}
//            h.post(()->{
//                try {
//                    Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
//                    frameOnline.startAnimation(slideDown);
//                    frameOnline.setVisibility(View.GONE);
//                }catch (Exception ignored) {}
//            });
            pinging = false;
        }).start();
    }

    public void checkMessages(){
        if(messageChecker!=null){
            return;
        }
        messageChecker = new Thread(()->{
            while (messageList!=null){
                try{
                    //noinspection BusyWait
                    Thread.sleep(5000);
                    DbHelper.deleteOldMessages((DxApplication) getApplication(),address);
                }catch (Exception ignored){break;}
            }
        });
        messageChecker.start();
    }

    public void stopCheckingMessages(){
        if(messageChecker==null){
            return;
        }
        if(messageChecker.isAlive()){
            messageChecker.interrupt();
            messageChecker = null;
        }
    }

    public void updateUi(boolean scrollDown, boolean animate){
        try{
            new Thread(()->{
                DxApplication app = (DxApplication) getApplication();
                if(app.getEntity()==null){
                    new Handler(Looper.getMainLooper()).post(()->{
                        TextView keyStatus = findViewById(R.id.txt_key_status);
                        keyStatus.setVisibility(View.VISIBLE);
                        keyStatus.setText(R.string.waiting_for_connectivity);
                        ConstraintLayout chatBox = findViewById(R.id.layout_chatbox);
                        chatBox.setVisibility(View.GONE);
                    });
                }else if(app.getEntity().getStore()==null){
                    new Handler(Looper.getMainLooper()).post(()->{
                        TextView keyStatus = findViewById(R.id.txt_key_status);
                        keyStatus.setVisibility(View.VISIBLE);
                        keyStatus.setText(R.string.waiting_for_tor);
                        ConstraintLayout chatBox = findViewById(R.id.layout_chatbox);
                        chatBox.setVisibility(View.GONE);
                    });
                }else if(
                        !app.getEntity().getStore().containsSession(new SignalProtocolAddress(address,
                                1)) ||
                        app.getEntity().getStore().loadSession(new SignalProtocolAddress(address,1)).getSessionState().hasPendingKeyExchange() ||
                        app.getEntity().getStore().getIdentity(new SignalProtocolAddress(address,1)) == null
                ){
                    if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(address,
                            1))){
                        new Thread(()-> MessageSender.sendKeyExchangeMessageWithoutBroadcast(app,
                                address)).start();
                        new Handler(Looper.getMainLooper()).post(()->{
                            TextView keyStatus = findViewById(R.id.txt_key_status);
                            keyStatus.setVisibility(View.VISIBLE);
                            keyStatus.setText(R.string.no_session);
                            Button retry = findViewById(R.id.btn_retry);
                            retry.setOnClickListener((v) -> {
                                //reset session
                                resetSession();
                            });
                            retry.setVisibility(View.VISIBLE);
                            ConstraintLayout chatBox = findViewById(R.id.layout_chatbox);
                            chatBox.setVisibility(View.GONE);
                        });
                    }
                    if(app.getEntity().getStore().loadSession(new SignalProtocolAddress(address,1)).getSessionState().hasPendingKeyExchange()){
                        new Handler(Looper.getMainLooper()).post(()->{
                            TextView keyStatus = findViewById(R.id.txt_key_status);
                            keyStatus.setVisibility(View.VISIBLE);
                            keyStatus.setText(R.string.waiting_for_response);
                            Button retry = findViewById(R.id.btn_retry);
                            retry.setOnClickListener((v) -> {
                                //reset session
                                resetSession();
                            });
                            retry.setVisibility(View.VISIBLE);
                            ConstraintLayout chatBox = findViewById(R.id.layout_chatbox);
                            chatBox.setVisibility(View.GONE);
                        });
                    }
                }else{
                    new Handler(Looper.getMainLooper()).post(()->{
                        TextView keyStatus = findViewById(R.id.txt_key_status);
                        keyStatus.setVisibility(View.GONE);
                        Button retry = findViewById(R.id.btn_retry);
                        retry.setVisibility(View.GONE);
                        ConstraintLayout chatBox = findViewById(R.id.layout_chatbox);
                        chatBox.setVisibility(View.VISIBLE);
                    });
                }
                messageList = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                        address);
                String nickname = DbHelper.getContactNickname(address,app);
                runOnUiThread(()->{
                    try{
                        mMessageAdapter.setMessageList(messageList);
                        mMessageAdapter.notifyDataSetChanged();
                        setTitle(nickname);
//                        mMessageRecycler.scheduleLayoutAnimation();
                        if(!messageList.isEmpty()){
                            findViewById(R.id.no_messages).setVisibility(View.GONE);
                            if(scrollDown){
                                mMessageRecycler.scrollToPosition(messageList.size() - 1);
                                scrollDownFab.setVisibility(View.GONE);
                                ((DxApplication)getApplication()).clearMessageNotification();
                                if(!messageList.get(messageList.size()-1).getAddress().equals(((DxApplication)getApplication()).getHostname())){
                                    String newName = messageList.get(messageList.size()-1).getSender();
                                    setTitle(newName);
                                    getIntent().putExtra("nickname",newName);
                                }
                                if(animate){
                                    mMessageRecycler.scheduleLayoutAnimation();
                                }
                            }
                        }else{
                            findViewById(R.id.no_messages).setVisibility(View.VISIBLE);
                        }
                    }catch (Exception ignored) {}
                });
                new Thread(() -> {
                    try{
                        byte[] image = FileHelper.getFile(DbHelper.getContactProfileImagePath(address,(DxApplication)getApplication()), (DxApplication)getApplication());
                        if (image == null) {
                            return;
                        }
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeByteArray(image, 0, image.length));
                        drawable.setCircular(true);
                        new Handler(Looper.getMainLooper()).post(()-> ((MaterialToolbar) findViewById(R.id.toolbar)).getMenu().getItem(0).setIcon(drawable));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }).start();
            }).start();
        }catch (Exception ignored) {}
    }

    public void updateUi(List<QuotedUserMessage> tmp){
        if(quoteTextTyping==null && mMyBroadcastReceiver==null){
            return;
        }
        messageList = tmp;
        runOnUiThread(()->{
            try{
                mMessageAdapter.setMessageList(messageList);
                mMessageAdapter.notifyDataSetChanged();
                mMessageRecycler.scheduleLayoutAnimation();
                mMessageRecycler.smoothScrollToPosition(messageList.size() - 1);
            }catch (Exception ignored){}
        });
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        updateUi();
//    }

    @SuppressLint({"ClickableViewAccessibility", "ShowToast"})
    @Override
    protected void onStart() {
        super.onStart();
        if(mMyBroadcastReceiver!=null){
            return;
        }
        /* init stuff */

        final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                "address"),
                (DxApplication) getApplication());
        if(fullAddress == null){
            return;
        }
        address = fullAddress;

        new Thread(() -> {
            try{
                byte[] image = FileHelper.getFile(DbHelper.getContactProfileImagePath(address,(DxApplication)getApplication()), (DxApplication)getApplication());
                if (image == null) {
                    return;
                }
                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeByteArray(image, 0, image.length));
                drawable.setCircular(true);
                new Handler(Looper.getMainLooper()).post(()-> ((MaterialToolbar) findViewById(R.id.toolbar)).getMenu().getItem(0).setIcon(drawable));
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();
        ((MaterialToolbar)findViewById(R.id.toolbar)).setOnMenuItemClickListener(this::onOptionsItemSelected);

        send.setOnClickListener(v -> {
            if(((DxApplication) getApplication()).getEntity()==null){
                Snackbar.make(send, R.string.no_encryption_yet,Snackbar.LENGTH_SHORT).setAnchorView(chatbox).show();
                return;
            }
            if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(fullAddress,1))){
                Snackbar.make(send, R.string.no_session,Snackbar.LENGTH_SHORT).setAnchorView(chatbox).show();
                new Thread(()-> MessageSender.sendKeyExchangeMessage(((DxApplication)getApplication()),fullAddress)).start();
                return;
            }
            if(((DxApplication)getApplication()).getEntity().getStore().loadSession(new SignalProtocolAddress(fullAddress,1)).getSessionState().hasPendingKeyExchange() || ((DxApplication)getApplication()).getEntity().getStore().getIdentity(new SignalProtocolAddress(fullAddress,1)) == null){
                Snackbar.make(send, R.string.cant_encrypt_message,Snackbar.LENGTH_SHORT).setAnchorView(chatbox).show();
                return;
            }
            TextView quoteSenderTyping = findViewById(R.id.quote_sender_typing);
            TextView quoteTextTyping = findViewById(R.id.quote_text_typing);
            QuotedUserMessage msg =
                    new QuotedUserMessage(quoteSenderTyping.getText().toString().equals(getString(R.string.you))?
                            ((DxApplication)getApplication()).getAccount().getNickname():
                            getIntent().getStringExtra("nickname"),
                            quoteTextTyping.getText().toString(),
                            ((DxApplication)getApplication()).getHostname(),
                            txt.getText().toString(),
                            ((DxApplication)getApplication()).getAccount().getNickname(),
                            new Date().getTime(),false,fullAddress,false);
//            messageList.add(msg);
            new Thread(()-> MessageSender.sendMessage(msg,((DxApplication)getApplication()),fullAddress)).start();
            txt.setText("");
            quoteSenderTyping.setText("");
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteTextTyping.setVisibility(View.GONE);
            audio.setVisibility(View.VISIBLE);
            file.setVisibility(View.VISIBLE);
//            try{
//                mMessageAdapter.notifyItemInserted(messageList.size()-1);
//                mMessageRecycler.smoothScrollToPosition(messageList.size() - 1);
//            }catch (Exception ignored) {}
        });
        quoteTextTyping.setOnClickListener(v -> {
            quoteTextTyping.setVisibility(View.GONE);
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteSenderTyping.setText("");
        });
        quoteSenderTyping.setOnClickListener(v -> {
            quoteTextTyping.setVisibility(View.GONE);
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteSenderTyping.setText("");
        });
        txt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()==0){
                    audio.setVisibility(View.VISIBLE);
                    file.setVisibility(View.VISIBLE);
                }else{
                    audio.setVisibility(View.GONE);
                    file.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        AtomicReference<Float> downRawX = new AtomicReference<>((float) 0);
        AtomicReference<Float> dX = new AtomicReference<>((float) 0);
        audio.setOnTouchListener((arg0, arg1) -> {
            //todo : add wakelock here
            // while user has his/her thumb on the button
            if (arg1.getAction()== MotionEvent.ACTION_DOWN){
                downRawX.set(arg1.getRawX());
                if(x==null){
                    x = new AtomicReference<>();
                    x.set(arg0.getX());
                }

                dX.set(arg0.getX() - downRawX.get());
                txt.setVisibility(View.GONE);
                quoteTextTyping.setVisibility(View.GONE);
                quoteSenderTyping.setVisibility(View.GONE);
                file.setVisibility(View.GONE);
                audioLayout.setVisibility(View.VISIBLE);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    new Thread(()->{
                        try{
                            if(mMessageAdapter!=null && mMessageAdapter.ap!=null){
                                mMessageAdapter.ap.stop();
                                mMessageAdapter.ap = null;
                            }
                        }catch (Exception ignored) {}
                    }).start();

                    Intent intent = new Intent(this, AudioRecordingService.class);
                    intent.setAction("start_recording");
                    intent.putExtra("address", requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
                    intent.putExtra("nickname",getIntent().getStringExtra("nickname"));
                    startService(intent);
                    timeBroadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            try{
                                txtAudioTimer.setText(intent.getStringExtra("time"));
                            }catch (Exception ignored) {}
                        }
                    };
                    try {
                        LocalBroadcastManager.getInstance(this).registerReceiver(timeBroadcastReceiver,new IntentFilter("recording_action"));
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO },RECORD_AUDIO_REQUEST_CODE);
                    }
                }
            }
            // user has lifted his/her thumb off the button
            else if(arg1.getAction()== MotionEvent.ACTION_UP){
                audioLayout.setVisibility(View.GONE);
                txt.setVisibility(View.VISIBLE);
                file.setVisibility(View.VISIBLE);
                if(quoteTextTyping.getText().length()>0){
                    quoteTextTyping.setVisibility(View.VISIBLE);
                    quoteSenderTyping.setVisibility(View.VISIBLE);
                }
                Intent intent = new Intent(this, AudioRecordingService.class);
                stopService(intent);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(timeBroadcastReceiver);
                timeBroadcastReceiver = null;
                txtAudioTimer.setText(getString(R.string._00_00));
                arg0.animate()
                        .x(x.get())
                        .y(arg0.getY())
                        .setDuration(0)
                        .start();
                return true;
            }else if(arg1.getAction()== MotionEvent.ACTION_MOVE){
                int viewWidth = arg0.getWidth();

                View viewParent = (View)arg0.getParent();
                int parentWidth = viewParent.getWidth();

                float newX = arg1.getRawX() + dX.get();
                newX = Math.min(parentWidth - viewWidth , newX);

                Configuration config = getResources().getConfiguration();

                if(newX>=x.get()){
                    if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                        //in Right To Left layout
                        if(newX>=(x.get()+100)){
                            audioLayout.setVisibility(View.GONE);
                            txt.setVisibility(View.VISIBLE);
                            file.setVisibility(View.VISIBLE);
                            if(quoteTextTyping.getText().length()>0){
                                quoteTextTyping.setVisibility(View.VISIBLE);
                                quoteSenderTyping.setVisibility(View.VISIBLE);
                            }
                            Intent intent = new Intent(this, AudioRecordingService.class);
                            intent.setAction("stop_recording");
                            startService(intent);
                            LocalBroadcastManager.getInstance(this).unregisterReceiver(timeBroadcastReceiver);
                            timeBroadcastReceiver = null;
                            txtAudioTimer.setText(getString(R.string._00_00));
                            arg0.animate()
                                    .x(x.get())
                                    .y(arg0.getY())
                                    .setDuration(0)
                                    .start();
                        }else{
                            arg0.animate()
                                    .x(newX)
                                    .y(arg0.getY())
                                    .setDuration(0)
                                    .start();
                        }
                    }
                    return true;
                }
                if(newX<=(x.get())){
                    if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                        return true;
                    }
                }
                if(newX<=(x.get()-100)){
                    audioLayout.setVisibility(View.GONE);
                    txt.setVisibility(View.VISIBLE);
                    file.setVisibility(View.VISIBLE);
                    if(quoteTextTyping.getText().length()>0){
                        quoteTextTyping.setVisibility(View.VISIBLE);
                        quoteSenderTyping.setVisibility(View.VISIBLE);
                    }
                    Intent intent = new Intent(this, AudioRecordingService.class);
                    intent.setAction("stop_recording");
                    startService(intent);
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(timeBroadcastReceiver);
                    timeBroadcastReceiver = null;
                    txtAudioTimer.setText(getString(R.string._00_00));
                    arg0.animate()
                            .x(x.get())
                            .y(arg0.getY())
                            .setDuration(0)
                            .start();
                }else{
                    arg0.animate()
                            .x(newX)
                            .y(arg0.getY())
                            .setDuration(0)
                            .start();
                }
                return true;
            }
            return true;
        });
        picsHelp.setOnClickListener(v -> {
            mediaRecyclerView.setVisibility(View.GONE);
            send.setVisibility(View.VISIBLE);
            audio.setVisibility(View.VISIBLE);
            file.setVisibility(View.VISIBLE);
            txt.setVisibility(View.VISIBLE);
            if(quoteTextTyping.getText().length()>0){
                quoteTextTyping.setVisibility(View.VISIBLE);
                quoteSenderTyping.setVisibility(View.VISIBLE);
            }
            picsHelp.setVisibility(View.GONE);
        });
        file.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_CODE);
                getReadStoragePerms();
                return;
            }

            openGallery();
        });
        findViewById(R.id.btn_return_to_call).setOnClickListener(v->{
            if (((DxApplication) getApplication()).isInCall()){
                //goto call
                Intent contentIntent = new Intent(this, CallActivity.class);
                if(((DxApplication) getApplication()).getCc()!=null){
                    contentIntent.putExtra("address",((DxApplication) getApplication()).getCc().getAddress().substring(0,10));
                    if(!((DxApplication) getApplication()).getCc().isAnswered()){
                        contentIntent.setAction(DxCallService.ACTION_START_INCOMING_CALL);
                    }else{
                        contentIntent.setAction("");
                    }
                }
//                contentIntent.setAction(type);
                contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(contentIntent);
            }else{
                findViewById(R.id.frame_return_to_call).setVisibility(View.GONE);
            }
        });
        if(mMessageRecycler!=null){
            mMessageRecycler.suppressLayout(false);
        }
        /* end init stuff */
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @SuppressLint("ShowToast")
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if(intent.getLongExtra("delete",-1) > -1){
                    for (QuotedUserMessage quotedUserMessage : messageList) {
                        if(quotedUserMessage.getCreatedAt()==intent.getLongExtra("delete",-1)){
                            try{
                                int pos = messageList.indexOf(quotedUserMessage);
                                mMessageAdapter.removeData(pos);
                            }catch (Exception ignored) {}
                            return;
                        }
                    }
                    return;
                }else if(intent.getStringExtra("error")!=null){
                    try{
                        Snackbar.make(send, requireNonNull(intent.getStringExtra("error")),Snackbar.LENGTH_SHORT).setAnchorView(chatbox).show();
                    }catch (Exception ignored) {}
                    return;
                }else if(intent.getStringExtra("type")!=null && Objects.equals(intent.getStringExtra("type"), "online_status")){
                    return;
                }else if(intent.getLongExtra("delivery",-1) != -1){
                    for (QuotedUserMessage quotedUserMessage : messageList) {
                        if(quotedUserMessage.getCreatedAt()==intent.getLongExtra("delivery",-1)){
                            quotedUserMessage.setReceived(true);
                            mMessageAdapter.notifyItemChanged(mMessageAdapter.getMessageList().indexOf(quotedUserMessage));
                            return;
                        }
                    }
                    return;
                }else if(intent.getStringExtra("address")!=null && Objects.equals(intent.getStringExtra("address"), getIntent().getStringExtra("address"))){
                    updateUi(true,false);
                    return;
                }else if(intent.getStringExtra("address")!=null){
                    return;
                }

                updateUi(true,true);
            }
        };
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMyBroadcastReceiver,new IntentFilter("your_action"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if(started){
            updateUi(false,false);
            if(!online && !pinging){
                ping();
            }
        }else{
            updateUi(true,false);
            started = true;
            ping();
        }
        if(((DxApplication) getApplication()).isInCall()){
            findViewById(R.id.frame_return_to_call).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.frame_return_to_call).setVisibility(View.GONE);
        }

        checkMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mMessageRecycler!=null){
            mMessageRecycler.suppressLayout(true);
        }
        stopCheckingMessages();
        if(mMyBroadcastReceiver==null){
            return;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
    }

    @Override
    public boolean onSupportNavigateUp(){
        if(mMessageRecycler!=null){
            mMessageRecycler.suppressLayout(true);
        }
        stopCheckingMessages();
        if(mMessageAdapter!=null && mMessageAdapter.ap!=null){
            mMessageAdapter.ap.stop();
            mMessageAdapter.ap = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
        quoteTextTyping = null;
        quoteSenderTyping = null;
        mMessageRecycler = null;
        mMessageAdapter = null;
        messageList = null;
//        mainThread = null;
        mMyBroadcastReceiver = null;
        messageChecker = null;
        send = null;
        txt = null;
        finishAfterTransition();
        return true;
    }

    @Override
    public void onBackPressed() {
        if(mediaRecyclerView!=null && mediaRecyclerView.getVisibility()==View.VISIBLE){
            mediaRecyclerView.setVisibility(View.GONE);
            send.setVisibility(View.VISIBLE);
            audio.setVisibility(View.VISIBLE);
            file.setVisibility(View.VISIBLE);
            txt.setVisibility(View.VISIBLE);
            if(quoteTextTyping.getText().length()>0){
                quoteTextTyping.setVisibility(View.VISIBLE);
                quoteSenderTyping.setVisibility(View.VISIBLE);
            }
            picsHelp.setVisibility(View.GONE);
            return;
        }
        //super.onBackPressed();
        onSupportNavigateUp();
    }

//    @Override
//    protected void onDestroy() {
//        onSupportNavigateUp();
//        super.onDestroy();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.message_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_notepad){
            stopCheckingMessages();
            Intent intent = new Intent(this, NotepadActivity.class);
            intent.putExtra("address", requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
            startActivity(intent);
        } else if(item.getItemId() == R.id.action_contact_profile){
            stopCheckingMessages();
            new Handler().postDelayed(()->{
                Intent intent2 = new Intent(this, ContactProfileActivity.class);
                intent2.putExtra("address", requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
                View v = findViewById(R.id.action_contact_profile);
                ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(this, v, "profile_picture");
                this.startActivity(intent2,activityOptions.toBundle());
            },150);
//                startActivity(intent2);
        } else if(item.getItemId() == R.id.action_call){
            new Thread(()->{
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    if(((DxApplication)getApplication()).isInCall()){
                        runOnUiThread(()-> Snackbar.make(send,"Already in a call",Snackbar.LENGTH_SHORT).show());
                        return;
                    }
                    //create a call message to save the call attempt in the log
                    final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                                    "address"),
                            (DxApplication) getApplication());
                    if(fullAddress == null){
                        return;
                    }
                    address = fullAddress;

                    DbHelper.saveMessage(new QuotedUserMessage("","", ((DxApplication) getApplication()).getHostname(), "", ((DxApplication)getApplication()).getAccount().getNickname(), new Date().getTime(), true, address, false, "", "", "call"), (DxApplication) this.getApplication(),address,true);
                    //broadcast to message log
                    Intent gcm_rec = new Intent("your_action");
                    gcm_rec.putExtra("address",address.substring(0,10));
                    LocalBroadcastManager.getInstance(((DxApplication)getApplication()).getApplicationContext()).sendBroadcast(gcm_rec);

                    //start activity with start_out_call action
                    Intent callIntent = new Intent(this, CallActivity.class);
                    callIntent.setAction("start_out_call");
                    callIntent.putExtra("address", requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
                    callIntent.putExtra("nickname",getIntent().getStringExtra("nickname"));
                    startActivity(callIntent);
                }else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO },RECORD_AUDIO_REQUEST_CODE);
                    }
                }
            }).start();
        } else if(item.getItemId() == R.id.action_reset_session){
            //reset session
            resetSession();
            //((DxSignalKeyStore)app.getEntity().getStore()).removeIdentity(new
            // SignalProtocolAddress(address,1));
        } else if(item.getItemId() == R.id.action_verify_identity){
            stopCheckingMessages();
            Intent intent = new Intent(this, VerifyIdentityActivity.class);
            intent.putExtra("address", requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
            startActivity(intent);
        } else if(item.getItemId() == R.id.action_clear_conversation){
            new AlertDialog.Builder(this,R.style.AppAlertDialog)
                .setTitle(R.string.delete_messages)
                .setMessage(R.string.delete_messages_help)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes, (dialog, which) -> new Thread(()-> {
                    try{
                        DbHelper.clearConversation(address,
                                (DxApplication) getApplication());
                    }catch (Exception ignored) {}
                    runOnUiThread(()->{
                        scrollDownFab.setVisibility(View.GONE);
                        updateUi(false,false);
                    });
                }).start())
                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetSession() {
        new AlertDialog.Builder(this,R.style.AppAlertDialog)
                .setTitle(R.string.action_reset_session)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    try{
                        DxApplication app =  ((DxApplication) getApplication());
                        if(app.getEntity()==null){
                            return;
                        }
                        app.getEntity().getStore().deleteSession(new SignalProtocolAddress(address,1));
                        updateUi(true,true);
                    }catch (Exception ignored) {}
                })
                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                }).show();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        if(requestCode == REQUEST_PICK_FILE){
//            try{
//                if(data == null || data.getData() == null){
//                    return;
//                }
//                Intent intent = new Intent(this, FileViewerActivity.class);
//                intent.putExtra("uri",data.getData());
//                intent.putExtra("filename", FileHelper.getFileName(data.getData(),this));
//                intent.putExtra("size",FileHelper.getFileSize(data.getData(),this));
//                intent.putExtra("address", Objects.requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
//                startActivity(intent);
//            }catch (Exception e){e.printStackTrace();}
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_STORAGE_REQUEST_CODE) {
//            new AlertDialog.Builder(this, R.style.AppAlertDialog)
//                    .setTitle(R.string.denied_microphone)
//                    .setMessage(R.string.denied_microphone_help)
//                    .setIcon(android.R.drawable.ic_dialog_alert)
//                    .setPositiveButton(R.string.ask_me_again, (dialog, which) -> getMicrophonePerms())
//                    .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
//                    });
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //permission was just granted by the user
                openGallery();
            }
        }
    }

    public void getReadStoragePerms(){
//        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//            new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
//                    .setTitle(R.string.read_storage_perm_ask_title)
//                    .setMessage(R.string.why_need_read_storage)
//                    .setIcon(android.R.drawable.ic_dialog_alert)
//                    .setPositiveButton(R.string.ask_for_mic_btn, (dialog, which) -> requestPermissions(
//                            new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
//                            READ_STORAGE_REQUEST_CODE))
//                    .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
//
//                    });
//        } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, READ_STORAGE_REQUEST_CODE);
        }
//        }
    }

    public void getMicrophonePerms(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
                    .setTitle(R.string.mic_perm_ask_title)
                    .setMessage(R.string.why_need_mic)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.ask_for_mic_btn, (dialog, which) -> requestPermissions(
                            new String[] { Manifest.permission.RECORD_AUDIO },
                            RECORD_AUDIO_REQUEST_CODE))
                    .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                    });
            } else {
                requestPermissions(
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    RECORD_AUDIO_REQUEST_CODE);
            }
        }
    }

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * '@param' 'level' the memory-related event that was raised.
     */
//    public void onTrimMemory(int level) {
//
//        // Determine which lifecycle or system event was raised.
//        switch (level) {
//
//            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
//            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
//                onSupportNavigateUp();
//                break;
//            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
//            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
//            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
//
//                /*
//                   Release any memory that your app doesn't need to run.
//
//                   The device is running low on memory while the app is running.
//                   The event raised indicates the severity of the memory-related event.
//                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
//                   begin killing background processes.
//                */
//
//                /*
//                   Release as much memory as the process can.
//
//                   The app is on the LRU list and the system is running low on memory.
//                   The event raised indicates where the app sits within the LRU list.
//                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
//                   the first to be terminated.
//                */
//
//                /*
//                   Release any UI objects that currently hold memory.
//
//                   The user interface has moved to the background.
//                */
//
////                onSupportNavigateUp();
//
//            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
//            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
//            default:
//                /*
//                  Release any non-critical data structures.
//
//                  The app received an unrecognized memory level value
//                  from the system. Treat this as a generic low-memory message.
//                */
//                stopCheckingMessages();
//                break;
//        }
//    }

    @Override
    public void doStuff() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
            }
        }
    }
}