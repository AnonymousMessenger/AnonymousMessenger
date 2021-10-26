package com.dx.anonymousmessenger.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.tor.ServerSocketViaTor;

public class DxService extends Service {
    public final static String SERVICE_NOTIFICATION_CHANNEL = "service_running";
    private DxApplication app;
    private Thread torThread;
    private BroadcastReceiver mMyBroadcastReceiver;

    public DxService() {
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
        startTor();
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
            if(intent.getIntExtra("start_syncing",0)>0){
                new Thread(()-> app.queueAllUnsentMessages()).start();
            }
            }
        };
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMyBroadcastReceiver,new IntentFilter("dx_service"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String command = intent.getStringExtra("inputExtra");
        if (command != null && command.contains("reconnect now")) {
            startTor();
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
            mMyBroadcastReceiver = null;
//          csm.disable();
            if(app.getTorSocket()!=null&&app.getTorSocket().getOnionProxyManager()!=null){
                try {
                    app.getTorSocket().tryKill();
                    if(torThread != null){
                        torThread.interrupt();
                        torThread = null;
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
//            stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SHUTDOWN ERROR","SHUTDOWN ERROR");
        }
    }

    private void startTor(){
        if(torThread!=null){
            if(app.getTorSocket()!=null&&app.getTorSocket().getOnionProxyManager()!=null){
                try {
                    app.getTorSocket().tryKill();
                    if(torThread != null){
                        torThread.interrupt();
                        torThread = null;
                    }
//                    Thread.sleep(1000);
                    startTor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        app.enableStrictMode();
        Thread torThread = new Thread(() -> {
            app.setTorSocket(new ServerSocketViaTor());
            app.getTorSocket().init(app);
        });
        torThread.start();
        setTorThread(torThread);
    }

    private void setTorThread(Thread torThread) {
        this.torThread = torThread;
    }

}