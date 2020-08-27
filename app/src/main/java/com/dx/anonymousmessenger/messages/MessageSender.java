package com.dx.anonymousmessenger.messages;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.crypto.AddressedEncryptedMessage;
import com.dx.anonymousmessenger.crypto.AddressedKeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.KeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.SessionBuilder;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.tor.TorClientSocks4;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.StaleKeyExchangeException;
import org.whispersystems.libsignal.UntrustedIdentityException;

import java.util.Arrays;
import java.util.Date;

public class MessageSender {
    public static void sendMessage(QuotedUserMessage msg, DxApplication app, String to){
        try {
            boolean received = false;
            try {
                if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(to,1))){
                    Log.e("MESSAGE SENDER", "sendMessage: session isn't contained nigga!!!" );
                    return;
                }
                DbHelper.saveMessage(msg,app,to,false);
                AddressedEncryptedMessage aem = new AddressedEncryptedMessage(MessageEncryptor.encrypt(msg.toJson().toString(),app.getEntity().getStore(),new SignalProtocolAddress(to,1)),app.getHostname());
                received = new TorClientSocks4().Init(to,app,aem.toJson().toString());
            }catch (Exception e){
                Toast.makeText(app,"Couldn't encrypt message",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.e("MESSAGE SENDER","SENDING MESSAGE FAILED WITH ENCRYPTION");
            }finally {
                DbHelper.setMessageReceived(msg,app,to,received);
            }
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
        } catch (Exception e) {
            Log.e("MESSAGE SENDER", "FAILED TO SEND MESSAGE" );
            e.printStackTrace();
        }
    }

    public static void sendKeyExchangeMessage(DxApplication app, String to){
        try {
            QuotedUserMessage msg = new QuotedUserMessage("","",app.getHostname(),"Key Exchange Message", app.getHostname(),new Date().getTime(),false,to,false);
            DbHelper.saveMessage(msg,app,to,false);
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            KeyExchangeMessage kem = MessageEncryptor.getKeyExchangeMessage(app.getEntity().getStore(),new SignalProtocolAddress(to,1));
            AddressedKeyExchangeMessage akem = new AddressedKeyExchangeMessage(kem,app.getHostname(),false);
            boolean received = new TorClientSocks4().Init(to,app,akem.toJson().toString());
            DbHelper.setMessageReceived(msg,app,to,received);
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
        }catch (Exception e){
            Log.e("SENDER MESSAGE EXCHANGE", "FAIL" );
            e.printStackTrace();
        }
    }

    public static void sendKeyExchangeMessage(DxApplication app, String to, KeyExchangeMessage ikem){
        try {
            QuotedUserMessage msg = new QuotedUserMessage("","",app.getHostname(),"RESPONSE Key Exchange Message", app.getHostname(),new Date().getTime(),false,to,false);
            DbHelper.saveMessage(msg,app,to,false);
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            KeyExchangeMessage kem = MessageEncryptor.getKeyExchangeMessage(app.getEntity().getStore(),new SignalProtocolAddress(to,1),ikem);
            AddressedKeyExchangeMessage akem = new AddressedKeyExchangeMessage(kem,app.getHostname(),true);
            boolean received = new TorClientSocks4().Init(to,app,akem.toJson().toString());
            DbHelper.saveMessage(msg,app,to,received);
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
        }catch (Exception | StaleKeyExchangeException e){
            Log.e("SENDER MESSAGE EXCHANGE", "FAIL" );
            e.printStackTrace();
        }
    }

    public static void sendProcessedKeyExchangeMessage(AddressedKeyExchangeMessage akem, DxApplication app){
        try {
            DbHelper.saveMessage(new QuotedUserMessage(app.getHostname(),"Response Key Exchange Message",akem.getAddress()),app,akem.getAddress(),false);
            KeyExchangeMessage kem = new SessionBuilder(app.getEntity().getStore(), new SignalProtocolAddress(akem.getAddress(),1)).process(akem.getKem());
            Log.d("KEM INITIATE", "messageReceiver: "+ Arrays.toString(kem.serialize()));
            AddressedKeyExchangeMessage akem3 = new AddressedKeyExchangeMessage(kem,app.getHostname(),true);
            new TorClientSocks4().Init(akem.getAddress(),app,akem3.toJson().toString());
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
            if(json.has("kem")){
//                if(app.getEntity().getStore().containsSession(new SignalProtocolAddress(json.getString("address"),1))){
//                    if(!app.getEntity().getStore().loadSession(new SignalProtocolAddress(json.getString("address"),1)).getSessionState().hasPendingKeyExchange()){
//                        DbHelper.saveMessage(new QuotedUserMessage("","",app.getHostname(),"Received Key Exchange Message WHEN WE ALREADY HAVE A SESSION", app.getHostname(),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
////                        DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),"Received Key Exchange Message WHEN WE ALREADY HAVE A SESSION", json.getString("address"),new Date().getTime(),false,app.getHostname(),false),app,app.getHostname(),false);
//                        Log.e("MESSAGE RECEIVER","GOT KEM WHEN WE ALREADY HAVE A SESSION");
//                        return;
//                    }
//                }
                AddressedKeyExchangeMessage akem = AddressedKeyExchangeMessage.fromJson(json);

                if(!akem.getKem().isInitiate()){
                    try {
                        Log.d("KEM RESPONSE", "messageReceiver: "+ Arrays.toString(akem.getKem().serialize()));
                        Log.d("KEM RESPONSE", "FROM: "+ akem.getAddress());

                        SessionBuilder sb = new SessionBuilder(app.getEntity().getStore(), new SignalProtocolAddress(akem.getAddress(),1));
                        sb.process(akem.getKem());

                        DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),"Response Key Exchange Message", json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
                        Intent gcm_rec = new Intent("your_action");
                        LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

                    } catch (UntrustedIdentityException | InvalidKeyException | StaleKeyExchangeException e) {
                        e.printStackTrace();
                        Log.e("MESSAGE RECEIVER", "FAILED!!! Received Response Key Exchange Message : " + json.getString("address"));
                    } catch (Exception e){
                        e.printStackTrace();
                        Log.e("MESSAGE RECEIVER ERROR", "FAILED!!! Received Response Key Exchange Message : " + json.getString("address"));
                    }
                    return;
                }else{
                    DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),"Key Exchange Message", json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);
                    sendKeyExchangeMessage(app,akem.getAddress(),akem.getKem());
                    Log.e("MESSAGE RECEIVER", "GOT KEM: SENDING KEM BACK TO : "+json.getString("address"));
                }
                return;
            }else{
                try {
                    AddressedEncryptedMessage aem = AddressedEncryptedMessage.fromJson(json);
                    if(aem==null){
                        Log.e("MESSAGE RECEIVER", "FAILED TO GET AddressedEncryptedMessage FROM MESSAGE" );
                        return;
                    }
                    String decrypted = MessageEncryptor.decrypt(aem,app.getEntity().getStore(),new SignalProtocolAddress(json.getString("address"),1));
                    json = new JSONObject(decrypted);
                    Log.d("RECEIVING MESSAGE",json.getString("msg"));

                    QuotedUserMessage um = QuotedUserMessage.fromJson(json);
                    if(um == null){return;}
                    new Thread(()->{DbHelper.setContactNickname(um.getSender(),um.getAddress(),app);}).start();
                    new Thread(()->{DbHelper.setContactUnread(um.getAddress(),app);}).start();
                    DbHelper.saveMessage(um,app,um.getAddress(),true);
                    Intent gcm_rec = new Intent("your_action");
                    LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

                } catch (LegacyMessageException | InvalidMessageException | DuplicateMessageException | NoSessionException | UntrustedIdentityException e) {
                    e.printStackTrace();
                    Log.e("MESSAGE RECEIVER", "FAILED TO RECEIVE MESSAGE" );
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("MESSAGE RECEIVER", "FAILED TO RECEIVE MESSAGE" );
        }
    }

    public static void pinMessage(QuotedUserMessage message, DxApplication app) {
        DbHelper.pinMessage(message,app);
    }

    public static void unPinMessage(QuotedUserMessage message, DxApplication app) {
        DbHelper.unPinMessage(message,app);
    }
}
