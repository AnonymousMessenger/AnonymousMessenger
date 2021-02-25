package com.dx.anonymousmessenger.messages;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.crypto.AddressedEncryptedMessage;
import com.dx.anonymousmessenger.crypto.AddressedKeyExchangeMessage;
import com.dx.anonymousmessenger.crypto.SessionBuilder;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.StaleKeyExchangeException;
import org.whispersystems.libsignal.UntrustedIdentityException;

import java.util.Date;
import java.util.Objects;

import static com.dx.anonymousmessenger.messages.MessageSender.sendKeyExchangeMessage;

public class MessageReceiver {

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
                    DbHelper.saveLog("RECEIVED BAD/UNKNOWN ADDRESS "+json.getString("address") ,new Date().getTime(),"SEVERE",app);
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
                        gcm_rec.putExtra("address",json.getString("address").substring(0,10));
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
                        gcm_rec.putExtra("address",json.getString("address").substring(0,10));
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
                    String decrypted = MessageEncrypter.decrypt(aem,app.getEntity().getStore(),new SignalProtocolAddress(json.getString("address"),1));
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
                    gcm_rec.putExtra("address",um.getAddress().substring(0,10));
                    LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

                } catch (Exception e) {
                    DbHelper.saveMessage(new QuotedUserMessage("","",json.getString("address"),app.getString(R.string.failed_to_decrypt), json.getString("address"),new Date().getTime(),false,json.getString("address"),false),app,json.getString("address"),false);

                    Intent gcm_rec = new Intent("your_action");
                    gcm_rec.putExtra("address",json.getString("address").substring(0,10));
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
            String decrypted = MessageEncrypter.decrypt(aem,app.getEntity().getStore(),new SignalProtocolAddress(address,1));
            json = new JSONObject(decrypted);

            QuotedUserMessage um = QuotedUserMessage.fromJson(json,app);
            if(um == null){
                return;
            }
            file = MessageEncrypter.decrypt(file,app.getEntity().getStore(),new SignalProtocolAddress(address,1));
            um.setPath(FileHelper.saveFile(file,app,um.getFilename()));
            if(um.getPath()==null){
                return;
            }

            DbHelper.saveMessage(um,app,um.getAddress(),true);

            DbHelper.setContactNickname(um.getSender(), um.getAddress(), app);
            DbHelper.setContactUnread(um.getAddress(),app);

            app.sendNotification(app.getString(R.string.new_message),app.getString(R.string.you_have_message));
            Intent gcm_rec = new Intent("your_action");
            gcm_rec.putExtra("address",um.getAddress().substring(0,10));
            LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MESSAGE RECEIVER", "FAILED TO RECEIVE MEDIA MESSAGE" );
        }
    }
}
