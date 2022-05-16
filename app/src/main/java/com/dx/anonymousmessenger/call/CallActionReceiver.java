package com.dx.anonymousmessenger.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CallActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action=intent.getStringExtra("action");
//        if(Objects.requireNonNull(action).equals("answer")){
//            answer();
//        }
//        else if(action.equals("hangup")){
//            hangup();
//        }
        //This is used to close the notification tray
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }

//    public void answer(){
//
//    }
//
//    public void hangup(){
//
//    }
}
