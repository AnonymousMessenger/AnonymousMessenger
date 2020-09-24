package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SetupInProcess extends AppCompatActivity {

    private BroadcastReceiver mMyBroadcastReceiver;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_setup_in_process);
        new Thread(()->{
            if(((DxApplication) getApplication()).isServerReady()){
                Intent intent = new Intent(this, AppActivity.class);
                startActivity(intent);
                finish();
            }
        }).start();
        statusText = findViewById(R.id.status_text);
//        try {
//            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if(((DxApplication) getApplication()).isServerReady()){
            Intent intent = new Intent(this, AppActivity.class);
            startActivity(intent);
            finish();
        }
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                new Thread(()->{
                    try{
                        updateUi(intent.getStringExtra("tor_status"));
                    }catch (Exception ignored) {}
                }).start();
            }
        };
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateUi(String torStatus){
        if (torStatus==null){
            return;
        }
        if(torStatus.contains("ALL GOOD") || torStatus.contains("message") || torStatus.contains("status")){
            runOnUiThread(()->{
                try {
                    statusText.setText(torStatus);
                }catch (Exception ignored) {}
                Intent intent = new Intent(this, AppActivity.class);
                startActivity(intent);
                finish();
            });
            new Thread(()->{
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
                if(getIntent().getBooleanExtra("first_time",true)){
                    ((DxApplication) this.getApplication()).sendNotification("Ready to chat securely!",
                            "You got all you need to chat securely with your friends!",false);
                }
            }).start();
        }else{
            try {
                runOnUiThread(()->{
                    statusText.setText(torStatus);
                });
            }catch (Exception ignored){}
        }
    }
}