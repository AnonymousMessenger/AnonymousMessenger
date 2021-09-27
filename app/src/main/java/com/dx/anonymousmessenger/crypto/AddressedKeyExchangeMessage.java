package com.dx.anonymousmessenger.crypto;

import android.util.Log;

import com.dx.anonymousmessenger.util.Base64;
import com.dx.anonymousmessenger.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class AddressedKeyExchangeMessage {
    KeyExchangeMessage kem;
    String address;
    boolean response;

    public boolean isResponse(){
        return response;
    }

    public KeyExchangeMessage getKem() {
        return kem;
    }

// --Commented out by Inspection START (9/27/21, 10:15 AM):
//    public void setKem(KeyExchangeMessage kem) {
//        this.kem = kem;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:15 AM)

    public String getAddress() {
        return address;
    }

// --Commented out by Inspection START (9/27/21, 10:15 AM):
//    public void setAddress(String address) {
//        this.address = address;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:15 AM)

// --Commented out by Inspection START (9/27/21, 10:15 AM):
//    public AddressedKeyExchangeMessage(KeyExchangeMessage kem, String address) throws IllegalStateException {
//        if(!Utils.isValidAddress(address)){
//            throw new IllegalStateException("Invalid Address");
//        }
//        this.kem = kem;
//        this.address = address;
//        this.response = false;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:15 AM)

    public AddressedKeyExchangeMessage(KeyExchangeMessage kem, String address, boolean response) throws IllegalStateException{
        if(!Utils.isValidAddress(address)){
            throw new IllegalStateException("Invalid Address");
        }
        this.kem = kem;
        this.address = address;
        this.response = response;
    }

    public JSONObject toJson(){
        try {
            JSONObject json = new JSONObject();
            json.put("address", this.address);
            json.put("kem", Base64.encodeBytesWithoutPadding(kem.serialize()));
            json.put("response",response);
            return json;
          } catch (JSONException i) {
             i.printStackTrace();
          }
        return null;
    }

    public static AddressedKeyExchangeMessage fromJson(JSONObject input){
        try {
            String address = input.getString("address");
            if(!Utils.isValidAddress(address)){
                throw new IllegalStateException();
            }
            KeyExchangeMessage kem = new KeyExchangeMessage(Base64.decodeWithoutPadding(input.getString("kem")));
            boolean response = input.getBoolean("response");
            return new AddressedKeyExchangeMessage(kem, address,response);
        } catch (Exception i) {
            i.printStackTrace();
            Log.e("FROM JSON", "fromJson: AddressedKeyExchangeMessage had invalid input");
        }
        return null;
    }
}
