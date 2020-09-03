package com.dx.anonymousmessenger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.StrictMode;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.call.CallController;
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.tor.ServerSocketViaTor;

import net.sf.controller.network.AndroidTorRelay;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.IOException;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class DxApplication extends Application {

    private ServerSocketViaTor torSocket;
    private String hostname;
    private DxAccount account;
    private Thread torThread;
    private SQLiteDatabase database;
    private long time2delete = 86400000;
    private boolean serverReady = false;
    private boolean lockTorStart = false;
    private boolean weAsked = false;
    private Entity entity;
    private CallController cc;

    public boolean isInCall(){
        return cc!=null;
    }

    public boolean isInActiveCall(){
        return cc.isAnswered();
    }

    public CallController getCc() {
        return cc;
    }

    public void setCc(CallController cc) {
        this.cc = cc;
    }

    public DxAccount getAccount() {
        return account;
    }

    public void setAccount(DxAccount account) {
        this.account = account;
        if(account!=null){
            DxAccount.saveAccount(account,this);
        }
    }

    public void setAccount(DxAccount account, boolean b) {
        this.account = account;
        if(account!=null && b){
            DxAccount.saveAccount(account,this);
        }
    }

    public void createAccount(String password, String nickname){
        new Thread(() -> {
            try{
                if(nickname==null || password==null){
                    throw new IllegalStateException();
                }

                this.sendNotification("Almost Ready!","Starting tor and warming up to get all we need to connect!",false);

                DxAccount account;
                if(this.getAccount()==null){
                    account = new DxAccount();
                }else{
                    account = this.getAccount();
                }
                account.setNickname(nickname);
                account.setPassword(password);

                this.setAccount(account);

                if (!isServerReady()) {
                    if (getTorThread() != null) {
                        getTorThread().interrupt();
                        setTorThread(null);
                    }
                    startTor();
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();
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
        if(serverReady){
            this.lockTorStart = false;
            Intent gcm_rec = new Intent("tor_status");
            gcm_rec.putExtra("tor_status","ALL GOOD");
            LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
        }
    }

    public void sendNotification(String title, String msg){
        Intent resultIntent = new Intent(this, MainActivity.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //todo change this to not add to stack but delete it
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        String CHANNEL_ID = "messages";
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
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

    public void sendNotification(String title, String msg, boolean isMessage){
        if(isMessage){
            sendNotification(title,msg);
            return;
        }
        String CHANNEL_ID = "status_messages";
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .build();
        }
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,NotificationManager.IMPORTANCE_LOW));
        }
        // Issue the notification.
        mNotificationManager.notify(2 , notification);
    }

    public Notification getCallInProgressNotification(Context context, String type, String address) {
        Intent contentIntent = new Intent(context, CallActivity.class);
        contentIntent.putExtra("address",address);
        contentIntent.setAction(type);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);

        Intent answerIntent = new Intent(context, DxCallService.class);
        answerIntent.putExtra("address",address);
        answerIntent.setAction("answer");
        PendingIntent answerPendingIntent = PendingIntent.getService(context, 0, answerIntent, 0);

        Intent hangupIntent = new Intent(context, DxCallService.class);
        hangupIntent.putExtra("address",address);
        hangupIntent.setAction("hangup");
        PendingIntent hangupPendingIntent = PendingIntent.getService(context, 0, hangupIntent, 0);

        String CHANNEL_ID = "calls";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_call_24)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setContentTitle(address);

        try {
            builder.setFullScreenIntent(pendingIntent, true);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setCategory(NotificationCompat.CATEGORY_CALL);
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }

        Intent gcm_rec = new Intent("call_action");

        switch (type) {
            case CallService.ACTION_START_OUTGOING_CALL_RESPONSE:
                gcm_rec.putExtra("action",CallService.ACTION_START_OUTGOING_CALL_RESPONSE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.ringing));
                builder.setPriority(NotificationCompat.PRIORITY_MIN);
                builder.addAction(R.drawable.ic_baseline_call_24, "Hangup", hangupPendingIntent);
                break;
            case CallService.ACTION_START_INCOMING_CALL:
                gcm_rec.putExtra("action",CallService.ACTION_START_INCOMING_CALL);
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.NotificationBarManager__incoming_call));
                builder.addAction(R.drawable.ic_baseline_call_24, "Answer", answerPendingIntent);
                builder.addAction(R.drawable.ic_baseline_call_24, "Hangup", hangupPendingIntent);
                break;
            case CallService.ACTION_START_OUTGOING_CALL:
                gcm_rec.putExtra("action",CallService.ACTION_START_OUTGOING_CALL);
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.connecting));
                builder.addAction(R.drawable.ic_baseline_call_24, "Hangup", hangupPendingIntent);
                break;
            default:
                gcm_rec.putExtra("action",context.getString(R.string.NotificationBarManager_call_in_progress));
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.NotificationBarManager_call_in_progress));
                builder.addAction(R.drawable.ic_baseline_call_24, "Hangup", hangupPendingIntent);
                break;
        }

        Notification notification = builder.build();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,NotificationManager.IMPORTANCE_HIGH));
        }
        return notification;
    }

    public Notification getServiceNotification(String title, String msg, String CHANNEL_ID){
        Notification notification;
        Intent intent = new Intent(this, NotificationHiderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(pendingIntent)
                    .setChannelId(CHANNEL_ID)
                    .build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.notification)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(pendingIntent)
                    .build();
        }
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,NotificationManager.IMPORTANCE_NONE));
        }
        return notification;
    }

//    public Notification getCallNotification(String title, String msg, String CHANNEL_ID){
//        Notification notification;
//        Intent intent = new Intent(this, CallActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                this,
//                1,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT
//        );
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notification = new Notification.Builder(this,CHANNEL_ID)
//                    .setSmallIcon(R.drawable.notification)
//                    .setContentTitle(title)
//                    .setContentText(msg)
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .setChannelId(CHANNEL_ID).build();
//        }else{
//            notification = new Notification.Builder(this)
//                    .setSmallIcon(R.drawable.notification)
//                    .setContentTitle(title)
//                    .setContentText(msg)
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .build();
//        }
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
//                    CHANNEL_ID,NotificationManager.IMPORTANCE_NONE));
//        }
////        // Issue the notification.
////        mNotificationManager.notify(1 , notification);
//        return notification;
//    }

    public AndroidTorRelay getAndroidTorRelay(){
        if(torSocket==null){
            return null;
        }
        return torSocket.getAndroidTorRelay();
    }

    public Thread getTorThread() {
        return torThread;
    }

    public void setTorThread(Thread torThread) {
        this.torThread = torThread;
    }

    public void startTor(){
        System.out.println("start tor requested");
        if(isServiceRunningInForeground(this,MyService.class)){
            System.out.println("Service already running, not starting tor");
            return;
        }
//        if(lockTorStart){
//            System.out.println("tor locked, not starting tor");
//            return;
//        }
//        lockTorStart();
        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("inputExtra", "wtf bitch");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

    public void restartTor(){
        if(lockTorStart){
            return;
        }
        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("inputExtra", "reconnect now");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

    public void startTor(int num){
        if(num==2&&torThread!=null){
            if(torSocket!=null&&torSocket.getAndroidTorRelay()!=null){
                new Thread(()->{
                    try {
                        torSocket.getAndroidTorRelay().shutDown();
                        torSocket.tryKill();
                        Thread.sleep(1000);
                        startTor(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            return;
        }
        lockTorStart();
        enableStrictMode();
        Thread torThread = new Thread(() -> {
            try {
                setTorSocket(new ServerSocketViaTor(getApplicationContext()));
                getTorSocket().init(this);
                lockTorStart = false;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        torThread.start();
        setTorThread(torThread);
//        System.out.println("beautiful day");
//        if(lockTorStart){
//            return;
//        }
//        System.out.println("locking it");
//        lockTorStart();
//        if(num==2&&torThread!=null){
//            Log.e("RESTART","doing it");
//            if (getTorThread() != null) {
//                getTorThread().interrupt();
//                setTorThread(null);
//            }
//            lockTorStart = false;
//            startTor(1);
//            return;
//            if(torSocket!=null&&torSocket.getAndroidTorRelay()!=null){
//                new Thread(()->{
//                    try {
//                        torSocket.getAndroidTorRelay().shutDown();
//                        torSocket.getServerThread().interrupt();
//                        torThread.interrupt();
//                        torThread = null;
//                        torSocket.setServerThread(null);
//                        torSocket.setAndroidTorRelay(null);
//                        torSocket = null;
//                        Thread.sleep(1000);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }).start();
//            }
//            return null;
//        }
//        Log.e("AGAIN","doing it");
//        enableStrictMode();
//        Thread torThread = new Thread(() -> {
//            try {
//                setTorSocket(new ServerSocketViaTor(getApplicationContext()));
//                ServerSocketViaTor ssvt = getTorSocket();
//                ssvt.init(this);
//            } catch (InterruptedException | IOException e) {
//                e.printStackTrace();
//            }
//        });
//        torThread.start();
//        setTorThread(torThread);
//        lockTorStart = false;
//        return;
    }

    public SQLiteDatabase getDb(){
        //Log.e("SOMEONE ASKED FOR DB","YES INDEED");
        if(this.database==null){
            SQLiteDatabase.loadLibs(this);
            File databaseFile = new File(getFilesDir(), "demo.db");
            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile,
                    account.getPassword(),
                    null);
            this.database = database;
            return database;
        }else{
            return this.database;
        }
    }

    public SQLiteDatabase getDb(String password){
        if(database!=null){
            database.close();
            database = null;
        }
        SQLiteDatabase.loadLibs(this);
        File databaseFile = new File(getFilesDir(), "demo.db");
        if(!databaseFile.exists()){
            databaseFile.mkdirs();
            databaseFile.delete();
        }
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile,password,null);
        this.database = database;
        return database;
    }

    public void setDb(SQLiteDatabase database){
        this.database = database;
    }

    @SuppressLint("BatteryLife")
    public void requestBatteryOptimizationOff(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getApplicationContext().getPackageName();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + packageName));
                getApplicationContext().startActivity(intent);
                this.weAsked = true;
            }
        }
    }

    public boolean isIgnoringBatteryOptimizations(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getApplicationContext().getPackageName();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return false;
    }

    public boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getTime2delete() {
        return time2delete;
    }

    public void setTime2delete(long time2delete) {
        this.time2delete = time2delete;
    }

    public void lockTorStart() {
        this.lockTorStart = true;
    }

    public boolean isTorStartLocked(){
        return this.lockTorStart;
    }

    public void setWeAsked(boolean b){
        this.weAsked = b;
    }

    public boolean isWeAsked() {
        return weAsked;
    }

    public void emptyVars() {
        if(database!=null){
            database.close();
        }
        database = null;
        if(account!=null){
            account.setPassword("");
            account.setPassword(null);
            account.setNickname(null);
        }
        account = null;
        hostname = "";
        hostname = null;
        serverReady = false;
        lockTorStart = false;
        weAsked = false;
        torSocket = null;
        if(torThread!=null && torThread.isAlive()){
            torThread.interrupt();
        }
        torThread = null;
        System.gc();
    }

    public void shutdown(int status){
        System.exit(status);
    }

    public void shutdown(){
        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("inputExtra", "shutdown now");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void commandCallService(String address, String type) {
        Intent serviceIntent = new Intent(this, DxCallService.class);
        serviceIntent.setAction(type);
        serviceIntent.putExtra("address", address);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }
}
