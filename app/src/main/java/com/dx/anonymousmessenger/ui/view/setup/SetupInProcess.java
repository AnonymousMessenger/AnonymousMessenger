package com.dx.anonymousmessenger.ui.view.setup;

import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.app.AppActivity;

public class SetupInProcess extends DxActivity implements ComponentCallbacks2 {

    private BroadcastReceiver mMyBroadcastReceiver;
    private TextView statusText;
    private Thread serverChecker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_setup_in_process);

        statusText = findViewById(R.id.status_text);
        Button gotoContact = findViewById(R.id.btn_goto_contacts);
        Button restartTorButton = findViewById(R.id.btn_restart_tor);
        restartTorButton.setVisibility(View.VISIBLE);
        restartTorButton.setOnClickListener(v -> ((DxApplication)getApplication()).restartTor());
        if(!getIntent().getBooleanExtra("first_time",true)){
            gotoContact.setVisibility(View.VISIBLE);
            gotoContact.setOnClickListener(v -> {
                ((DxApplication)getApplication()).setExitingHoldup(true);
                Intent intent = new Intent(this, AppActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("force_app",true);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mMyBroadcastReceiver==null){
            return;
        }
        stopCheckingServerReady();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(((DxApplication) getApplication()).isServerReady()){
            Intent intent = new Intent(this, AppActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(mMyBroadcastReceiver!=null){
            return;
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
        checkServerReady();
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void checkServerReady(){
        if(serverChecker!=null){
            return;
        }
        serverChecker = new Thread(()->{
            while (true){
                try{
                    Thread.sleep(1000);
                    if(((DxApplication) getApplication()).isServerReady()){
                        runOnUiThread(()->{
                            Intent intent = new Intent(this, AppActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        });
                    }
                }catch (Exception ignored){break;}
            }
        });
        serverChecker.start();
    }

    public void stopCheckingServerReady(){
        if(serverChecker==null){
            return;
        }
        if(serverChecker.isAlive()){
            serverChecker.interrupt();
        }
        serverChecker = null;
    }

    public void updateUi(String torStatus){
        if (torStatus==null){
            return;
        }
        if(((DxApplication)getApplication()).isExitingHoldup()){
            return;
        }
//        if(!torStatus.toUpperCase().contains("NOTICE")){
//            return;
//        }
        if(torStatus.contains("DisableNetwork is set")){
            torStatus = getString(R.string.waiting_for_tor);
        }
        if(torStatus.contains("Opening Socks listener")){
            torStatus = getString(R.string.opening_socks_listener);
        }
        if(torStatus.contains("Socks listener listening")){
            torStatus = getString(R.string.socks_listener_listening);
        }
        if(torStatus.contains("Opened Socks listener")){
            torStatus = getString(R.string.opened_socks_listener);
        }
        if(torStatus.contains("Opening Control listener")){
            torStatus = getString(R.string.opening_control_listener);
        }
        if(torStatus.contains("Control listener listening")){
            torStatus = getString(R.string.control_listener_listening);
        }
        if(torStatus.contains("Opened Control listener")){
            torStatus = getString(R.string.opened_control_listener);
        }
        if(torStatus.contains("Bootstrapped 100%") || torStatus.contains("ALL GOOD")){
            ((DxApplication)getApplication()).setExitingHoldup(true);
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
            if(getIntent().getBooleanExtra("first_time",true)){
                ((DxApplication) this.getApplication()).sendNotification(getString(R.string.ready_to_chat),
                        getString(R.string.you_got_all_you_need),false);
            }
            String finalTorStatus = torStatus;
            runOnUiThread(()->{
                try {
                    statusText.setText(finalTorStatus);
                }catch (Exception ignored) {}
            });
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(this, AppActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }else{
            try {
                String finalTorStatus1 = torStatus;
                runOnUiThread(()-> statusText.setText(finalTorStatus1));
            }catch (Exception ignored){}
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

                finish();
                break;

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
    }
}