package com.dx.anonymousmessenger.crypto;

import android.util.Log;

import com.dx.anonymousmessenger.util.Base64;
import com.dx.anonymousmessenger.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class AddressedEncryptedMessage {

    byte[] msg;
    String address;

    public byte[] getMsg() {
        return msg;
    }

// --Commented out by Inspection START (9/27/21, 10:01 AM):
//    public void setMsg(byte[] msg) {
//        this.msg = msg;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:01 AM)

// --Commented out by Inspection START (9/27/21, 10:01 AM):
//    public String getAddress() {
//        return address;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:01 AM)

// --Commented out by Inspection START (9/27/21, 10:01 AM):
//    public void setAddress(String address) {
//        this.address = address;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:01 AM)

    public AddressedEncryptedMessage(byte[] msg, String address) throws Exception {
        if(!address.trim().endsWith(".onion")){
            throw new Exception("Invalid Address");
        }
        this.msg = msg;
        this.address = address;
    }

    public JSONObject toJson(){
        try {
            JSONObject json = new JSONObject();
            json.put("address", this.address);
            json.put("msg", Base64.encodeBytesWithoutPadding(this.msg));
            return json;
        } catch (JSONException i) {
            i.printStackTrace();
        }
        return null;
    }

    public static AddressedEncryptedMessage fromJson(JSONObject input){
        try {
            String address = input.getString("address");
            if(!Utils.isValidAddress(address)){
                throw new IllegalStateException();
            }
            byte[] msg = Base64.decodeWithoutPadding(input.getString("msg"));
            return new AddressedEncryptedMessage(msg, address);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FROM JSON", "fromJson: EROREOROOROROROEOROERORO");
        }
        return null;
    }
}
