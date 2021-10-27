package com.dx.anonymousmessenger.messages;

import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.crypto.AddressedEncryptedMessage;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.Date;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class QuotedUserMessage extends UserMessage {

    private String quotedMessage;
    private String quoteSender;
    private boolean pinned;
    private String filename = null;
    private String path = null;
    private String type = null;

    //message only constructor
    public QuotedUserMessage(String quoteSender,String quotedMessage, String address, String message, String sender, long createdAt, boolean received, String to, boolean pinned) {
        super(address, message, sender, createdAt, received, to);
        this.quotedMessage = quotedMessage;
        this.quoteSender = quoteSender;
        this.pinned = pinned;
//        this.type = "";
    }

    //anything and everything
    public QuotedUserMessage(String quoteSender,String quotedMessage, String address, String message, String sender, long createdAt, boolean received, String to, boolean pinned, String filename, String path, String type) {
        super(address, message, sender, createdAt, received, to);
        this.quotedMessage = quotedMessage;
        this.quoteSender = quoteSender;
        this.pinned = pinned;
        this.filename = filename;
        this.path = path;
        this.type = type;
    }

    //for key exchanges to have shorter lines
    public QuotedUserMessage(String address, String message, String to) {
        super(address, message, address, new Date().getTime(), false, to);
        this.quotedMessage = "";
        this.quoteSender = "";
        this.pinned = false;
//        this.type = "";
    }

    //for media only messages
    public QuotedUserMessage(String address, String sender, long createdAt, boolean received, String to, String filename, String path, String type){
        super(address, "", sender, createdAt, received, to);
        this.quotedMessage = "";
        this.quoteSender = "";
        pinned = false;
        this.filename = filename;
        this.path = path;
        this.type = type;
    }

    public JSONObject toJson(DxApplication app) {
        JSONObject json = new JSONObject();
        try {
            json.put("address",getAddress());
            json.put("sender",getSender());
            json.put("msg",getMessage()==null?"":getMessage());
            json.put("createdAt",getCreatedAt());
            json.put("received",isReceived());
            json.put("to",getTo());
            json.put("quote",getQuotedMessage()==null?"":getQuotedMessage());
            json.put("quoteSender",getQuoteSender()==null?"":getQuoteSender());
            json.put("pinned",isPinned());
            json.put("filename",getFilename()==null?"":getFilename());
            json.put("path","");
            json.put("type",getType()==null?"":getType());
//            json.put("eFile", getPath()==null?"": Hex.toStringCondensed(FileHelper.getFile(getPath(),app)));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

    public static QuotedUserMessage fromJson(JSONObject json, DxApplication app){
        QuotedUserMessage um;
        String path = "";
        try {
            if(!Utils.isValidAddress(json.getString("address"))){
                throw new IllegalStateException();
            }
            if(!Utils.isValidAddress(json.getString("to"))){
                throw new IllegalStateException();
            }
            if(!isValidType(json.getString("type"))){
                throw new IllegalStateException();
            }
            if(json.getLong("createdAt")<=0){
                throw new IllegalStateException();
            }
            um = new QuotedUserMessage(json.getString("quoteSender"),json.getString("quote"),json.getString("address"),json.getString("msg"),json.getString("sender"),json.getLong("createdAt"),true,json.getString("to"),false,json.getString("filename"),path,json.getString("type"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return um;
    }

    public String getQuotedMessage() {
        return quotedMessage;
    }

    public void setQuotedMessage(String quotedMessage) {
        this.quotedMessage = quotedMessage;
    }

    public String getQuoteSender() {
        return quoteSender;
    }

    public void setQuoteSender(String quoteSender) {
        this.quoteSender = quoteSender;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public String getFilename() {
        return filename;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static QuotedUserMessage fromEncryptedJson(String msg, DxApplication app){
        try{
            JSONObject json = new JSONObject(msg);
            AddressedEncryptedMessage aem = AddressedEncryptedMessage.fromJson(json);
            if(aem==null){
                Log.e("MESSAGE RECEIVER", "FAILED TO GET AddressedEncryptedMessage FROM MESSAGE" );
                return null;
            }
            String address = json.getString("address");
            if(!Utils.isValidAddress(address)){
                throw new IllegalStateException();
            }
            if(!DbHelper.contactExists(address,app)){
                throw new IllegalStateException();
            }
            String decrypted = MessageEncryptor.decrypt(aem,app.getEntity().getStore(),new SignalProtocolAddress(address,1));
            json = new JSONObject(decrypted);

            return QuotedUserMessage.fromJson(json,app);
        }catch (Exception e){
            return null;
        }
    }

    public static boolean isValidType(String type){
        return type != null && (type.equals("") || type.equals("audio") || type.equals("image") || type.equals("file"));
    }


}
