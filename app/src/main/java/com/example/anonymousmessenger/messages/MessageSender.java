package com.example.anonymousmessenger.messages;

import android.util.Log;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.db.DbHelper;
import com.example.anonymousmessenger.tor.TorClientSocks4;
import com.example.anonymousmessenger.util.Utils;

import java.io.IOException;
import java.io.StreamCorruptedException;

public class MessageSender {
    public static boolean sendMessage(UserMessage msg, DxApplication app, String to){
        try {
            DbHelper.saveMessage(msg,app,to,false);
            String out = new TorClientSocks4(app.getApplicationContext()).Init(to,app,
                    ("Anonymous Message, "+msg.getAddress()+","+msg.getSender()+","+msg.getMessage()+","+msg.getCreatedAt()).getBytes());
            if(out.contains("received")){
                return true;
            }else {
                //add to queue for resend
                return false;
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public static boolean messageReceiver(String msg, DxApplication app){
        String address = "";
        String sender = "";
        String message = "";
        long createdAt = 0;
        String[] splt = msg.split(",");
        int j = 0;
        for(int i=0;i<splt.length;i++){
            if(splt[i].contains("Anonymous Message")){
//                if((splt.length-1-i)==4){
                address = splt[i+1].trim();
                sender = splt[i+2].trim();
                message = splt[i+3].trim();
                Log.e("message",message);
                createdAt = Long.parseLong(splt[i+4].trim());
                UserMessage um = new UserMessage(address,message,sender,createdAt,true,app.getHostname());
                boolean saved = DbHelper.saveMessage(um,app,address,true);
                Log.e("savedsavedsaved",saved?"yes":"no");
                return true;
//                }
            }
        }
        return false;
    }
}
