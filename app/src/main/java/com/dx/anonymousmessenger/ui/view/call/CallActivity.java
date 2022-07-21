package com.dx.anonymousmessenger.ui.view.call;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.call.DxCallService;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class CallActivity extends DxActivity {

    private static final int REQUEST_CODE = 1;
    TextView name;
    TextView phone;
    TextView state;
    TextView timer;
    TextView uploader;
//    TextView downloader;
    FloatingActionButton hangup;
    FloatingActionButton answer;
    FloatingActionButton speaker;
    FloatingActionButton mute;
    FloatingActionButton upload;
//    FloatingActionButton download;

    String address;
    BroadcastReceiver br;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_call);
        address = getIntent().getStringExtra("address");
        name = findViewById(R.id.name);
        if(getIntent().getStringExtra("nickname")!=null){
            name.setText(getIntent().getStringExtra("nickname"));
        }else{
            setNameFromAddress(address);
        }
        phone = findViewById(R.id.phoneNumber);
        phone.setText(address);
        state = findViewById(R.id.callStateLabel);
        state.setText(R.string.connecting);
        timer = findViewById(R.id.elapsedTime);
        timer.setText("00:00");
        hangup = findViewById(R.id.hangup_fab);
        hangup.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, DxCallService.class);
            serviceIntent.putExtra("address",address.substring(0,10));
            serviceIntent.setAction("hangup");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }else {
                startService(serviceIntent);
            }
            hangupCall();
        });
        answer = findViewById(R.id.answer_fab);
        answer.setOnClickListener(v -> new Thread(()->{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                        "address"),
                        (DxApplication) getApplication());
                if(fullAddress == null){
                    return;
                }
                address = fullAddress;
                answerCall(true);
                Intent serviceIntent = new Intent(this, DxCallService.class);
                serviceIntent.setAction("answer");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                }else {
                    startService(serviceIntent);
                }
            }else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO },REQUEST_CODE);
                }
            }
        }).start());
        speaker = findViewById(R.id.speaker_fab);
        speaker.setOnClickListener(v -> {
            try {
                if(((DxApplication)getApplication()).isInCall()){
                    if(speaker.getAlpha()<0.8){
                        speaker.setAlpha((float) 1.0);
                        ((DxApplication)getApplication()).getCc().setSpeakerPhoneOn(true);
                    }else{
                        speaker.setAlpha((float) 0.26);
                        ((DxApplication)getApplication()).getCc().setSpeakerPhoneOn(false);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        mute = findViewById(R.id.mute_fab);
        mute.setOnClickListener(v -> {
            try {
                if(((DxApplication)getApplication()).isInCall()){
                    if(mute.getAlpha()<0.8){
                        mute.setAlpha((float) 1.0);
                        ((DxApplication)getApplication()).getCc().setMuteMic(true);
                    }else{
                        mute.setAlpha((float) 0.26);
                        ((DxApplication)getApplication()).getCc().setMuteMic(false);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        upload = findViewById(R.id.fab_upload);
//        download = findViewById(R.id.fab_download);
        uploader = findViewById(R.id.txt_upload);
//        downloader = findViewById(R.id.txt_download);

        ((DxApplication)getApplication()).enableStrictMode();

    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(()->{
//            try{
//                Thread.sleep(200);
//            }catch (Exception ignored) {}
            String action = getIntent().getAction();
            handleAction(action,getIntent());
        }).start();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action=intent.getStringExtra("action");
                new Thread(()->{
                    handleAction(action,intent);
                }).start();
            }
        };
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(br,new IntentFilter("call_action"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setNameFromAddress(String address) {
        new Thread(()->{
            String nickname = DbHelper.getContactNickname(DbHelper.getFullAddress(address,((DxApplication)getApplication())),((DxApplication)getApplication()));
            runOnUiThread(()-> name.setText(nickname));
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void handleAction(String action, Intent intent) {
        if(action!=null){
            switch (action){
                case "upload":
                    if(intent.getLongExtra("upload",0)>0){
                        runOnUiThread(()-> uploader.setText(Utils.humanReadableByteCountBin(intent.getLongExtra("upload",0))));
                        runOnUiThread(()-> upload.setAlpha((float) 1));
                    }else{
                        runOnUiThread(()-> upload.setAlpha((float) 0.26));
                    }
                    break;
//                case "download":
//                    runOnUiThread(()-> downloader.setText(intent.getIntExtra("download",0)+"b"));
//                    if(intent.getIntExtra("download",0)>0){
//                        runOnUiThread(()-> download.setAlpha((float) 1));
//                    }else{
//                        runOnUiThread(()-> download.setAlpha((float) 0.26));
//                    }
//                    break;
                case "timer":
                    runOnUiThread(()-> timer.setText(Utils.secToTime(intent.getIntExtra("time",0))));
                    break;
                case DxCallService.ACTION_START_INCOMING_CALL:
                    runOnUiThread(()->{
                        state.setText(R.string.incoming_call);
                        showAnswerButton();
                    });
                    break;
                case DxCallService.ACTION_START_OUTGOING_CALL_RESPONSE:
                    runOnUiThread(()-> state.setText(R.string.ringing));
                    break;
                case "hangup":
                    runOnUiThread(this::hangupCall);
                    break;
                case "answer":
                case "running":
                    runOnUiThread(()-> state.setText(R.string.connected));
                    break;
                case "trying":
                case "ringing":
                case "connecting":
                case "connected":
                    runOnUiThread(()-> state.setText(action));
                    break;
                case "start_out_call":
                    runOnUiThread(()-> state.setText(R.string.trying));
                    Intent serviceIntent = new Intent(this, DxCallService.class);
                    serviceIntent.setAction("start_out_call");
                    serviceIntent.putExtra("address",Objects.requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
                    serviceIntent.putExtra("nickname",getIntent().getStringExtra("nickname"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    }else{
                        startService(serviceIntent);
                    }
                    break;
                case "":
                    if(((DxApplication)getApplication()).isInActiveCall()){
                        runOnUiThread(()-> state.setText("connected"));
                    }
                    break;
            }
        }
    }

    private void showAnswerButton() {
        answer.setVisibility(View.VISIBLE);
    }

    public void hangupCall(){
        finish();
    }

    public void answerCall(){
        runOnUiThread(()-> answer.setVisibility(View.GONE));
    }

    public void answerCall(boolean forReal){
        answerCall();
        if(forReal){
            ((DxApplication)getApplication()).commandCallService(address,"answer");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length <= 0 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        new AlertDialog.Builder(getApplicationContext(), R.style.AppAlertDialog)
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
                    }  // Permission is granted. Continue the action or workflow
            // in your app.

        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    public void getMicrophonePerms(){
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        }  // You can use the API that requires the permission.

    }
}