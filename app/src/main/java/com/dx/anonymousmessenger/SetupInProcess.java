package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SetupInProcess extends AppCompatActivity {

    private BroadcastReceiver mMyBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateUi(intent.getStringExtra("tor_status"));
        }
    };
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_setup_in_process);
        statusText = findViewById(R.id.status_text);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onResume() {
        super.onResume();
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
        if (torStatus==null){
            return;
        }
        if(torStatus.contains("ALL GOOD") || torStatus.contains("message") || torStatus.contains("status")){
            statusText.setText(torStatus);
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
            ((DxApplication) this.getApplication()).sendNotification("Ready to chat securely!",
                    "You got all you need to chat securely with your friends!",false);
            Intent intent = new Intent(this, AppActivity.class);
            startActivity(intent);
            finish();
        }else{
            try {
                statusText.setText(torStatus);
            }catch (Exception ignored){}
        }
    }
}