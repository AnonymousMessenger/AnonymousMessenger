package com.dx.anonymousmessenger.messages;

import org.json.JSONException;
import org.json.JSONObject;

public class UserMessage extends Message{
    private String address;
    private  boolean received;
    private String to;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public UserMessage(String address, String message, String sender, long createdAt, boolean received, String to){
        super();
        if(address == null || to == null || message == null || createdAt == 0){
            System.out.println("address");
            System.out.println(address==null);
            System.out.println("to");
            System.out.println(to==null);
            System.out.println("msg");
            System.out.println(message==null);
            System.out.println("cat");
            System.out.println(createdAt==0);
            throw new IllegalStateException();
        }
        this.address = address;
        this.received = received;
        this.to = to;
        this.setMessage(message);
        this.setSender(sender);
        this.setCreatedAt(createdAt);
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        try {
            json.put("type","AM3");
            json.put("address",getAddress());
            json.put("sender",getSender());
            json.put("msg",getMessage());
            json.put("createdAt",getCreatedAt());
            json.put("received",isReceived());
            json.put("to",getTo());
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    public static UserMessage fromJson(JSONObject json){
        UserMessage um;
        try {
            um = new UserMessage(json.getString("address"),json.getString("msg"),json.getString("sender"),json.getLong("createdAt"),json.getBoolean("received"),json.getString("to"));
        } catch (Exception e) {
            return null;
        }
        return um;
    }
}
