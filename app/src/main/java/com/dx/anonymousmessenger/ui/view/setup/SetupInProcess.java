package com.dx.anonymousmessenger.ui.view.setup;

import android.app.AlertDialog;
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
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.app.AppActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.SettingsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Date;

public class SetupInProcess extends DxActivity implements ComponentCallbacks2 {

    private BroadcastReceiver mMyBroadcastReceiver;
    private TextView statusText;
    private Thread serverChecker = null;
    private AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_setup_in_process);

        statusText = findViewById(R.id.status_text);
        Button gotoContact = findViewById(R.id.btn_goto_contacts);
        Button restartTorButton = findViewById(R.id.btn_restart_tor);
        restartTorButton.setVisibility(View.VISIBLE);
        restartTorButton.setOnClickListener(v -> restartTorWithAlert());
//        if(!getIntent().getBooleanExtra("first_time",true)){
            gotoContact.setVisibility(View.VISIBLE);
            gotoContact.setOnClickListener(v -> {
                ((DxApplication)getApplication()).setExitingHoldup(true);
                Entity myEntity = new Entity((DxApplication)getApplication());
                ((DxApplication)getApplication()).setEntity(myEntity);
                Intent intent = new Intent(this, AppActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("force_app",true);
                startActivity(intent);
                finish();
            });
//        }
        FloatingActionButton settings = findViewById(R.id.fab_settings);
        settings.setOnClickListener((v) -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void restartTorWithAlert() {
        new AlertDialog.Builder(this,R.style.AppAlertDialog)
            .setTitle(R.string.restart_tor)
            .setMessage(R.string.restart_tor_explain)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> ((DxApplication) getApplication()).restartTor())
            .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onStop() {
        if(alert!=null){
            alert.dismiss();
            alert = null;
        }
        super.onStop();
        if(mMyBroadcastReceiver==null){
            return;
        }
        stopCheckingServerReady();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
    }

    @Override
    public void onStart() {
        super.onStart();
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
//                new Thread(()->{
                    try{
                        updateUi(intent.getStringExtra("tor_status"));
                    }catch (Exception ignored) {}
//                }).start();
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
            try {
                Thread.sleep(1500);
                ((DxApplication) getApplication()).clearNotification(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!((DxApplication) getApplication()).isExitingHoldup() && ((DxApplication) getApplication()).getTorStartTime()!=0 && new Date().getTime()>(((DxApplication) getApplication()).getTorStartTime()+5000) && !((DxApplication) getApplication()).isServerReady() && ((DxApplication) getApplication()).getAndroidTorRelay()==null || (((DxApplication) getApplication()).getAndroidTorRelay()!=null && !((DxApplication) getApplication()).getAndroidTorRelay().isTorRunning())){
                runOnUiThread(this::displayTorError);
            }
//            while (true){
                try{
                    if(((DxApplication) getApplication()).isServerReady()){
                        runOnUiThread(()->{
                            Intent intent = new Intent(this, AppActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        });
//                        return;
                    }
//                    Thread.sleep(1000);
                }catch (Exception ignored){
//                    break;
                }
//            }
        });
        serverChecker.start();
    }

    private void displayTorError() {
        alert = new AlertDialog.Builder(this, R.style.AppAlertDialog)
                .setTitle(R.string.tor_error_title)
                .setMessage(R.string.tor_error)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.restart_tor, (dialog, whichButton) -> ((DxApplication) getApplication()).restartTor())
                .setNegativeButton(R.string.stay_offline, (dialog, whichButton) -> {
                }).show();
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
        if(torStatus.contains("tor_error")){
//            torStatus = getString(R.string.tor_error);
            runOnUiThread(this::displayTorError);
            return;
        }
//        if(torStatus.contains("DisableNetwork is set")){
//            torStatus = getString(R.string.waiting_for_tor);
//            runOnUiThread(()-> statusText.setText(getString(R.string.waiting_for_tor)));
//            return;
//        }
        if(torStatus.contains("Opening Socks listener")){
            runOnUiThread(()-> statusText.setText(getString(R.string.opening_socks_listener)));
            return;
        }
        if(torStatus.contains("Socks listener listening")){
            runOnUiThread(()-> statusText.setText(getString(R.string.socks_listener_listening)));
            return;
        }
        if(torStatus.contains("Opened Socks listener")){
            runOnUiThread(()-> statusText.setText(getString(R.string.opened_socks_listener)));
            return;
        }
        if(torStatus.contains("Opening Control listener")){
            runOnUiThread(()-> statusText.setText(getString(R.string.opening_control_listener)));
            return;
        }
        if(torStatus.contains("Control listener listening")){
            runOnUiThread(()-> statusText.setText(getString(R.string.control_listener_listening)));
            return;
        }
        if(torStatus.contains("Opened Control listener")){
            runOnUiThread(()-> statusText.setText(getString(R.string.opened_control_listener)));
            return;
        }
        if(torStatus.contains("Bootstrapped 100%") || torStatus.contains("ALL GOOD")){
            ((DxApplication)getApplication()).setExitingHoldup(true);
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
            if(getIntent().getBooleanExtra("first_time",true)){
                ((DxApplication) this.getApplication()).sendNotification(getString(R.string.ready_to_chat),
                        getString(R.string.you_got_all_you_need),false);
            }
            runOnUiThread(()->{
                try {
                    statusText.setText(torStatus);
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
                runOnUiThread(()-> statusText.setText(torStatus));
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