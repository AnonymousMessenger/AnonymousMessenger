package com.dx.anonymousmessenger.account;

import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;

import net.sqlcipher.database.SQLiteDatabase;

public class DxAccount {
    private String address;
    private String nickname;
    private byte[] password;

    public static final String CREATE_ACCOUNT_TABLE_SQL = "CREATE TABLE IF NOT EXISTS account(nickname,password)";
    public static final String INSERT_ACCOUNT_SQL = "INSERT INTO account(nickname,password) values(?,?)";
    public static final String DELETE_ACCOUNT_SQL = "DELETE FROM account";

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeNickname(String nickname, DxApplication app){
        setNickname(nickname);
        saveAccount(this,app);
    }

    public DxAccount(String nickname){
        this.nickname = nickname;
    }

    public DxAccount(String nickname, String address){
        this.address = address;
        this.nickname = nickname;
    }

    public DxAccount(String nickname, byte[] password){
        this.nickname = nickname;
        this.password = password;
    }

    public DxAccount(){}

    public Object[] getSqlInsertValues(){
        return new Object[]{nickname,""};
    }

    public static void saveAccount(DxAccount account, DxApplication app) {
        Log.d("Account Saver","Saving Account");
        SQLiteDatabase database = app.getDb(account.getPassword());
//        SQLiteDatabase database = app.getDb();
        try{
            database.execSQL(DxAccount.CREATE_ACCOUNT_TABLE_SQL);
            database.execSQL(DxAccount.DELETE_ACCOUNT_SQL);
            database.execSQL(DxAccount.INSERT_ACCOUNT_SQL,account.getSqlInsertValues());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
