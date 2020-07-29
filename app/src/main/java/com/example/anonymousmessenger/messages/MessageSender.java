package com.example.anonymousmessenger.messages;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.MessageListActivity;
import com.example.anonymousmessenger.db.DbHelper;
import com.example.anonymousmessenger.tor.TorClientSocks4;

import java.io.IOException;

public class MessageSender {
    public static boolean sendMessage(UserMessage msg, DxApplication app, String to){
        try {
            boolean b = new TorClientSocks4(app.getApplicationContext()).Init(to,app,
                    ("AM3, "+msg.getAddress()+","+msg.getSender()+","+msg.getMessage()+","+msg.getCreatedAt()));
            DbHelper.saveMessage(msg,app,to,false);
            return b;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean messageReceiver(String msg, DxApplication app){
        Log.e("MESSAGE INCOMING","IT IS COMING NIGGER");
        Log.e("MESSAGE INCOMING",msg+"::END OF MESSAGE::");
        String address;
        String sender;
        String message;
        long createdAt;
        String[] splt = msg.split(",");
        int j = 0;
        for(int i=0;i<splt.length;i++){
            if(splt[i].contains("AM3")){
//                if((splt.length-1-i)==4){
                address = splt[i+1].trim();
                sender = splt[i+2].trim();
                message = splt[i+3].trim();
                createdAt = Long.parseLong(splt[i+4].trim());
                UserMessage um = new UserMessage(address,message,sender,createdAt,true,app.getHostname());
                boolean saved = DbHelper.saveMessage(um,app,address,true);
                Log.e("savedsavedsaved",saved?"yes":"no");
                Intent gcm_rec = new Intent("your_action");  LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                return saved;
//                }
            }
        }
        return false;
    }
}
