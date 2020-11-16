package com.dx.anonymousmessenger;

import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

public class DxAccount {
    private String nickname;
    private String password;

    public static final String CREATE_ACCOUNT_TABLE_SQL = "CREATE TABLE IF NOT EXISTS account(nickname,password)";
    public static final String INSERT_ACCOUNT_SQL = "INSERT INTO account(nickname,password) values(?,?)";
    public static final String DELETE_ACCOUNT_SQL = "DELETE FROM account";

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

    public void changeNickname(String nickname, DxApplication app){
        setNickname(nickname);
        saveAccount(this,app);
    }

    public DxAccount(String nickname){
        this.nickname = nickname;
    }

    public DxAccount(String nickname, String password){
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

}
