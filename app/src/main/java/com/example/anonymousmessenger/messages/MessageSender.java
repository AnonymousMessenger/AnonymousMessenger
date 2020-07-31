package com.example.anonymousmessenger.messages;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.db.DbHelper;
import com.example.anonymousmessenger.tor.TorClientSocks4;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MessageSender {
    public static void sendMessage(UserMessage msg, DxApplication app, String to){
        try {
            boolean b = new TorClientSocks4().Init(to,app,
                    msg.toJson().toString());
            DbHelper.saveMessage(msg,app,to,b);
            Intent gcm_rec = new Intent("your_action");  LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean messageReceiver(String msg, DxApplication app){
        try {
            JSONObject json = new JSONObject(msg);
            UserMessage um = UserMessage.fromJson(json);
            boolean saved = DbHelper.saveMessage(um,app,um.getAddress(),true);
            Intent gcm_rec = new Intent("your_action");  LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            return saved;
        } catch (JSONException e) {
            return false;
        }
//        String address;
//        String sender;
//        String message;
//        long createdAt;
//        String[] splt = msg.split(",");
//        for(int i=0;i<splt.length;i++){
//            if(splt[i].contains("AM3")){
////                if((splt.length-1-i)==4){
//                address = splt[i+1].trim();
//                sender = splt[i+2].trim();
//                message = splt[i+3].trim();
//                createdAt = Long.parseLong(splt[i+4].trim());
//                UserMessage um = new UserMessage(address,message,sender,createdAt,true,app.getHostname());
//                boolean saved = DbHelper.saveMessage(um,app,address,true);
//                Intent gcm_rec = new Intent("your_action");  LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
//                return saved;
////                }
//            }
//            Log.e("BAD MESSAGE","SOOOOOOOO MFKNG BAD");
//        }
//        return false;
    }
}
