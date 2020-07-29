package com.example.anonymousmessenger.db;

import android.util.Log;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.messages.UserMessage;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DbHelper {
    public static String getContactSqlInsert(){
        return "insert into contact(nickname,address) values(?,?)";
    }

    public static Object[] getContactSqlValues(String address){
        return new Object[]{"",address};
    }

    public static Object[] getContactSqlValues(String address, String nickname){
        return new Object[]{nickname,address};
    }

    public static String getContactTableSqlCreate(){
        return "create table if not exists contact (nickname,address);";
    }

    public static List<String[]> getContactsList(DxApplication app){
        Log.d("Contact Saver","Saving Account");
        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        Cursor cr = database.rawQuery("select * from contact;",null);
        List<String[]> contacts = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                contacts.add(new String[]{cr.getString(0),cr.getString(1)});
            } while (cr.moveToNext());
        }
        cr.close();
        return contacts;
    }

    public static String getMessageTableSqlCreate(){
        return "create table if not exists message (send_from,send_to,number,msg,sender,created_at,conversation,received);";
    }

    public static String getMessageSqlInsert(){
        return "insert into message (send_from,send_to,number,msg,sender,created_at,conversation,received) values(?,?,?,?,?,?,?,?)";
    }

    public static Object[] getMessageSqlValues(String from, String to, String number, String msg, String sender, long createdAt, String conversation, boolean received){
        return new Object[]{from,to,number,msg,sender,createdAt,conversation,received};
    }

    public static List<UserMessage> getMessageList(DxApplication app, String conversation){
        Log.d("Message Getter","Getting Message");
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        Cursor cr = database.rawQuery("select * from message where conversation=?;",new Object[]{conversation});
        List<UserMessage> messages = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                messages.add(new UserMessage(cr.getString(0),cr.getString(3),cr.getString(4),
                        cr.getLong(5),cr.getString(6).equals("yes"),cr.getString(1)));
            } while (cr.moveToNext());
        }
        cr.close();
        return messages;
    }

    public static boolean saveMessage(UserMessage msg, DxApplication app, String conversation, boolean received){
        Log.d("Message Saver","Saving Message");
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received));
        Log.d("Message Saver","Saved Message");
        return true;
    }
}
