package com.example.anonymousmessenger;

public class DxAccount {
    private String nickname;
    private byte[] identity_key;
    private String address;
    private int port = 0;
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public byte[] getIdentity_key() {
        return identity_key;
    }

    public void setIdentity_key(byte[] identity_key) {
        this.identity_key = identity_key;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    //    nickname,identity_key,address,port,
    // signal store,
    public DxAccount(String nickname, byte[] identity_key,String address,int port){

        this.nickname = nickname;
        this.identity_key = identity_key;
        this.address = address;
        this.port = port;
    }

    public DxAccount(){}

    public String getSqlInsertString(){
        return "insert into account(nickname,identity_key,address,port,password) values(?,?,?,?,?)";
    }

    public Object[] getSqlInsertValues(){
        return new Object[]{nickname,identity_key,address,port,password};
    }

    public String getSqlCreateTableString(){
        return "create table account(nickname,identity_key,address,port,password)";
    }

//    public boolean saveAccount(Context ctx){
//        if(nickname==null | address==null | port==0 | identity_key==null){
//            //maybe put more fail logic here
//            return false;
//        }
//        return true;
//    }
}
