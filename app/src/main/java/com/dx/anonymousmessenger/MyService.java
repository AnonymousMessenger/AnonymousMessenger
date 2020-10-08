package com.dx.anonymousmessenger;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MyService extends Service {
    public final static String SERVICE_NOTIFICATION_CHANNEL = "service_running";
    private final ConnectionStateMonitor csm = new ConnectionStateMonitor();
    private DxApplication app;

    public MyService() {
    }

    protected void createAccount(){

    }

    @Override
    public void onCreate() {
        app = (DxApplication) getApplication();
        if(app.getAccount()==null||app.getAccount().getPassword()==null){
            return;
        }

        super.onCreate();

        Notification ntf = app.getServiceNotification("Still running in background", "click here to hide notification", SERVICE_NOTIFICATION_CHANNEL);
        startForeground(3, ntf);
        csm.enable(this.getApplication());
        startTor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("inputExtra");
        if (command != null && command.contains("reconnect now")) {
            startTor(2);
        }else if (command != null && command.contains("shutdown now")) {
            shutdownFromBackground();
        }
        //do heavy work on a background thread
        //stopSelf();

        return START_NOT_STICKY;
    }

    public void startTor(){
        app.startTor(1);
    }

    public void startTor(int force){
        if(force>0){
            app.startTor(2);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdownFromBackground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void shutdownFromBackground() {
        // Hide the UI
        //hideUi();
        // Wait for shutdown to complete, then exit
        new Thread(() -> {
            try {
                csm.disable();
                if(app!=null){
                    if(app.getAndroidTorRelay()!=null){
                        app.getAndroidTorRelay().shutDown();
                    }
                    app.emptyVars();
                    app.shutdown(0);
                }
                stopSelf();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SHUTDOWN ERROR","ya");
            }
        }).start();
    }
}