package com.dx.anonymousmessenger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Explode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.media.MediaRecycleViewAdapter;
import com.dx.anonymousmessenger.messages.MessageListAdapter;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MessageListActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, ComponentCallbacks2, MyRecyclerViewAdapter.ItemClickListener {

    private static final int REQUEST_CODE = 1;
    private static final int READ_STORAGE_REQUEST_CODE = 2;
    public TextView quoteTextTyping;
    public TextView quoteSenderTyping;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    List<QuotedUserMessage> messageList = new ArrayList<>();
//    public Handler mainThread = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mMyBroadcastReceiver;
    private Thread messageChecker = null;
    private Button send = null;
    private EditText txt = null;
    private TextView txtAudioTimer = null;
    private FloatingActionButton audio, file;
    private LinearLayout audioLayout;
    private FloatingActionButton scrollDownFab;
    private BroadcastReceiver timeBroadcastReceiver;
    private TextView status;
    private RecyclerView mediaRecyclerView;
    private LinearLayout picsHelp;
    private String address;
    private String nickname;
//    private TextView picsHelpText;
//    private ImageView syncedImg;
//    private ImageView unsyncedImg;
//    private TextView syncTxt;
//    private Toolbar syncToolbar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_message_list);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getIntent().getStringExtra("nickname"));
        getSupportActionBar().setSubtitle(getIntent().getStringExtra("address"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        address = getIntent().getStringExtra("address");
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
        status = findViewById(R.id.txt_status);
        status.setOnClickListener((v)-> status.setVisibility(View.GONE));
//        syncedImg = findViewById(R.id.synced_image);
//        unsyncedImg = findViewById(R.id.unsynced_image);
//        syncTxt = findViewById(R.id.sync_text);
//        syncToolbar = findViewById(R.id.sync_toolbar);
        scrollDownFab = findViewById(R.id.fab_scroll_down);
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication(), mMessageRecycler);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        mMessageRecycler.setAdapter(mMessageAdapter);
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
        scrollDownFab.setOnClickListener(v -> mMessageRecycler.smoothScrollToPosition(messageList.size()-1));
        send.setOnClickListener(v -> {
            if(((DxApplication) getApplication()).getEntity()==null){
                Snackbar.make(send, R.string.no_encryption_yet,Snackbar.LENGTH_SHORT).show();
                return;
            }
            if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(getIntent().getStringExtra("address"),1))){
                Snackbar.make(send, R.string.no_session,Snackbar.LENGTH_SHORT).show();
                new Thread(()-> MessageSender.sendKeyExchangeMessage(((DxApplication)getApplication()),getIntent().getStringExtra("address"))).start();
                return;
            }
            TextView quoteSenderTyping = findViewById(R.id.quote_sender_typing);
            TextView quoteTextTyping = findViewById(R.id.quote_text_typing);
            QuotedUserMessage msg =
                    new QuotedUserMessage(quoteSenderTyping.getText().toString().equals(getString(R.string.you))?
                            ((DxApplication)getApplication()).getAccount().getNickname():getIntent().getStringExtra("nickname"),quoteTextTyping.getText().toString(),((DxApplication)getApplication()).getHostname(),txt.getText().toString(),((DxApplication)getApplication()).getAccount().getNickname(),new Date().getTime(),false,getIntent().getStringExtra("address"),false);
            messageList.add(msg);
            new Thread(()-> MessageSender.sendMessage(msg,((DxApplication)getApplication()),getIntent().getStringExtra("address"))).start();
            txt.setText("");
            quoteSenderTyping.setText("");
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteTextTyping.setVisibility(View.GONE);
            audio.setVisibility(View.VISIBLE);
            file.setVisibility(View.VISIBLE);
            mMessageAdapter.notifyItemInserted(messageList.size()-1);
            mMessageRecycler.smoothScrollToPosition(messageList.size() - 1);
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
        audio.setOnTouchListener((arg0, arg1) -> {
            if (arg1.getAction()== MotionEvent.ACTION_DOWN){
                txt.setVisibility(View.GONE);
                quoteTextTyping.setVisibility(View.GONE);
                quoteSenderTyping.setVisibility(View.GONE);
                audioLayout.setVisibility(View.VISIBLE);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    new Thread(()->{
                        try{
                            if(mMessageAdapter!=null && mMessageAdapter.ap!=null){
                                mMessageAdapter.ap.stop(1);
                                mMessageAdapter.ap = null;
                            }
                        }catch (Exception ignored) {}
                    }).start();

                    Intent intent = new Intent(this, AudioRecordingService.class);
                    intent.setAction("start_recording");
                    intent.putExtra("address",getIntent().getStringExtra("address"));
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
                    requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO },REQUEST_CODE);
                }
            }else if(arg1.getAction()== MotionEvent.ACTION_UP){
                audioLayout.setVisibility(View.GONE);
                txt.setVisibility(View.VISIBLE);
                if(quoteTextTyping.getText().length()>0){
                    quoteTextTyping.setVisibility(View.VISIBLE);
                    quoteSenderTyping.setVisibility(View.VISIBLE);
                }
                Intent intent = new Intent(this, AudioRecordingService.class);
                stopService(intent);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(timeBroadcastReceiver);
                timeBroadcastReceiver = null;
                txtAudioTimer.setText(getString(R.string._00_00));
            }else if(arg1.getAction()== MotionEvent.ACTION_CANCEL){
                audioLayout.setVisibility(View.GONE);
                txt.setVisibility(View.VISIBLE);
                if(quoteTextTyping.getText().length()>0){
                    quoteTextTyping.setVisibility(View.VISIBLE);
                    quoteSenderTyping.setVisibility(View.VISIBLE);
                }
                Intent intent = new Intent(this, AudioRecordingService.class);
                stopService(intent);
                LocalBroadcastManager.getInstance(this).unregisterReceiver(timeBroadcastReceiver);
                timeBroadcastReceiver = null;
                txtAudioTimer.setText(getString(R.string._00_00));
            }
            return true;
        });
//        audio.setOnClickListener(v -> {
//            txt.setVisibility(View.GONE);
//            audioLayout.setVisibility(View.VISIBLE);
//        });
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
//            pickImage();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_CODE);
                return;
            }
            mediaRecyclerView.setVisibility(View.VISIBLE);
            send.setVisibility(View.GONE);
            audio.setVisibility(View.GONE);
            file.setVisibility(View.GONE);
            txt.setVisibility(View.GONE);
            quoteTextTyping.setVisibility(View.GONE);
            quoteSenderTyping.setVisibility(View.GONE);
            picsHelp.setVisibility(View.VISIBLE);

            new Thread(()->{
//            ArrayList<Bitmap> pics = new ArrayList<>();
                ArrayList<String> paths = new ArrayList<>();
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String[] projection = { MediaStore.Images.ImageColumns.DATA ,MediaStore.Images.Media.DISPLAY_NAME};
                Cursor cursor = this.getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media._ID + " DESC");
                try {
                    if (cursor != null) {
                        cursor.moveToFirst();
                        do{
//                        String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                            paths.add(path);
//                        pics.add(BitmapFactory.decodeFile(path));
                        }while(cursor.moveToNext());
                        cursor.close();
                    }
                    // set up the RecyclerView
                    new Handler(Looper.getMainLooper()).post(()->{

                        LinearLayoutManager horizontalLayoutManager
                                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
                        mediaRecyclerView.setLayoutManager(horizontalLayoutManager);
                        MediaRecycleViewAdapter adapter = new MediaRecycleViewAdapter(this, paths);
                        adapter.setClickListener(this::onItemClick);
                        mediaRecyclerView.setAdapter(adapter);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });

        updateUi();
        ping();

        //run db message checker to delete any old messages then tell us to update ui
        checkMessages();

        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), R.string.crash_message, Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }.start();
            try
            {
                Thread.sleep(4000); // Let the Toast display before app will get shutdown
            }
            catch (InterruptedException ignored) {    }
            System.exit(2);
        });
    }

    @Override
    public void onItemClick(View view, int position) {
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
        Intent intent = new Intent(this,PictureViewerActivity.class);
        intent.putExtra("address",address);
        intent.putExtra("nickname",nickname);
        if(mediaRecyclerView!=null && mediaRecyclerView.getAdapter()!=null){
            String path = ((MediaRecycleViewAdapter)mediaRecyclerView.getAdapter()).mPaths.get(position);
            intent.putExtra("path",path);
            intent.putExtra("type","image");
            startActivity(intent);
        }
    }

//    public void pickImage() {
//        try{
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
//                intent.putExtra("crop", "true");
//                intent.putExtra("scale", true);
//                intent.putExtra("outputX", 256);
//                intent.putExtra("outputY", 256);
//                intent.putExtra("aspectX", 1);
//                intent.putExtra("aspectY", 1);
//                intent.putExtra("return-data", true);
//                if (intent.resolveActivity(getPackageManager()) != null) {
//                    startActivityForResult(intent, READ_STORAGE_REQUEST_CODE);
//                }else{
//                    Snackbar.make(file,"You do not have a gallery application installed",Snackbar.LENGTH_LONG).show();
//                }
//            }else{
//                getReadStoragePerms();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }

    public void ping(){
        new Thread(()->{
            boolean b = TorClientSocks4.testAddress(((DxApplication) getApplication()), getIntent().getStringExtra("address"));
            try {
                Thread.sleep(500);
            } catch (Exception ignored) {}
            Handler h = new Handler(Looper.getMainLooper());
            h.post(()->{
                try{
                    if(b){
                        ((DxApplication)getApplication()).addToOnlineList(getIntent().getStringExtra("address"));
                        status.setText(R.string.user_is_online);
                        ((DxApplication)getApplication()).queueUnsentMessages(getIntent().getStringExtra("address"));
                    }else{
                        ((DxApplication)getApplication()).onlineList.remove(getIntent().getStringExtra("address"));
                        status.setText(R.string.user_is_offline);
                    }
                    status.setVisibility(View.VISIBLE);
                    Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                    status.startAnimation(slideUp);
                }catch (Exception ignored) {}
            });
            try {
                Thread.sleep(2000);
            } catch (Exception ignored) {}
            h.post(()->{
                try {
                    Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
                    status.startAnimation(slideDown);
                    status.setVisibility(View.GONE);
                }catch (Exception ignored) {}
            });
        }).start();
    }

    public void checkMessages(){
        if(messageChecker!=null){
            return;
        }
        messageChecker = new Thread(()->{
            while (messageList!=null){
                try{
                    Thread.sleep(5000);
                    List<QuotedUserMessage> tmp = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                            getIntent().getStringExtra("address"));

                    if(messageList.size() != tmp.size()){
                        updateUi(tmp);
                    }
                }catch (Exception ignored){messageList=null;break;}
            }
            //maybe remove this line?
            messageList = null;
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

    public void updateUi(){
        try{
            messageList = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                    getIntent().getStringExtra("address"));
            runOnUiThread(()->{
                try{
//                    mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
//                    mMessageRecycler.setAdapter(mMessageAdapter);
                    mMessageAdapter.setMessageList(messageList);
                    mMessageAdapter.notifyDataSetChanged();
                    if(!messageList.isEmpty()){
                        mMessageRecycler.scrollToPosition(messageList.size() - 1);
                        ((DxApplication)getApplication()).clearMessageNotification();
                        if(!messageList.get(messageList.size()-1).getAddress().equals(((DxApplication)getApplication()).getHostname())){
                            String newName = messageList.get(messageList.size()-1).getSender();
                            Objects.requireNonNull(getSupportActionBar()).setTitle(newName);
                            getIntent().putExtra("nickname",newName);
                        }
                    }
                }catch (Exception ignored) {}
            });
        }catch (Exception ignored) {}

    }

    public void updateUi(List<QuotedUserMessage> tmp){
        new Thread(()->{
            if(quoteTextTyping==null && mMyBroadcastReceiver==null){
                return;
            }
            messageList = tmp;
            runOnUiThread(()->{
                try{
//                    mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
//                    mMessageRecycler.setAdapter(mMessageAdapter);
                    mMessageAdapter.setMessageList(messageList);
                    mMessageAdapter.notifyDataSetChanged();
                    mMessageRecycler.smoothScrollToPosition(messageList.size() - 1);
                }catch (Exception ignored){}
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMessages();
        if(mMyBroadcastReceiver!=null){
            return;
        }
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if(intent.getStringExtra("error")!=null){
                    try{
                        Snackbar.make(send, Objects.requireNonNull(intent.getStringExtra("error")),Snackbar.LENGTH_SHORT).show();
                    }catch (Exception ignored) {}
                }
                updateUi();
            }
        };
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMyBroadcastReceiver,new IntentFilter("your_action"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCheckingMessages();
        if(mMyBroadcastReceiver==null){
            return;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
    }

    @Override
    public boolean onSupportNavigateUp(){
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
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
        switch(item.getItemId()) {
            case R.id.action_call:
                new Thread(()->{
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        if(((DxApplication)getApplication()).isInCall()){
                            runOnUiThread(()-> Snackbar.make(send,"Already in a call",Snackbar.LENGTH_SHORT).show());
                            return;
                        }
                        Intent intent = new Intent(this, CallActivity.class);
                        intent.setAction("start_out_call");
                        intent.putExtra("address",getIntent().getStringExtra("address"));
                        intent.putExtra("nickname",getIntent().getStringExtra("nickname"));
                        startActivity(intent);
                    }else{
                        requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO },REQUEST_CODE);
                    }
                }).start();
                break;
            case R.id.action_settings:
                //add the function to perform here
                try{
                    DxApplication app =  ((DxApplication) getApplication());
                    if(app.getEntity()==null){
                        break;
                    }
                    app.getEntity().getStore().deleteSession(new SignalProtocolAddress(getIntent().getStringExtra("address"),1));
                }catch (Exception ignored) {}
                //((DxSignalKeyStore)app.getEntity().getStore()).removeIdentity(new SignalProtocolAddress(getIntent().getStringExtra("address"),1));
                break;
            case R.id.action_verify_identity:
                stopCheckingMessages();
                Intent intent = new Intent(this, VerifyIdentityActivity.class);
                intent.putExtra("address",getIntent().getStringExtra("address"));
                startActivity(intent);
                break;
            case R.id.action_clear_conversation:
                DbHelper.clearConversation(getIntent().getStringExtra("address"),(DxApplication)getApplication());
                updateUi();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
//            if (grantResults.length > 0 &&
//                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission is granted. Continue the action or workflow
//                // in your app.
//            } else {
                new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
                        .setTitle("Denied Microphone Permission")
                        .setMessage("this way you can't make or receive calls")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.ask_me_again, (dialog, which) -> getMicrophonePerms())
                        .setNegativeButton(R.string.no_thanks, (dialog, which) -> {

                        });
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
//            }
        }//else if(requestCode == READ_STORAGE_REQUEST_CODE){
        //}
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode != RESULT_OK) {
//            return;
//        }
//        if (requestCode == READ_STORAGE_REQUEST_CODE) {
//            final Bundle extras = data.getExtras();
//            if (extras != null) {
//                //Get image
//                Bitmap image = extras.getParcelable("data");
//                ImageView img2send = findViewById(R.id.img_to_send);
//                ImageView img2sendIcon = findViewById(R.id.img_to_send_icon);
//                img2sendIcon.setVisibility(View.VISIBLE);
//                img2send.setVisibility(View.VISIBLE);
//                img2send.setImageBitmap(image);
//            }
//        }
//    }

    public void getReadStoragePerms(){
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
                    .setTitle(R.string.read_storage_perm_ask_title)
                    .setMessage(R.string.why_need_read_storage)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.ask_for_mic_btn, (dialog, which) -> requestPermissions(
                            new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                            READ_STORAGE_REQUEST_CODE))
                    .setNegativeButton(R.string.no_thanks, (dialog, which) -> {

                    });
        } else {
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_CODE);
        }
    }

    public void getMicrophonePerms(){
//        if (ContextCompat.checkSelfPermission(
//                this, Manifest.permission.RECORD_AUDIO) ==
//                PackageManager.PERMISSION_GRANTED) {
//            // You can use the API that requires the permission.
//
//        } else
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
                .setTitle(R.string.mic_perm_ask_title)
                .setMessage(R.string.why_need_mic)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ask_for_mic_btn, (dialog, which) -> requestPermissions(
                        new String[] { Manifest.permission.RECORD_AUDIO },
                        REQUEST_CODE))
                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {

                });
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissions(
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    REQUEST_CODE);
        }
    }

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    public void onTrimMemory(int level) {

        // Determine which lifecycle or system event was raised.
        switch (level) {

            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                onSupportNavigateUp();
                break;
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

//                onSupportNavigateUp();

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                stopCheckingMessages();
                break;
        }
    }

}