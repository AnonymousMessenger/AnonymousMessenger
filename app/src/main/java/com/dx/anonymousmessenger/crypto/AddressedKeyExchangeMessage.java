package com.dx.anonymousmessenger.crypto;

import android.util.Log;

import com.dx.anonymousmessenger.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

public class AddressedKeyExchangeMessage {
    KeyExchangeMessage kem;
    String address;
    boolean response;

    public KeyExchangeMessage getKem() {
        return kem;
    }

    public void setKem(KeyExchangeMessage kem) {
        this.kem = kem;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public AddressedKeyExchangeMessage(KeyExchangeMessage kem, String address) throws Exception {
        if(!address.trim().endsWith(".onion")){
            throw new Exception("Invalid Address");
        }
        this.kem = kem;
        this.address = address;
        this.response = false;
    }

    public AddressedKeyExchangeMessage(KeyExchangeMessage kem, String address, boolean response) throws Exception {
        if(!address.trim().endsWith(".onion")){
            throw new Exception("Invalid Address");
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
            KeyExchangeMessage kem = new KeyExchangeMessage(Base64.decodeWithoutPadding(input.getString("kem")));
            boolean response = input.getBoolean("response");
            return new AddressedKeyExchangeMessage(kem, address,response);
        } catch (Exception i) {
            i.printStackTrace();
            Log.e("FROM JSON", "fromJson: EROREOROOROROROEOROERORO");
        }
        return null;
    }
}
