package com.dx.anonymousmessenger;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.IOException;

public class MyService extends Service {
//    private final String DB_FILE_NAME = "dxam.db";//future name, maybe
//    private String signalProtocolStore;
//    private String messageAdapter;
//    private String dbAdapter;
//    private String netAdapter;
//    private String torAdapter;
//    private String webAdapter;
//    private String kj;
    private int count = 0;
    public final static String SERVICE_NOTIFICATION_CHANNEL = "service_running";
    private Thread torThread;
    private ConnectionStateMonitor csm = new ConnectionStateMonitor();
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

        Notification ntf = app.sendNotification("Service running", "click here to hide notification", SERVICE_NOTIFICATION_CHANNEL);
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
        torThread = app.startTor(1);
    }

    public void startTor(int force){
        if(force>0){
            torThread = app.startTor(2);
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
        // Stop the service
        stopSelf();
        // Hide the UI
        //hideUi();
        // Wait for shutdown to complete, then exit
        new Thread(() -> {
            try {
                if(csm!=null){
                    csm.disable();
                }
                if(app!=null){
                    app.getAndroidTorRelay().shutDown();
                    app.emptyVars();
                    app.shutdown(0);
                }
            } catch (IOException ignored) {}
        }).start();
    }
}