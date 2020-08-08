package com.dx.anonymousmessenger;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyService extends Service {
    private final String DB_FILE_NAME = "dxam.db";
    private String signalProtocolStore;
    private String messageAdapter;
    private String dbAdapter;
    private String netAdapter;
    private String torAdapter;
    private String webAdapter;
    private String kj;
    private int count = 0;
    public final static String SERVICE_NOTIFICATION_CHANNEL = "service_running";
    private Thread torThread;
    private Notification ntf;
    private ConnectionStateMonitor csm = new ConnectionStateMonitor();

    public MyService() {
    }

    protected void createAccount(){

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("inputExtra");
        if(ntf == null && torThread == null){
            ntf = ((DxApplication)getApplication()).sendNotification("Service running","click here to hide notification",SERVICE_NOTIFICATION_CHANNEL);
            startForeground(3,ntf);
            csm.enable(this.getApplication());
            startTor();
        }else if (command != null && command.contains("reconnect now")) {
            startTor(2);
        }
        //do heavy work on a background thread
        //stopSelf();

        return START_STICKY;
    }
    public void startTor(){
        torThread = ((DxApplication)getApplication()).startTor(1);
    }
    public void startTor(int force){
        if(force>0){
            torThread = ((DxApplication)getApplication()).startTor(2);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}