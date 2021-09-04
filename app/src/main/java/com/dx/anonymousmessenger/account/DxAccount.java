package com.dx.anonymousmessenger.account;

import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;

import net.sqlcipher.database.SQLiteDatabase;

public class DxAccount {
    private String address;
    private String nickname;
    private byte[] password;
    private String profileImagePath = "";

    public static final String CREATE_ACCOUNT_TABLE_SQL = "CREATE TABLE IF NOT EXISTS account(nickname,password,profile_image_path)";
    public static final String INSERT_ACCOUNT_SQL = "INSERT INTO account(nickname,password,profile_image_path) values(?,?,?)";
    public static final String DELETE_ACCOUNT_SQL = "DELETE FROM account";

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public void changeProfileImage(String profileImagePath, DxApplication app){
        setProfileImagePath(profileImagePath);
        saveAccount(this,app);
    }

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
        return new Object[]{nickname,"",profileImagePath};
    }

    public static void saveAccount(DxAccount account, DxApplication app) {
        Log.d("Account Saver","Saving Account");
        SQLiteDatabase database;
        try{
            if(app.getDbOrNull()==null && account.getPassword()!=null){
                database = app.getDb(account.getPassword());
            }else{
                database = app.getDb();
            }
            database.beginTransaction();
            database.execSQL(DxAccount.CREATE_ACCOUNT_TABLE_SQL);
            database.execSQL(DxAccount.DELETE_ACCOUNT_SQL);
            database.execSQL(DxAccount.INSERT_ACCOUNT_SQL,account.getSqlInsertValues());
            database.setTransactionSuccessful();
            database.endTransaction();
            Log.d("Account Saver","Account Saved");
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
