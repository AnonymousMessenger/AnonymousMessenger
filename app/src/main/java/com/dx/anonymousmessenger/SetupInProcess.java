package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SetupInProcess extends AppCompatActivity {

    private BroadcastReceiver mMyBroadcastReceiver;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_in_process);
        statusText = findViewById(R.id.status_text);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onResume() {
        super.onResume();
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                updateUi(intent.getStringExtra("tor_status"));
            }
        };
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
    }

    public void updateUi(String torStatus){
        if(torStatus.contains("ALL GOOD")){
            Intent intent = new Intent(this, AppActivity.class);
            startActivity(intent);
            finish();
        }
        statusText.setText(torStatus);
    }
}