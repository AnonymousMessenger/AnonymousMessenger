package com.dx.anonymousmessenger.messages;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.crypto.AddressedEncryptedMessage;
import com.dx.anonymousmessenger.crypto.AddressedKeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.KeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.SessionBuilder;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.tor.TorClientSocks4;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.StaleKeyExchangeException;
import org.whispersystems.libsignal.UntrustedIdentityException;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class MessageSender {
    public static void sendMessage(QuotedUserMessage msg, DxApplication app, String to){
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());
                DbHelper.saveMessage(msg,app,to,false);
                //second broadcast here to make sure everything aligns correctly for the user
                Intent gcm_rec = new Intent("your_action");
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                received = TorClientSocks4.Init(to,app,aem.toJson().toString());
            }catch (Exception e){
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("error",app.getString(R.string.error)+": "+e.getMessage());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
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
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());
                received = TorClientSocks4.Init(to,app,aem.toJson().toString());
            }catch (Exception e){
//                Intent gcm_rec = new Intent("your_action");
//                gcm_rec.putExtra("error",app.getString(R.string.cant_encrypt_message));
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

    public static void sendFile(QuotedUserMessage msg, DxApplication app, String to){
        //get encrypted file stream
        //send stream
        //needs message for to know it was received

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
                //split message to file and metadata (msg is metadata)
                FileInputStream file = FileHelper.getSharedFileStream(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

//                received = TorClientSocks4.sendFile(to,app,aem.toJson().toString(),MessageEncryptor.encrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(to,1)));
            }catch (Exception e){
//                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
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
                //split message to file and metadata (msg is metadata)
                byte[] file = FileHelper.getFile(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClientSocks4.sendMedia(to,app,aem.toJson().toString(),MessageEncryptor.encrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(to,1)));
            }catch (Exception e){
//                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
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

    public static void sendQueuedMediaMessage(QuotedUserMessage msg, DxApplication app, String to) {
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained!!!" );
                    return;
                }
                //split message to file and metadata (msg is metadata)
                byte[] file = FileHelper.getFile(msg.getPath(),app);

                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson(app).toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());

                received = TorClientSocks4.sendMedia(to,app,aem.toJson().toString(),MessageEncryptor.encrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(to,1)));
            }catch (Exception e){
//                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
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
            boolean received = TorClientSocks4.Init(to,app,akem.toJson().toString());
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
            boolean received = TorClientSocks4.Init(to,app,akem.toJson().toString());
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
            boolean received = TorClientSocks4.Init(to,app,akem.toJson().toString());
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

    public static void sendProcessedKeyExchangeMessage(AddressedKeyExchangeMessage akem, DxApplication app){
        try {
            DbHelper.saveMessage(new QuotedUserMessage(app.getHostname(),app.getString(R.string.resp_key_exchange),akem.getAddress()),app,akem.getAddress(),false);
            KeyExchangeMessage kem = new SessionBuilder(app.getEntity().getStore(), new SignalProtocolAddress(akem.getAddress(),1)).process(akem.getKem());
            Log.d("KEM INITIATE", "messageReceiver: "+ Arrays.toString(kem.serialize()));
            AddressedKeyExchangeMessage akem3 = new AddressedKeyExchangeMessage(kem,app.getHostname(),true);
            TorClientSocks4.Init(akem.getAddress(),app,akem3.toJson().toString());
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
        }catch (Exception | StaleKeyExchangeException e){
            Log.e("MESSAGE RECEIVER", "FAILED TO sendProcessedKeyExchangeMessage " );
            e.printStackTrace();
        }
    }

    public static void messageReceiver(String msg, DxApplication app){
        try {
            JSONObject json = new JSONObject(msg);
            if(json.has("address")){
                //make isValid function dude
                if(json.getString("address").length() < 54 ||
                        json.getString("address").length() > 64 ||
                        !json.getString("address").endsWith(".onion") ||
                        !DbHelper.contactExists(json.getString("address"),app)){
                    Log.e("MSG RCVR","RECEIVED BAD/UNKNOWN ADDRESS, throwing away message");
                    return;
                }
            }else{
                return;
            }
            app.sendNotification(app.getString(R.string.new_message),app.getString(R.string.you_have_message));
            if(json.has("kem")){
                AddressedKeyExchangeMessage akem = AddressedKeyExchangeMessage.fromJson(json);

                if(!Objects.requireNonNull(akem).getKem().isInitiate()){
                    try {
                        SessionBuilder sb = new SessionBuilder(app.getEntity().getStore(), new SignalProtocolAddress(akem.getAddress(),1));
                        sb.process(akem.getKem());

                        DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),app.getString(R.string.resp_key_exchange), json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
                        Intent gcm_rec = new Intent("your_action");
                        LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

                    } catch (UntrustedIdentityException | InvalidKeyException | StaleKeyExchangeException e) {
                        DbHelper.saveMessage(new QuotedUserMessage("",
                                "",
                                json.getString("address"),
                                (e.getClass().equals(StaleKeyExchangeException.class)?app.getString(R.string.stale_key)
                                        :e.getClass().equals(InvalidKeyException.class)?app.getString(R.string.invalid_key)
                                        :e.getClass().equals(UntrustedIdentityException.class)?app.getString(R.string.untrusted_identity):app.getString(R.string.bad_message)),
                                json.getString("address"),
                                new Date().getTime(),
                                false,
                                json.getString("address"),
                                false),app,json.getString("address"),false);
                        Intent gcm_rec = new Intent("your_action");
                        LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                        e.printStackTrace();
                        Log.e("MESSAGE RECEIVER", "FAILED!!! Received Response Key Exchange Message : ");
                    } catch (Exception e){
                        e.printStackTrace();
                        Log.e("MESSAGE RECEIVER ERROR", "FAILED!!! Received Response Key Exchange Message : ");
                    }
                }else{
                    DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),app.getString(R.string.key_exchange_message), json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
                    sendKeyExchangeMessage(app,akem.getAddress(),akem.getKem());
//                    Log.e("MESSAGE RECEIVER", "GOT KEM: SENDING KEM BACK TO : "+json.getString("address"));
                }
            }else{
                try {
                    AddressedEncryptedMessage aem = AddressedEncryptedMessage.fromJson(json);
                    if(aem==null){
                        DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),app.getString(R.string.failed_to_get_AEM), json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
                        Log.e("MESSAGE RECEIVER", "FAILED TO GET AddressedEncryptedMessage FROM MESSAGE" );
                        return;
                    }
                    String decrypted = MessageEncryptor.decrypt(aem,app.getEntity().getStore(),new SignalProtocolAddress(json.getString("address"),1));
                    json = new JSONObject(decrypted);

                    QuotedUserMessage um = QuotedUserMessage.fromJson(json,app);
                    if(um == null){
                        DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),app.getString(R.string.failed_to_read_after_decrypt), json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
                        return;
                    }
                    new Thread(()-> DbHelper.setContactNickname(um.getSender(),um.getAddress(),app)).start();
                    new Thread(()-> DbHelper.setContactUnread(um.getAddress(),app)).start();
                    DbHelper.saveMessage(um,app,um.getAddress(),true);
                    Intent gcm_rec = new Intent("your_action");
                    LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

                } catch (Exception e) {
                    DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),app.getString(R.string.failed_to_decrypt), json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);

                    Intent gcm_rec = new Intent("your_action");
                    LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                    e.printStackTrace();
                    Log.e("MESSAGE RECEIVER", "FAILED TO DECRYPT MESSAGE" );
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("MESSAGE RECEIVER", "FAILED TO RECEIVE MESSAGE" );
        }
    }

    public static void mediaMessageReceiver(byte[] file, String msg, DxApplication app){
        try {
            JSONObject json = new JSONObject(msg);
            AddressedEncryptedMessage aem = AddressedEncryptedMessage.fromJson(json);
            if(aem==null){
                Log.e("MESSAGE RECEIVER", "FAILED TO GET AddressedEncryptedMessage FROM MESSAGE" );
                return;
            }
            String address = json.getString("address");
            String decrypted = MessageEncryptor.decrypt(aem,app.getEntity().getStore(),new SignalProtocolAddress(address,1));
            json = new JSONObject(decrypted);

            QuotedUserMessage um = QuotedUserMessage.fromJson(json,app);
            if(um == null){
                return;
            }
            file = MessageEncryptor.decrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(address,1));
            um.setPath(FileHelper.saveFile(file,app,um.getFilename()));
            if(um.getPath()==null){
                return;
            }

            new Thread(()-> DbHelper.setContactNickname(um.getSender(),um.getAddress(),app)).start();
            new Thread(()-> DbHelper.setContactUnread(um.getAddress(),app)).start();

            DbHelper.saveMessage(um,app,um.getAddress(),true);

            app.sendNotification(app.getString(R.string.new_message),app.getString(R.string.you_have_message));
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MESSAGE RECEIVER", "FAILED TO RECEIVE MEDIA MESSAGE" );
        }
    }

    public static void pinMessage(QuotedUserMessage message, DxApplication app) {
        DbHelper.pinMessage(message,app);
    }

    public static void unPinMessage(QuotedUserMessage message, DxApplication app) {
        DbHelper.unPinMessage(message,app);
    }

}
