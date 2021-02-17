package com.dx.anonymousmessenger.call;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class CallService extends Service {

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("CALL SERVICE", "onStartCommand...");
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;

//        if      (intent.getAction().equals(ACTION_RECEIVE_OFFER))               handleReceivedOffer(intent);
//        else if (intent.getAction().equals(ACTION_RECEIVE_BUSY))                handleReceivedBusy(intent);
//        else if (intent.getAction().equals(ACTION_OUTGOING_CALL))               handleOutgoingCall(intent);
//        else if (intent.getAction().equals(ACTION_DENY_CALL))                   handleDenyCall(intent);
//        else if (intent.getAction().equals(ACTION_LOCAL_HANGUP))                handleLocalHangup(intent);
//        else if (intent.getAction().equals(ACTION_SET_MUTE_AUDIO))              handleSetMuteAudio(intent);
//        else if (intent.getAction().equals(ACTION_START_OUTGOING_CALL))         handleStartOutgoingCall(intent);
        else if (intent.getAction().equals(ACTION_START_OUTGOING_CALL_RESPONSE)) handleOutgoingCallResponse(intent);
//        else if (intent.getAction().equals(ACTION_START_INCOMING_CALL))         handleStartIncomingCall(intent);
        else if (intent.getAction().equals(ACTION_ACCEPT_CALL))                 handleAcceptCall(intent);
//        else if (intent.getAction().equals(ACTION_LOCAL_RINGING))               handleLocalRinging(intent);
//        else if (intent.getAction().equals(ACTION_REMOTE_RINGING))              handleRemoteRinging(intent);
//        else if (intent.getAction().equals(ACTION_RECEIVE_ANSWER))              handleReceivedAnswer(intent);
//        else if (intent.getAction().equals(ACTION_RECEIVE_HANGUP))              handleReceivedHangup(intent);
//        else if (intent.getAction().equals(ACTION_ENDED))                       handleEnded(intent);

        return START_NOT_STICKY;
    }

    private void handleOutgoingCallResponse(Intent intent) {
        handleAcceptCall(intent);
    }

    private void handleAcceptCall(Intent intent) {
    }

//    private void handleStartIncomingCall(Intent intent) {
//        startForeground(1,getCallNotification(intent.getStringExtra("address"),"Incoming call"));
//        if(cmi!=null){
//            cmi.stop();
//            cmi = null;
//        }
//        if(cmo!=null){
//            cmo.stop();
//            cmo = null;
//        }
//        cmo = new CallMaker(intent.getStringExtra("address"),((DxApplication)getApplication()));
//    }

//    private void handleStartOutgoingCall(Intent intent) {
//        startForeground(1,getCallNotification(intent.getStringExtra("address"),"Outgoing call"));
//        if(cmo!=null){
//            cmo.stop();
//            cmo = null;
//        }
//        cmo = new CallMaker(intent.getStringExtra("address"),((DxApplication)getApplication()));
//    }

//    public Notification getCallNotification(String address, String type){
//        Intent fullScreenIntent = new Intent(this, CallActivity.class);
//        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
//                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "call")
//            .setSmallIcon(R.drawable.notification)
//            .setContentTitle(type)
//            .setContentText(address)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setCategory(NotificationCompat.CATEGORY_CALL)
//            .setFullScreenIntent(fullScreenPendingIntent, true);

        //dbhelper.getnickanmefromaddress(Address) ok?

//        Notification ntf = ((DxApplication)getApplication()).sendNotification(type, address, "calls");
//        Notification ntf = DxApplication.getCallInProgressNotification(this,DxApplication.TYPE_INCOMING_RINGING,address);
//        startForeground(9, ntf);
//         return ntf;
//    }

}
