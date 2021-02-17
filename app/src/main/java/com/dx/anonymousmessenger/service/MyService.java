package com.dx.anonymousmessenger.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.tor.ServerSocketViaTor;

import java.io.IOException;

public class MyService extends Service {
    public final static String SERVICE_NOTIFICATION_CHANNEL = "service_running";
    private DxApplication app;
    private Thread torThread;

    public MyService() {
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
        startTor(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("inputExtra");
        if (command != null && command.contains("reconnect now")) {
            startTor(2);
        }else if (command != null && command.contains("shutdown now")) {
            shutdownFromBackground();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        shutdownFromBackground();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void shutdownFromBackground() {
        try {
//          csm.disable();
            if(app.getAndroidTorRelay()!=null){
                app.getAndroidTorRelay().shutDown();
            }
//            stopSelf();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SHUTDOWN ERROR","ya");
        }
    }

    private void startTor(int num){
        if(num==2&&torThread!=null){
            if(app.torSocket!=null&&app.torSocket.getAndroidTorRelay()!=null){
                new Thread(()->{
                    try {
                        app.torSocket.getAndroidTorRelay().shutDown();
                        app.torSocket.tryKill();
                        Thread.sleep(1000);
                        startTor(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            return;
        }
        app.enableStrictMode();
        Thread torThread = new Thread(() -> {
            try {
                app.setTorSocket(new ServerSocketViaTor());
                app.getTorSocket().init(app);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        torThread.start();
        setTorThread(torThread);
    }

    private void setTorThread(Thread torThread) {
        this.torThread = torThread;
    }

}