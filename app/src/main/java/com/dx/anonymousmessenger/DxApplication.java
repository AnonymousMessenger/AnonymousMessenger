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

import com.dx.anonymousmessenger.account.DxAccount;
import com.dx.anonymousmessenger.call.CallController;
import com.dx.anonymousmessenger.call.CallService;
import com.dx.anonymousmessenger.call.DxCallService;
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.receiver.NotificationHiderReceiver;
import com.dx.anonymousmessenger.service.MyService;
import com.dx.anonymousmessenger.tor.ServerSocketViaTor;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.dx.anonymousmessenger.ui.view.MainActivity;
import com.dx.anonymousmessenger.ui.view.call.CallActivity;

import net.sf.controller.network.AndroidTorRelay;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class DxApplication extends Application {

    private String hostname;
    public ServerSocketViaTor torSocket;
    private boolean serverReady = false;
    private boolean exitingHoldup;

    private DxAccount account;
    private Entity entity;

    private SQLiteDatabase database;
    private long time2delete = 86400000;
    private boolean weAsked = false;

    private CallController cc;

    private final List<QuotedUserMessage> messageQueue = new ArrayList<>();
    private volatile boolean syncing;
    private volatile String syncingAddress;
    private volatile boolean pinging;
    public List<String> onlineList = new ArrayList<>();

    private boolean sendingFile;
    public List<String> sendingTo = new ArrayList<>();

    /*
     * Tracking sending messages
     */
    public boolean isSendingFile(){
        return sendingFile;
    }

    public void setSendingFile(boolean b){
        sendingFile = b;
    }


    /*
     * Syncing related functions
     * */

    public void addToOnlineList(String address){
        if(!onlineList.contains(address)){
            onlineList.add(address);
            Intent gcm_rec = new Intent("your_action");
            gcm_rec.putExtra("type","online_status");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(gcm_rec);
        }
    }

    public void addToMessagesQueue(List<QuotedUserMessage> messages){
        for (QuotedUserMessage message:messages) {
            if(messageQueue.contains(message)){
                continue;
            }
            messageQueue.add(message);
        }
    }

//    public void addToMessageQueue(QuotedUserMessage message){
//        if(!messageQueue.contains(message)){
//            messageQueue.add(message);
//        }
//    }

    public void sendQueuedMessages(){
        for (QuotedUserMessage msg:messageQueue){
            System.out.println("sending a message");
            if(msg.getPath()!=null && !msg.getPath().equals("")){
                if(msg.getType().equals("file")){
                    MessageSender.sendQueuedFile(msg,this,msg.getTo());
                    continue;
                }
                MessageSender.sendQueuedMediaMessage(msg,this,msg.getTo());
                continue;
            }
            MessageSender.sendQueuedMessage(msg,this,msg.getTo());
        }
        messageQueue.clear();
    }

    public void queueAllUnsentMessages(){
        if(pinging || syncing){
            System.out.println("still pinging or syncing!");
            return;
        }
        try{
            pinging = true;
            List<String[]> contactsList = DbHelper.getContactsList(this);
            if(contactsList==null || contactsList.size()==0){
                pinging = false;
                return;
            }
            for (String[] contact: contactsList){
                boolean b = TorClientSocks4.testAddress(this, contact[1]);
                if(!b){
                    if(onlineList.contains(contact[1])){
                        onlineList.remove(contact[1]);
                        Intent gcm_rec = new Intent("your_action");
                        gcm_rec.putExtra("type","online_status");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(gcm_rec);
                    }
                    continue;
                }
                if(!onlineList.contains(contact[1])){
                    onlineList.add(contact[1]);
                    Intent gcm_rec = new Intent("your_action");
                    gcm_rec.putExtra("type","online_status");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(gcm_rec);
                }
                if(syncingAddress!=null && syncingAddress.equals(contact[1])){
                    continue;
                }
                syncing = true;
                syncingAddress = contact[1];
                List<QuotedUserMessage> undeliveredMessageList = DbHelper.getUndeliveredMessageList(this, contact[1]);
                if(undeliveredMessageList.size()==0){
                    continue;
                }
                addToMessagesQueue(undeliveredMessageList);
                sendQueuedMessages();
            }
            syncing = false;
            pinging = false;
            syncingAddress = null;
        }catch (Exception ignored) {syncing = false; pinging = false; syncingAddress = null;}
    }

    public void queueUnsentMessages(String address){
        if(syncingAddress!=null && syncingAddress.equals(address)){
            return;
        }
        new Thread(()->{
            while (syncing){
                try{
                    Thread.sleep(1000);
                }catch (Exception ignored) {}
            }
            try{
                syncing = true;
                syncingAddress = address;
                List<QuotedUserMessage> undeliveredMessageList = DbHelper.getUndeliveredMessageList(this, address);
                if(undeliveredMessageList.size()==0){
                    syncing = false;
                    return;
                }
//                boolean b = TorClientSocks4.testAddress(this, address);
//                if(!b){
//                    onlineList.remove(address);
//                    syncing = false;
//                    return;
//                }
//                if(!onlineList.contains(address)){
//                    onlineList.add(address);
//                }
                addToMessagesQueue(undeliveredMessageList);
                sendQueuedMessages();
                syncing = false;
                syncingAddress = null;
            }catch (Exception ignored) {syncing = false; syncingAddress = null;}
        }).start();
    }

    public boolean isExitingHoldup() {
        return exitingHoldup;
    }

    public void setExitingHoldup(boolean exitingHoldup) {
        this.exitingHoldup = exitingHoldup;
    }

    /*
     * Call related functions
     * */

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

    public Notification getCallInProgressNotification(Context context, String type, String address) {
        Intent contentIntent = new Intent(context, CallActivity.class);
        contentIntent.putExtra("address",address.substring(0,10));
        contentIntent.setAction(type);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, 0);

        Intent answerIntent = new Intent(context, DxCallService.class);
        answerIntent.putExtra("address",address.substring(0,10));
        answerIntent.setAction("answer");
        PendingIntent answerPendingIntent = PendingIntent.getService(context, 0, answerIntent, 0);

        Intent hangupIntent = new Intent(context, DxCallService.class);
        hangupIntent.putExtra("address",address.substring(0,10));
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

    public void commandCallService(String address, String type) {
        Intent serviceIntent = new Intent(this, DxCallService.class);
        serviceIntent.setAction(type);
        serviceIntent.putExtra("address", address.substring(0,10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

    /*
     * Account related functions
     * */

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

    public void createAccount(byte[] password, String nickname){
        new Thread(() -> {
            try{
                if(nickname==null || password==null){
                    throw new IllegalStateException();
                }

                this.sendNotification(getString(R.string.almost_ready),getString(R.string.starting_tor_first_time),false);

//                getDb(password);
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
                    if (isServiceRunningInForeground(this, MyService.class)) {
                        Intent serviceIntent = new Intent(this, MyService.class);
                        stopService(serviceIntent);
                    }
                    startTor();
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    /*
     * General functions
     * */

    public byte[] getSha256() throws NoSuchAlgorithmException {
        MessageDigest crypt = MessageDigest.getInstance("SHA-256");
        crypt.reset();
        crypt.update(getAccount().getPassword());
        return crypt.digest();
    }

    public SQLiteDatabase getDb(){
        if(this.database==null){
            SQLiteDatabase.loadLibs(this);
            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase("demo.db",
                    account.getPassword(),null);
            this.database = database;
            return database;
        }else{
            return this.database;
        }
    }

    public SQLiteDatabase getDb(byte[] password){
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
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, new String(password,StandardCharsets.UTF_8),null);
        this.database = database;
        return database;
    }

    public void setDb(SQLiteDatabase database){
        this.database = database;
    }

    public void enableStrictMode(){
        StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public long getTime2delete() {
        return time2delete;
    }

    public void setTime2delete(long time2delete) {
        this.time2delete = time2delete;
    }

    public void setWeAsked(boolean b){
        this.weAsked = b;
    }

    public boolean isWeAsked() {
        return weAsked;
    }

    public void clearMessageNotification(){
        clearNotification(1);
    }

    public void clearNotification(int id){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }

    public void sendNotification(String title, String msg){
        Intent resultIntent = new Intent(this, MainActivity.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //change this to not add to stack but delete it
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        String CHANNEL_ID = "messages";
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
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
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
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
        mNotificationManager.notify(2 , notification);
    }

    public void sendNotificationWithProgress(String title, String msg, int progress){
        String CHANNEL_ID = "status_messages";
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
//        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setProgress(100,progress,false)
//                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setProgress(100,progress,false)
//                    .setContentIntent(resultPendingIntent)
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
        mNotificationManager.notify(2 , notification);
    }

    public Notification getServiceNotification(String title, String msg, String CHANNEL_ID){
        Notification notification;
        Intent intent = new Intent(this, NotificationHiderReceiver.class);
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent gotoApp = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent hideNotification = PendingIntent.getBroadcast(
                this,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setColor(getResources().getColor(R.color.dx_night_940,getTheme()))
                    .setContentTitle(title)
                    .addAction(0, msg, hideNotification)
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
//                    .setContentText(msg)
                    .setContentIntent(gotoApp)
                    .setChannelId(CHANNEL_ID)
                    .build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title + ", " + msg)
                    .setContentText(msg)
                    .setContentIntent(hideNotification)
                    .build();
        }
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    CHANNEL_ID,NotificationManager.IMPORTANCE_LOW));
        }
        return notification;
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

    public void emptyVars() {
        if(database!=null){
            database.close();
        }
        if (torSocket != null){
            torSocket = null;
        }
        database = null;
        if(account!=null){
            account.setPassword(null);
            account.setNickname(null);
        }
        account = null;
        hostname = "";
        hostname = null;
        serverReady = false;
        weAsked = false;
        System.gc();
    }

    public void shutdown(int status){
        System.exit(status);
    }

    public void shutdown(){
        emptyVars();
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
        shutdown(0);
    }


    /*
     * tor related functions
     */

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public AndroidTorRelay getAndroidTorRelay(){
        if(torSocket==null){
            return null;
        }
        return torSocket.getAndroidTorRelay();
    }

    public ServerSocketViaTor getTorSocket() {
        return torSocket;
    }

    public void setTorSocket(ServerSocketViaTor torSocket) {
        this.torSocket = torSocket;
    }

    public boolean isServerReady() {
        System.out.println("isServerReady: "+serverReady);
        return serverReady;
    }

    public void setServerReady(boolean serverReady) {
        this.serverReady = serverReady;
//        if (!serverReady) {
//            return;
//        }
//        Intent gcm_rec = new Intent("tor_status");
//        gcm_rec.putExtra("tor_status","ALL GOOD");
//        LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
    }

    public void startTor(){
        System.out.println("start tor requested");
        if(isServiceRunningInForeground(this,MyService.class)){
            System.out.println("Service already running, not starting tor");
            return;
        }
        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("inputExtra", "inputExtra");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

    public void restartTor(){
        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("inputExtra", "inputExtra");
//        serviceIntent.putExtra("inputExtra", "reconnect now");
        stopService(serviceIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

}
