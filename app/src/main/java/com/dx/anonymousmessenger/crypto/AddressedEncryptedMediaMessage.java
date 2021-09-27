package com.dx.anonymousmessenger.crypto;

import com.dx.anonymousmessenger.util.Base64;

import org.json.JSONObject;

public class AddressedEncryptedMediaMessage {
    String address;
    //e means encrypted for short
    byte[] eFile;
    String type;
    String filename;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getEFile() {
        return eFile;
    }

    public void setEFile(byte[] eFile) {
        this.eFile = eFile;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

// --Commented out by Inspection START (9/27/21, 10:15 AM):
//    public AddressedEncryptedMediaMessage(String address, byte[] eFile, String type, String filename) throws Exception {
//        if(!address.trim().endsWith(".onion")){
//            throw new Exception("Invalid Address");
//        }
//        this.address = address;
//        this.type = type;
//        this.filename = filename;
//        this.eFile = eFile;
//    }
// --Commented out by Inspection STOP (9/27/21, 10:15 AM)

    public JSONObject toJson(){
        try {
            JSONObject json = new JSONObject();
            json.put("address", this.address);
            json.put("eFile", Base64.encodeBytesWithoutPadding(this.eFile));
            json.put("type", this.type);
            json.put("filename", this.filename);
// --Commented out by Inspection START (9/27/21, 10:15 AM):
//            return json;
//        } catch (JSONException i) {
//            i.printStackTrace();
//        }
//        return null;
//    }
//
//    public static AddressedEncryptedMediaMessage fromJson(JSONObject input){
//        try {
//            String address = input.getString("address");
//            byte[] eFile = Base64.decodeWithoutPadding(input.getString("eFile"));
//            String type = input.getString("type");
//            String filename = input.getString("filename");
//            return new AddressedEncryptedMediaMessage(address,eFile,type,filename);
// --Commented out by Inspection STOP (9/27/21, 10:15 AM)
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
