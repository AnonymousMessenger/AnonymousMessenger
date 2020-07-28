package com.example.anonymousmessenger.messages;

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
        this.address = address;
        this.received = received;
        this.to = to;
        this.setMessage(message);
        this.setSender(sender);
        this.setCreatedAt(createdAt);
    }
}
