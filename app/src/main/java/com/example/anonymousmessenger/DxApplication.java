package com.example.anonymousmessenger;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;

import com.example.anonymousmessenger.tor.ServerSocketViaTor;

import java.io.IOException;

public class DxApplication extends Application {

    private ServerSocketViaTor torSocket;
    private String hostname;
    private int rport;
    private int lport;
    private boolean serverReady = false;
    private DxAccount account;
    private Thread torThread;

    public DxAccount getAccount() {
        return account;
    }

    public void setAccount(DxAccount account) {
        this.account = account;
    }

    public ServerSocketViaTor getTorSocket() {
        return torSocket;
    }

    public void enableStrictMode(){
        StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }
    public void setTorSocket(ServerSocketViaTor torSocket) {
        this.torSocket = torSocket;
    }

    public int getRport() {
        return rport;
    }

    public void setRport(int rport) {
        this.rport = rport;
    }

    public int getLport() {
        return lport;
    }

    public void setLport(int lport) {
        this.lport = lport;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public boolean isServerReady() {
        return serverReady;
    }

    public void setServerReady(boolean serverReady) {
        this.serverReady = serverReady;
    }

    public void sendNotification(String title, String msg){
        String CHANNEL_ID = "somechannel";
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .build();
        }
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,NotificationManager.IMPORTANCE_HIGH));
        }
        // Issue the notification.
        mNotificationManager.notify(1 , notification);
    }

    public Thread getTorThread() {
        return torThread;
    }

    public void setTorThread(Thread torThread) {
        this.torThread = torThread;
    }

    public boolean getServerReady() {
        return serverReady;
    }

    public void startTor(){
        enableStrictMode();
        Thread torThread = new Thread(() -> {
            try {
                setTorSocket(new ServerSocketViaTor(getApplicationContext()));
                ServerSocketViaTor ssvt = getTorSocket();
                ssvt.init(this);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        torThread.start();
        setTorThread(torThread);
    }
}
