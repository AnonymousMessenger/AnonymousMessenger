package com.dx.anonymousmessenger.messages;

import org.json.JSONException;
import org.json.JSONObject;

public class QuotedUserMessage extends UserMessage {
    private String quotedMessage;
    private String quoteSender;
    private boolean pinned = false;
    public QuotedUserMessage(String quoteSender,String quotedMessage, String address, String message, String sender, long createdAt, boolean received, String to, boolean pinned) {
        super(address, message, sender, createdAt, received, to);
        this.quotedMessage = quotedMessage;
        this.quoteSender = quoteSender;
        this.pinned = pinned;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("type","AM3");
            json.put("address",getAddress());
            json.put("sender",getSender());
            json.put("msg",getMessage());
            json.put("createdAt",getCreatedAt());
            json.put("received",isReceived());
            json.put("to",getTo());
            json.put("quote",getQuotedMessage());
            json.put("quoteSender",getQuoteSender());
            json.put("pinned",isPinned());
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    public static QuotedUserMessage fromJson(JSONObject json){
        QuotedUserMessage um;
        try {
            um = new QuotedUserMessage(json.getString("quoteSender"),json.getString("quote"),json.getString("address"),json.getString("msg"),json.getString("sender"),json.getLong("createdAt"),json.getBoolean("received"),json.getString("to"),json.getBoolean("pinned"));
        } catch (Exception e) {
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
}
