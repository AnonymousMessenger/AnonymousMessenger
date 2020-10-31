package com.dx.anonymousmessenger;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class MyService extends Service {
    public final static String SERVICE_NOTIFICATION_CHANNEL = "service_running";
//    private final ConnectionStateMonitor csm = new ConnectionStateMonitor();
    private DxApplication app;

    public MyService() {
    }

    protected void createAccount(){

    }

    @Override
    public void onCreate() {
        app = (DxApplication) getApplication();
        if(app.getAccount()==null||app.getAccount().getPassword()==null){
            super.onCreate();
            return;
        }
        super.onCreate();
        Notification ntf = app.getServiceNotification(getString(R.string.still_background), getString(R.string.click_to_hide), SERVICE_NOTIFICATION_CHANNEL);
        startForeground(3, ntf);
//        csm.enable(this.getApplication());
        app.startTor(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("inputExtra");
        if (command != null && command.contains("reconnect now")) {
            app.startTor(2);
        }else if (command != null && command.contains("shutdown now")) {
            shutdownFromBackground();
        }
        return START_NOT_STICKY;
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
        try {
//          csm.disable();
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
    }
}