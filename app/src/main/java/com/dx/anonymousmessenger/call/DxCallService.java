package com.dx.anonymousmessenger.call;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.db.DbHelper;

import java.util.Objects;

public class DxCallService extends Service {
    public static final String ACTION_RECEIVE_OFFER = "1";
    public static final String ACTION_RECEIVE_BUSY = "2";
    public static final String ACTION_OUTGOING_CALL = "3";
    public static final String ACTION_DENY_CALL = "4";
    public static final String ACTION_LOCAL_HANGUP = "5";
    public static final String ACTION_START_OUTGOING_CALL = "6";
    public static final String ACTION_START_INCOMING_CALL = "7";
    public static final String ACTION_SET_MUTE_AUDIO = "8";
    public static final String ACTION_ACCEPT_CALL = "9";
    public static final String ACTION_LOCAL_RINGING = "10";
    public static final String ACTION_REMOTE_RINGING = "11";
    public static final String ACTION_RECEIVE_ANSWER = "12";
    public static final String ACTION_RECEIVE_HANGUP = "13";
    public static final String ACTION_ENDED = "14";
    public static final String ACTION_START_OUTGOING_CALL_RESPONSE = "15";
//    BroadcastReceiver br;

    public DxCallService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

//        br = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.e("ACTIONNNNNNNN", Objects.requireNonNull(intent.getAction()));
//                String action=intent.getAction();
//                if (action != null) {
//                    if(action.equals("answer")){
//                        if(((DxApplication)getApplication()).isInCall()){
//                            try {
//                                ((DxApplication) getApplication()).getCc().answerCall(false);
//                                createNotification(((DxApplication)getApplication()).getCc().getAddress(),action);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                stopSelf();
//                            }
//                        }else{
//                            stopSelf();
//                        }
//                    }
//                    else if(action.equals("hangup")){
//                        if(((DxApplication)getApplication()).isInCall()){
//                            try {
//                                ((DxApplication) getApplication()).getCc().stopCall();
//                                ((DxApplication) getApplication()).setCc(null);
//                                stopSelf();
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }else{
//                            stopSelf();
//                        }
//                    }
//                }
//                //This is used to close the notification tray
//                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//                context.sendBroadcast(it);
//            }
//        };
//        try {
//            LocalBroadcastManager.getInstance(this).registerReceiver(br,new IntentFilter("call_action"));
//        } catch (Exception e)
//        {
//            e.printStackTrace();
//        }

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String partialAddress = intent.getStringExtra("address");
        final String address = DbHelper.getFullAddress(partialAddress,
                (DxApplication) getApplication());
        if(address==null){
            return START_NOT_STICKY;
        }
        String action = intent.getAction();

        //todo maybe put this line somewhere else or directly change text
        createNotification(Objects.requireNonNull(address).substring(0,10),action);

        new Thread(()->{
            if (action != null) {
                switch (action) {
                    case "start_out_call":
                        if (!((DxApplication) getApplication()).isInCall()) {
                            ((DxApplication) getApplication()).setCc(new CallController(address, ((DxApplication) getApplication())));
                        }
                        break;
                    case "answer":
                        if (((DxApplication) getApplication()).isInCall()) {
                            try {
                                ((DxApplication) getApplication()).getCc().answerCall(false);
                                Log.d("ANONYMOUSMESSENGER","call answered");
                                Intent gcm_rec = new Intent("call_action");
                                gcm_rec.putExtra("action","answer");
                                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                            } catch (Exception e) {
                                Log.d("ANONYMOUSMESSENGER","call not answered");
                                e.printStackTrace();
                                Intent gcm_rec = new Intent("call_action");
                                gcm_rec.putExtra("action","hangup");
                                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                                stopSelf();
                            }
                        } else {
                            Intent gcm_rec = new Intent("call_action");
                            gcm_rec.putExtra("action","hangup");
                            LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                            stopSelf();
                        }
                        break;
                    case "hangup":
                        if (((DxApplication) getApplication()).isInCall()) {
                            try {
                                ((DxApplication) getApplication()).getCc().stopCall();
                                ((DxApplication) getApplication()).setCc(null);
                                Intent gcm_rec = new Intent("call_action");
                                gcm_rec.putExtra("action","hangup");
                                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                                stopSelf();
                            } catch (Exception e) {
                                e.printStackTrace();
                                ((DxApplication) getApplication()).setCc(null);
                                Intent gcm_rec = new Intent("call_action");
                                gcm_rec.putExtra("action","hangup");
                                LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                                stopSelf();
                            }
                        } else {
                            Intent gcm_rec = new Intent("call_action");
                            gcm_rec.putExtra("action","hangup");
                            LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
                            stopSelf();
                        }
                        break;
                }
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(br);
        if(((DxApplication)getApplication()).isInCall()){
            ((DxApplication)getApplication()).getCc().stopCall();
            ((DxApplication)getApplication()).setCc(null);
        }
        super.onDestroy();
        Intent gcm_rec = new Intent("call_action");
        gcm_rec.putExtra("action","hangup");
        LocalBroadcastManager.getInstance(this).sendBroadcast(gcm_rec);
        stopSelf();
    }

    public void createNotification(String address, String type){
        Notification ntf = ((DxApplication)getApplication()).getCallInProgressNotification(this,type,address);
        startForeground(9, ntf);
    }

    public void updateNotification(){

    }

    public void makeNewCall(){

    }

    public void receiveNewCall(){

    }

    public void receiveCallResponse(){

    }

    public void makeCallResponse(){

    }

    public void startCall(){

    }

    public void sendAnswerOrHangup(){

    }

    public void stopCall(){

    }

    public void startRinging(){

    }

    public void stopRinging(){

    }
}
