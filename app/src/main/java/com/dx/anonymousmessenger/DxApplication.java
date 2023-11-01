package com.dx.anonymousmessenger;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.dx.anonymousmessenger.messages.MessageSender.sendMediaMessageWithoutSaving;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.account.DxAccount;
import com.dx.anonymousmessenger.call.CallController;
import com.dx.anonymousmessenger.call.DxCallService;
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.receiver.NotificationHiderReceiver;
import com.dx.anonymousmessenger.service.DxService;
import com.dx.anonymousmessenger.tor.ServerSocketViaTor;
import com.dx.anonymousmessenger.tor.TorClient;
import com.dx.anonymousmessenger.ui.view.MainActivity;
import com.dx.anonymousmessenger.ui.view.call.CallActivity;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.msopentech.thali.java.toronionproxy.FileUtilities;
import net.sf.msopentech.thali.java.toronionproxy.OnionProxyManager;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class DxApplication extends Application {

    private String hostname;
    private ServerSocketViaTor torSocket;
    private boolean serverReady = false;
    private boolean exitingHoldup;
    private volatile boolean restartingTor;
    private long torStartTime = 0;

    private DxAccount account;
    private Entity entity;

    private SQLiteDatabase database;
    private long time2delete = 86400000;
    private boolean weAsked = false;

    private CallController cc;

    private final List<QuotedUserMessage> messageQueue = new ArrayList<>();
//    private volatile boolean syncing;
    private volatile String syncingAddress;
//    private volatile boolean pinging;
    public final List<String> onlineList = new ArrayList<>();

    private boolean sendingFile;
    public final List<String> sendingTo = new ArrayList<>();

    //settings
    //settings array: bridgesEnabled,acceptUnknown,acceptCalls,receiveFiles,testAddress,fileSizeLimit,proxyAddress,proxyUsername,proxyPassword,excludeText,excludeUnknown,strictExclude
    public final Object[] DEFAULT_SETTINGS = {0,0,1,1,"duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion","3gb",0,"","","","",0,0};
    private boolean bridgesEnabled;
    private boolean isAcceptingUnknownContactsEnabled;
    private boolean isAcceptingCallsAllowed;
    private boolean isReceivingFilesAllowed;
    private long fileSizeLimit;
    private boolean enableSocks5Proxy;
    private String socks5AddressAndPort;
    private String socks5Username;
    private String socks5Password;
    private String excludeText;
    private boolean excludeUnknown;
    private boolean strictExclude;
    //the ddg site to check if we are online (can be overridden in the settings)
    private String testAddress = DEFAULT_SETTINGS[4].toString();

    private final Object mainQueueLock = new Object();
    private final Object peerQueueLock = new Object();

    public String getTestAddress() {
        return testAddress;
    }

    public String getExcludeText() {
        return excludeText;
    }

    public boolean isExcludeUnknown() {
        return excludeUnknown;
    }

    public boolean isStrictExclude() {
        return strictExclude;
    }

    public boolean isEnableSocks5Proxy() {
        return enableSocks5Proxy;
    }

    public String getSocks5AddressAndPort() {
        return socks5AddressAndPort;
    }

    public String getSocks5Username() {
        return socks5Username;
    }

    public String getSocks5Password() {
        return socks5Password;
    }

    public boolean isBridgesEnabled() {
        return bridgesEnabled;
    }

    public boolean isAcceptingUnknownContactsEnabled() {
        return isAcceptingUnknownContactsEnabled;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isAcceptingCallsAllowed() {
        return isAcceptingCallsAllowed;
    }

    public boolean isReceivingFilesAllowed() {
        return isReceivingFilesAllowed;
    }

    public long getFileSizeLimit() {
        return fileSizeLimit;
    }

    public long getTorStartTime(){
        return torStartTime;
    }

    public void resetTorStartTime(){
        torStartTime = 0;
    }

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

    public synchronized void addToMessagesQueue(List<QuotedUserMessage> messages){
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

    public synchronized void sendQueuedMessages(){
        for (QuotedUserMessage msg:messageQueue){
//            Log.d("ANONYMOUSMESSENGER","sending a message");
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

    public synchronized void sendQueuedMessages(List<QuotedUserMessage> messages){
        for (QuotedUserMessage msg:messages){
//            Log.d("ANONYMOUSMESSENGER","sending a message");
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
    }

    public void queueAllUnsentMessages(){
        synchronized (mainQueueLock) {
//        if(pinging /*|| syncing*/){
//            Log.d("ANONYMOUSMESSENGER","still pinging or syncing!");
//            return;
//        }
            try {
//            pinging = true;
                List<String[]> contactsList = DbHelper.getContactsList(this);
                if (contactsList == null || contactsList.size() == 0) {
//                pinging = false;
                    return;
                }
                for (String[] contact : contactsList) {
                    boolean b = TorClient.testAddress(this, contact[1]);
                    if (!b) {
                        if (onlineList.contains(contact[1])) {
                            onlineList.remove(contact[1]);
                            Intent gcm_rec = new Intent("your_action");
                            gcm_rec.putExtra("type", "online_status");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(gcm_rec);
                        }
                        continue;
                    }
                    if (!onlineList.contains(contact[1])) {
                        onlineList.add(contact[1]);
                        Intent gcm_rec = new Intent("your_action");
                        gcm_rec.putExtra("type", "online_status");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(gcm_rec);
                    }
                    queueUnsentMessages(contact[1]);
//                if(syncingAddress!=null && syncingAddress.equals(contact[1])){
//                    continue;
//                }
////                syncing = true;
//                syncingAddress = contact[1];
//                List<QuotedUserMessage> undeliveredMessageList = DbHelper.getUndeliveredMessageList(this, contact[1]);
//                if(undeliveredMessageList.size()==0){
//                    continue;
//                }
//                addToMessagesQueue(undeliveredMessageList);
//                sendQueuedMessages();
                }
//            syncing = false;
//            pinging = false;
                syncingAddress = null;
            } catch (Exception ignored) {/*syncing = false;*/ /*pinging = false; syncingAddress = null;*/}
        }
    }

    public void queueUnsentMessages(String address){
        if(syncingAddress!=null && syncingAddress.equals(address)){
//            System.out.println("Already Syncing with: "+syncingAddress);
            return;
        }
//        System.out.println("Starting Sync.............");
        new Thread(()-> doQueueUnsentMessages(address)).start();
    }

    public void doQueueUnsentMessages(String address){
//        System.out.println("Waiting sync start : "+address);
        synchronized (peerQueueLock) {
//            System.out.println("sync start: "+address);
            try {
//                syncing = true;
                syncingAddress = address;
                //send profile image if not delivered from before
                String path = getAccount().getProfileImagePath();
                if (path != null && !path.equals("")) {
                    if (!Objects.equals(DbHelper.getContactSentProfileImagePath(address, this), path)) {
                        QuotedUserMessage qum = new QuotedUserMessage("", "", getHostname(), "",
                                getAccount().getNickname(), new Date().getTime(), false, address, false, "profile_image", path, "image");
                        sendMediaMessageWithoutSaving(qum, this, address, false, true);
                    }
                }
//                System.out.println("getting undelivered list.........");
                List<QuotedUserMessage> undeliveredMessageList = DbHelper.getUndeliveredMessageList(this, address);
                if (undeliveredMessageList.size() == 0) {
//                    System.out.println("LIST SIZE WAS ZERO 00000000");
//                    syncing = false;
                    syncingAddress = null;
                    return;
                }
//                System.out.println("LIST SIZE : "+undeliveredMessageList.size());
                //double sending occurs because there is a queue pool, we need to stop using it
                // the queue pool should be cleared if the message is already there
                //double is because there are two syncing functions (all & by address)
                //also bad because we clear the queue in separate functions and threads
                //maybe make sendQueuedMessages synchronized?
//            addToMessagesQueue(undeliveredMessageList);
//                System.out.println("Sending.........");
                sendQueuedMessages(undeliveredMessageList);
//                syncing = false;
                syncingAddress = null;
            } catch (Exception ignored) {/*syncing = false;*/
                syncingAddress = null;
            }
        }
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
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            pendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_MUTABLE);
        }else
        {
             pendingIntent = PendingIntent.getActivity
                    (context, 0, contentIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        }


        Intent answerIntent = new Intent(context, DxCallService.class);
        answerIntent.putExtra("address",address.substring(0,10));
        answerIntent.setAction("answer");
        PendingIntent answerPendingIntent;
        if (Build.VERSION.SDK_INT >= 31) {
            answerPendingIntent = PendingIntent.getService(context, 0, answerIntent, PendingIntent.FLAG_MUTABLE);
        }else{
            answerPendingIntent = PendingIntent.getService(context, 0, answerIntent, PendingIntent.FLAG_IMMUTABLE);
        }

        Intent hangupIntent = new Intent(context, DxCallService.class);
        hangupIntent.putExtra("address",address.substring(0,10));
        hangupIntent.setAction("hangup");
        PendingIntent hangupPendingIntent;
        if (Build.VERSION.SDK_INT >= 31) {
            hangupPendingIntent = PendingIntent.getService(context, 0, hangupIntent, PendingIntent.FLAG_MUTABLE);
        }else{
            hangupPendingIntent = PendingIntent.getService(context, 0, hangupIntent, PendingIntent.FLAG_IMMUTABLE);
        }

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
            case DxCallService.ACTION_START_OUTGOING_CALL_RESPONSE:
                gcm_rec.putExtra("action",DxCallService.ACTION_START_OUTGOING_CALL_RESPONSE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.ringing));
                builder.setPriority(NotificationCompat.PRIORITY_MIN);
                builder.addAction(R.drawable.ic_baseline_call_24, getString(R.string.hangup), hangupPendingIntent);
                break;
            case DxCallService.ACTION_START_INCOMING_CALL:
                gcm_rec.putExtra("action",DxCallService.ACTION_START_INCOMING_CALL);
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.NotificationBarManager__incoming_call));
                builder.addAction(R.drawable.ic_baseline_call_24, getString(R.string.answer), answerPendingIntent);
                builder.addAction(R.drawable.ic_baseline_call_24, getString(R.string.hangup), hangupPendingIntent);
                break;
            case DxCallService.ACTION_START_OUTGOING_CALL:
                gcm_rec.putExtra("action",DxCallService.ACTION_START_OUTGOING_CALL);
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.connecting));
                builder.addAction(R.drawable.ic_baseline_call_24, getString(R.string.hangup), hangupPendingIntent);
                break;
            default:
                gcm_rec.putExtra("action",context.getString(R.string.NotificationBarManager_call_in_progress));
                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                builder.setContentText(context.getString(R.string.NotificationBarManager_call_in_progress));
                builder.addAction(R.drawable.ic_baseline_call_24, getString(R.string.hangup), hangupPendingIntent);
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

    public void setAccount(DxAccount account, boolean saveToDb) {
        this.account = account;
        if(account!=null && saveToDb){
            DxAccount.saveAccount(account,this);
        }
    }

    public void createAccount(byte[] password, String nickname, boolean bridgesEnabled, List<String> bridgeList, boolean isAcceptingUnknownContactsEnabled, boolean isAcceptingCallsAllowed,boolean isReceivingFilesAllowed, String checkAddress, String fileSizeLimit, boolean enableSocks5Proxy, String socks5AddressAndPort, String socks5Username, String socks5Password, String excludeText, boolean excludeUnknown, boolean strictExclude){
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

                //set and save the account to DB
                this.setAccount(account);

                //save the bridge list to DB
                for(int i=0; i<bridgeList.size(); i++){
                    DbHelper.saveBridge(bridgeList.get(i),this);
                }

                //save the settings to DB
                DbHelper.saveSettings(bridgesEnabled, isAcceptingUnknownContactsEnabled, isAcceptingCallsAllowed, isReceivingFilesAllowed,checkAddress,fileSizeLimit,enableSocks5Proxy,socks5AddressAndPort,socks5Username, socks5Password,excludeText,excludeUnknown,strictExclude,this);

                startTor();

            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public void reloadSettings(){
        Log.d("ANONYMOUSMESSENGER", String.valueOf(isAcceptingCallsAllowed));
        try{
            Object[] settings = DbHelper.getSettingsList(this);
            if(settings == null){
                Log.d("ANONYMOUSMESSENGER","settings were null when got from the db");
                settings = DEFAULT_SETTINGS;
                DbHelper.saveSettings((int)settings[0]>0,(int)settings[1]>0,(int)settings[2]>0,(int)settings[3]>0,(String)settings[4],(String)settings[5],(int)settings[6]>0, (String) settings[7], (String) settings[8], (String) settings[9], (String) settings[10], (int)settings[11]>0, (int)settings[12]>0,this);
            }
            this.bridgesEnabled = (int)settings[0]>0;
            this.isAcceptingUnknownContactsEnabled = (int)settings[1]>0;
            this.isAcceptingCallsAllowed = (int)settings[2]>0;
            this.isReceivingFilesAllowed = (int)settings[3]>0;
            this.testAddress = (String) settings[4];
            this.fileSizeLimit = Utils.parseFileSize((String) settings[5]);
            this.enableSocks5Proxy = (int)settings[6]>0;
            this.socks5AddressAndPort = (String) settings[7];
            this.socks5Username = (String) settings[8];
            this.socks5Password = (String) settings[9];
            this.excludeText = (String) settings[10];
            this.excludeUnknown = (int)settings[11]>0;
            this.strictExclude = (int)settings[12]>0;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public String getMyAddressOffline(){
        try{
            return new String(FileUtilities.read(new File(this.getDir("torfiles", MODE_PRIVATE),"/hiddenservice/hostname")), StandardCharsets.UTF_8).trim();
        }catch (Exception e){
            e.printStackTrace();
            return "Couldn't read hostname file";
        }
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
        if(this.database==null && getAccount()!=null){
            return getDb(getAccount().getPassword());
        }else{
            return this.database;
        }
    }

    public SQLiteDatabase getDbOrNull(){
        return this.database;
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
        Intent intent = new Intent(this, NotificationHiderReceiver.class);
        Intent resultIntent = new Intent(this, MainActivity.class);
        String packageName = getPackageName();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
        ComponentName componentName = new ComponentName(packageName,
                Objects.requireNonNull(alias));
        resultIntent.setComponent(componentName);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent;
        if (Build.VERSION.SDK_INT >= 31) {
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_MUTABLE);
        }else{
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String CHANNEL_ID = "messages";
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setSubText(msg)
//                    .setContentText(msg)
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
        sendNotification(title, msg, isMessage, R.mipmap.ic_launcher_foreground);
    }

    public void sendNotification(String title, String msg, boolean isMessage, int icon){
        if(isMessage){
            sendNotification(title,msg);
            return;
        }
        String CHANNEL_ID = "status_messages";
        Intent intent = new Intent(this, NotificationHiderReceiver.class);
        Intent resultIntent = new Intent(this, MainActivity.class);
        String packageName = getPackageName();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
        ComponentName componentName = new ComponentName(packageName,
                Objects.requireNonNull(alias));
        resultIntent.setComponent(componentName);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent;
        if (Build.VERSION.SDK_INT >= 31) {
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_MUTABLE);
        }else{
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setChannelId(CHANNEL_ID).build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(icon)
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
            notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setSubText(msg)
//                    .setContentText(msg)
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
        String packageName = getPackageName();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
        ComponentName componentName = new ComponentName(packageName,
                Objects.requireNonNull(alias));
        resultIntent.setComponent(componentName);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent gotoApp;
        if (Build.VERSION.SDK_INT >= 31) {
            gotoApp = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_MUTABLE);
        }else{
            gotoApp = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        PendingIntent hideNotification = null;
        if (Build.VERSION.SDK_INT >= 31) {
            hideNotification = PendingIntent.getBroadcast(this,1, intent, PendingIntent.FLAG_MUTABLE);
        }else{
            hideNotification = PendingIntent.getBroadcast(this,1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setColor(getResources().getColor(R.color.dx_night_940,getTheme()))
                    .setSubText(title)
//                    .addAction(R.drawable.ic_baseline_settings_24, msg, hideNotification)
                    .addAction(R.drawable.ic_baseline_settings_24, getString(R.string.go_to_app), gotoApp)
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
//                    .setContentText(msg)
//                    .setContentIntent(gotoApp)
                    .setChannelId(CHANNEL_ID)
                    .build();
        }else{
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(title)
//                    .setContentText(msg)
                    .setContentIntent(gotoApp)
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

    public void updateServiceNotification(){
        Notification ntf = getServiceNotification(getString(R.string.still_background), getString(R.string.click_to_hide), DxService.SERVICE_NOTIFICATION_CHANNEL);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(3,ntf);
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

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
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
        Intent serviceIntent = new Intent(this, DxService.class);
        stopService(serviceIntent);
        shutdown(0);
    }

    /*
     * tor related functions
     */

    public String getHostname() {
        if(hostname==null){
            this.hostname = getMyAddressOffline();
        }
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public OnionProxyManager getAndroidTorRelay(){
        if(torSocket==null){
            return null;
        }
        return torSocket.getOnionProxyManager();
    }

    public ServerSocketViaTor getTorSocket() {
        return torSocket;
    }

    public void setTorSocket(ServerSocketViaTor torSocket) {
        this.torSocket = torSocket;
    }

    public boolean isServerReady() {
        Log.d("ANONYMOUSMESSENGER","isServerReady: "+serverReady);
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
        Log.d("ANONYMOUSMESSENGER","start tor requested");
        torStartTime = new Date().getTime();
        reloadSettings();
        if(isServiceRunningInForeground(this, DxService.class)){
            Log.d("ANONYMOUSMESSENGER","Service already running, not starting tor");
            return;
        }
        Intent serviceIntent = new Intent(this, DxService.class);
        serviceIntent.putExtra("inputExtra", "inputExtra");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }else{
            startService(serviceIntent);
        }
    }

    public void restartTor(){
        if(restartingTor){
            Log.d("ANONYMOUSMESSENGER","still restarting, abort");
            return;
        }
        Log.d("ANONYMOUSMESSENGER","starting restart now");
        restartingTor = true;
        torStartTime = 0;
        new Thread(()->{
        if(torSocket!=null){
            try {
                Log.d("ANONYMOUSMESSENGER","starting trykill");
                torSocket.tryKill();
                Log.d("ANONYMOUSMESSENGER","trykill ok");
                Thread.sleep(1500);
            } catch (Exception ignored) {}
            if(serverReady){
                serverReady = false;
            }
        }

        try{
            Log.d("ANONYMOUSMESSENGER","stopping service");
            Intent serviceIntent = new Intent(this, DxService.class);
            stopService(serviceIntent);
            Log.d("ANONYMOUSMESSENGER","service stopped");

            try {
                Thread.sleep(3500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.d("ANONYMOUSMESSENGER","restarting service");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }catch (Exception ignored){}

        restartingTor = false;
        }).start();
    }

    public void deleteAnyOldFiles() {
        List<String> paths = DbHelper.getAllReferencedFiles(this);
        File curDir = getFilesDir();
        File[] filesList = curDir.listFiles();
        // it is safe to delete hw_cached_resid.list on Huawei devices
        if (filesList != null) {
            for(File f : filesList){
                if(f.isFile()){
                    if(!paths.contains(f.getName()) && !f.getName().contains("demo")){
                        f.delete();
                    }
                }
            }
        }
    }
}