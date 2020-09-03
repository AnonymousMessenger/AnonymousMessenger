package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class CallActivity extends AppCompatActivity {

    TextView name;
    TextView phone;
    TextView state;
    TextView timer;
    FloatingActionButton hangup;
    FloatingActionButton answer;

    String address;
    BroadcastReceiver br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_call);
        String action = getIntent().getAction();
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
            serviceIntent.setAction("hangup");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }else {
                startService(serviceIntent);
            }
            hangupCall();
        });
        answer = findViewById(R.id.answer_fab);
        answer.setOnClickListener(v -> {
            answerCall(true);
            Intent serviceIntent = new Intent(this, DxCallService.class);
            serviceIntent.setAction("answer");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }else {
                startService(serviceIntent);
            }
        });

        ((DxApplication)getApplication()).enableStrictMode();
        new Thread(()->{
            try{
                Thread.sleep(250);
            }catch (Exception ignored) {}
            handleAction(action,getIntent());
        }).start();

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action=intent.getStringExtra("action");
                handleAction(action,intent);
            }
        };
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(br,new IntentFilter("call_action"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void setNameFromAddress(String address) {
        new Thread(()->{
            String nickname = DbHelper.getContactNickname(address,((DxApplication)getApplication()));
            runOnUiThread(()-> name.setText(nickname));
        }).start();
    }

    private void handleAction(String action, Intent intent) {
        if(action!=null){
            switch (action){
                case "timer":
                    runOnUiThread(()-> timer.setText(Utils.getMinutesAndSecondsFromSeconds(intent.getIntExtra("time",0))));
                    break;
                case CallService.ACTION_START_INCOMING_CALL:
                    runOnUiThread(()->{
                        state.setText(R.string.incoming_call);
                        showAnswerButton();
                    });
                    break;
                case CallService.ACTION_START_OUTGOING_CALL_RESPONSE:
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
                    serviceIntent.putExtra("address",getIntent().getStringExtra("address"));
                    serviceIntent.putExtra("nickname",getIntent().getStringExtra("nickname"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    }else{
                        startService(serviceIntent);
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
        answer.setVisibility(View.GONE);
    }

    public void answerCall(boolean forReal){
        answerCall();
        if(forReal){
            ((DxApplication)getApplication()).commandCallService(address,"answer");
        }
    }
}