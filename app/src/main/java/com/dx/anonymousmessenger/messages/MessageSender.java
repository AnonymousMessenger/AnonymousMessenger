package com.dx.anonymousmessenger.messages;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.crypto.AddressedEncryptedMessage;
import com.dx.anonymousmessenger.crypto.AddressedKeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.KeyExchangeMessage;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.tor.TorClient;

import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.StaleKeyExchangeException;

import java.io.FileInputStream;
import java.util.Date;
import java.util.List;

public class MessageSender {

    public static void sendMessage(QuotedUserMessage msg, DxApplication app, String to){
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                DbHelper.saveMessage(msg,app,to,false);
                //second broadcast here to make sure everything aligns correctly for the user
                Intent gcm_rec = new Intent("your_action");
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                while (app.sendingTo.contains(to)){
                    try{
                        //noinspection BusyWait
                        Thread.sleep(200);
                    }catch (Exception ignored){}
                }
                if(DbHelper.getMessageReceived(msg,app,to)){
                    return;
                }
                app.sendingTo.add(to);
                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());
                received = TorClient.sendMessage(to,app,aem.toJson().toString());
                app.sendingTo.remove(to);
            }catch (Exception e){
                app.sendingTo.remove(to);
//                Intent gcm_rec = new Intent("your_action");
//                gcm_rec.putExtra("error",app.getString(R.string.error)+": "+e.getMessage());
//                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
                DbHelper.setMessageReceived(msg,app,to,received);
            }
            if(received){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendQueuedMessage(QuotedUserMessage msg, DxApplication app, String to){
        try {
            while (app.sendingTo.contains(to)){
                try{
                    //noinspection BusyWait
                    Thread.sleep(200);
                }catch (Exception ignored){}
            }
            if(DbHelper.getMessageReceived(msg,app,to)){
                return;
            }
            app.sendingTo.add(to);
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());
                received = TorClient.sendMessage(to,app,aem.toJson().toString());
                app.sendingTo.remove(to);
            }catch (Exception e){
//                Intent gcm_rec = new Intent("your_action");
//                gcm_rec.putExtra("error",app.getString(R.string.cant_encrypt_message));
//                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                app.sendingTo.remove(to);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
                DbHelper.setMessageReceived(msg,app,to,received);
            }
            if(received){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendFile(QuotedUserMessage msg, DxApplication app, String to){
        try {
            boolean received = false;
            try {

                DbHelper.saveMessage(msg,app,to, false);
                Intent gcm_rec = new Intent("your_action");
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }

                while (app.isSendingFile()){
                    try{
                        //noinspection BusyWait
                        Thread.sleep(200);
                    }catch (Exception ignored){}
                }
                if(DbHelper.getMessageReceived(msg,app,to)){
                    return;
                }
                app.setSendingFile(true);
                //split message to file and metadata (msg is metadata)
                FileInputStream fis = FileHelper.getSharedFileStream(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClient.sendFile(
                        to,app,aem.toJson().toString(),
                        fis,FileHelper.getFileSize(msg.getPath(),app)
                );
                app.setSendingFile(false);
            }catch (Exception e){
//                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
                app.setSendingFile(false);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
                DbHelper.setMessageReceived(msg,app,to,received);
            }
            if(received){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendQueuedFile(QuotedUserMessage msg, DxApplication app, String to){
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }

                while (app.isSendingFile()){
                    try{
                        //noinspection BusyWait
                        Thread.sleep(200);
                    }catch (Exception ignored){}
                }
                if(DbHelper.getMessageReceived(msg,app,to)){
                    return;
                }
                app.setSendingFile(true);
                //split message to file and metadata (msg is metadata)
                FileInputStream fis = FileHelper.getSharedFileStream(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClient.sendFile(
                        to,app,aem.toJson().toString(),
                        fis,FileHelper.getFileSize(msg.getPath(),app)
                );
                DbHelper.setMessageReceived(msg,app,to,received);
                app.setSendingFile(false);
            }catch (Exception e){
                app.setSendingFile(false);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
//                DbHelper.setMessageReceived(msg,app,to,received);
                if(received){
                    Intent gcm_rec = new Intent("your_action");
                    gcm_rec.putExtra("delivery",msg.getCreatedAt());
                    LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                }
            }

        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendMediaMessage(QuotedUserMessage msg, DxApplication app, String to) {
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                DbHelper.saveMessage(msg,app,to, false);
                Intent gcm_rec = new Intent("your_action");
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                while (app.sendingTo.contains(to)){
                    try{
                        //noinspection BusyWait
                        Thread.sleep(200);
                    }catch (Exception ignored){}
                }
                if(DbHelper.getMessageReceived(msg,app,to)){
                    return;
                }
                app.sendingTo.add(to);
                //split message to file and metadata (msg is metadata)
                byte[] file = FileHelper.getFile(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClient.sendMedia(to,app,aem.toJson().toString(), MessageEncryptor.encrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(to,1)),false);
                app.sendingTo.remove(to);
            }catch (Exception e){
                app.sendingTo.remove(to);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MEDIA MESSAGE FAILED");
            }finally {
                DbHelper.setMessageReceived(msg,app,to,received);
            }
            if(received){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendMediaMessageWithoutSaving(QuotedUserMessage msg, DxApplication app, String to, boolean saveMsg, boolean isProfileImage) {
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                if(saveMsg){
                    DbHelper.saveMessage(msg,app,to, false);
                    Intent gcm_rec = new Intent("your_action");
                    LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                }

                while (app.sendingTo.contains(to)){
                    try{
                        //noinspection BusyWait
                        Thread.sleep(200);
                    }catch (Exception ignored){}
                }

                if(saveMsg && DbHelper.getMessageReceived(msg,app,to)){
                    return;
                }
                app.sendingTo.add(to);
                //split message to file and metadata (msg is metadata)
                byte[] file = FileHelper.getFile(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClient.sendMedia(to,app,aem.toJson().toString(), MessageEncryptor.encrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(to,1)),isProfileImage);
                app.sendingTo.remove(to);
            }catch (Exception e){
//                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
                app.sendingTo.remove(to);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
                if(saveMsg){
                    DbHelper.setMessageReceived(msg,app,to,received);
                }
                if(received && isProfileImage){
                    DbHelper.setContactSentProfileImagePath(msg.getPath(),msg.getTo(),app);
                }
            }
            if(saveMsg && received){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendMediaMessageToAll(QuotedUserMessage msg, DxApplication app, String to, boolean saveMsg, boolean isProfileImage){
        try{
            //get contacts list
            List<String[]> contacts = DbHelper.getContactsList(app);
            //send to each in loop
            if (contacts != null) {
                for (String[] contact:contacts) {
                    if(contact == null){
                        continue;
                    }
                    try{
                        msg.setTo(contact[1]);
                        sendMediaMessageWithoutSaving(msg,app,contact[1],saveMsg,isProfileImage);
                    }catch (Exception ignored){
//                        e.printStackTrace();
                    }

                }
            }
        }catch (Exception e){
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE TO ALL" );
            e.printStackTrace();
        }
    }

    public static void sendQueuedMediaMessage(QuotedUserMessage msg, DxApplication app, String to) {
        try {
            while (app.sendingTo.contains(to)){
                try{
                    //noinspection BusyWait
                    Thread.sleep(200);
                }catch (Exception ignored){}
            }
            if(DbHelper.getMessageReceived(msg,app,to)){
                return;
            }
            app.sendingTo.add(to);
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                //split message to file and metadata (msg is metadata)
                byte[] file = FileHelper.getFile(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClient.sendMedia(to,app,aem.toJson().toString(), MessageEncryptor.encrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(to,1)),false);
                app.sendingTo.remove(to);
            }catch (Exception e){
//                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
                app.sendingTo.remove(to);
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
                DbHelper.setMessageReceived(msg,app,to,received);
            }
            if(received){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendKeyExchangeMessage(DxApplication app, String to){
        try {
            QuotedUserMessage msg = new QuotedUserMessage("","",app.getHostname(),app.getString(R.string.key_exchange_message), app.getHostname(),new Date().getTime(),false,to,false);
            DbHelper.saveMessage(msg,app,to,false);
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            KeyExchangeMessage kem = MessageEncryptor.getKeyExchangeMessage(app.getEntity().getStore(),new SignalProtocolAddress(to,1));
            AddressedKeyExchangeMessage akem = new AddressedKeyExchangeMessage(kem,app.getHostname(),false);
            boolean received = TorClient.sendMessage(to,app,akem.toJson().toString());
            DbHelper.setMessageReceived(msg,app,to,received);
            if(received){
//                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        }catch (Exception e){
            Log.e("SENDER MESSAGE EXCHANGE", "FAIL" );
            e.printStackTrace();
        }
    }

    public static void sendKeyExchangeMessageWithoutBroadcast(DxApplication app, String to){
        try {
            QuotedUserMessage msg = new QuotedUserMessage("","",app.getHostname(),app.getString(R.string.key_exchange_message), app.getHostname(),new Date().getTime(),false,to,false);
            DbHelper.saveMessage(msg,app,to,false);
            Intent gcm_rec = new Intent("your_action");
            KeyExchangeMessage kem = MessageEncryptor.getKeyExchangeMessage(app.getEntity().getStore(),new SignalProtocolAddress(to,1));
            AddressedKeyExchangeMessage akem = new AddressedKeyExchangeMessage(kem,app.getHostname(),false);
            boolean received = TorClient.sendMessage(to,app,akem.toJson().toString());
            DbHelper.setMessageReceived(msg,app,to,received);
            if(received){
//                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        }catch (Exception e){
            Log.e("SENDER MESSAGE EXCHANGE", "FAIL" );
            e.printStackTrace();
        }
    }

    public static void sendKeyExchangeMessage(DxApplication app, String to, KeyExchangeMessage ikem){
        try {
            QuotedUserMessage msg = new QuotedUserMessage("","",app.getHostname(),app.getString(R.string.resp_key_exchange), app.getHostname(),new Date().getTime(),false,to,false);
            DbHelper.saveMessage(msg,app,to,false);
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            KeyExchangeMessage kem = MessageEncryptor.getKeyExchangeMessage(app.getEntity().getStore(),new SignalProtocolAddress(to,1),ikem);
            AddressedKeyExchangeMessage akem = new AddressedKeyExchangeMessage(kem,app.getHostname(),true);
            boolean received = TorClient.sendMessage(to,app,akem.toJson().toString());
            DbHelper.setMessageReceived(msg,app,to,received);
            if(received){
//                gcm_rec.putExtra("delivery",msg.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            }
        }catch (Exception | StaleKeyExchangeException e){
            Log.e("SENDER MESSAGE EXCHANGE", "FAIL" );
            e.printStackTrace();
        }
    }

}
