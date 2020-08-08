package com.dx.anonymousmessenger.db;

import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DbHelper {
    public static String getContactSqlInsert(){
        return "INSERT INTO contact(nickname,address) VALUES(?,?)";
    }

    public static String getContactColumns(){return "(nickname,address,unread)";}

    public static String getContactSqlUpdate(){
        return "UPDATE contact SET nickname=? WHERE address=?";
    }

    public static String getContactSqlUpdateUnread(){ return "UPDATE contact SET unread=? WHERE address=?"; }

    public static Object[] getContactSqlValuesUnread(String address){ return new Object[]{true,address}; }

    public static Object[] getContactSqlValues(String address){
        return new Object[]{"",address};
    }

    public static Object[] getContactSqlValues(String address, String nickname){
        return new Object[]{nickname,address};
    }

    public static String getContactTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS contact (nickname,address,unread);";
    }

    public static List<String[]> getContactsList(DxApplication app){
        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        Cursor cr = database.rawQuery("SELECT * FROM contact ORDER BY unread DESC;",null);
        List<String[]> contacts = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                contacts.add(new String[]{cr.getString(0),cr.getString(1),cr.getInt(2)>0?"unread":"read"});
            } while (cr.moveToNext());
        }
        cr.close();
        return contacts;
    }

    public static boolean saveContact(String address, DxApplication app) {
        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            return false;
        }
        else
        {
            database.execSQL(DbHelper.getContactSqlInsert(),DbHelper.getContactSqlValues(address));
            return true;
        }
    }

    public static boolean saveContact(String address, String nickname, DxApplication app) {
        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            return false;
        }
        else
        {
            database.execSQL(DbHelper.getContactSqlInsert(),DbHelper.getContactSqlValues(address,nickname));
            return true;
        }
    }

    public static boolean setContactNickname(String nickname, String address, DxApplication app){
        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            database.execSQL(DbHelper.getContactSqlUpdate(),DbHelper.getContactSqlValues(address,nickname));
            return true;
        }
        else
        {
            return false;
        }
    }

    public static boolean setContactUnread(String address, DxApplication app) {

        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        android.database.Cursor c=database.rawQuery(getContactSqlUpdateUnread(), getContactSqlValuesUnread(address));
        if(c.moveToFirst())
        {
            database.execSQL(DbHelper.getContactSqlUpdateUnread(),DbHelper.getContactSqlValuesUnread(address));
            return true;
        }
        else
        {
            return false;
        }

    }

    public static boolean setContactRead(String address, DxApplication app) {

        while (app.getAccount()==null||app.getAccount().getPassword()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getContactTableSqlCreate());
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            database.execSQL(DbHelper.getContactSqlUpdateUnread(),new Object[]{0,address});
            return true;
        }
        else
        {
            return false;
        }

    }

    public static String getMessageTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS message "+getMessageColumns()+";";
    }

    public static String getMessageSqlInsert(){
        return "INSERT INTO message "+getMessageColumns()+" VALUES(?,?,?,?,?,?,?,?,?,?)";
    }

    public static String getMessageColumns(){
        return "(send_from,send_to,number,msg,sender,created_at,conversation,received,quote,quote_sender)";
    }

    public static Object[] getMessageSqlValues(String from, String to, String number, String msg, String sender, long createdAt, String conversation, boolean received, String quote, String quoteSender){
        return new Object[]{from,to,number,msg,sender,createdAt,conversation,received,quote,quoteSender};
    }

    public static List<QuotedUserMessage> getMessageList(DxApplication app, String conversation){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getMessageColumns().trim().substring(1,getMessageColumns().length()-1)+" FROM message");
        }catch (Exception e){
            Log.e("MESSAGE LIST", Objects.requireNonNull(e.getMessage()));
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM message WHERE conversation=?;",new Object[]{conversation});
        List<QuotedUserMessage> messages = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                messages.add(new QuotedUserMessage(cr.getString(9),cr.getString(8),cr.getString(0),cr.getString(3),cr.getString(4),
                        cr.getLong(5),cr.getInt(7)>0,cr.getString(1)));
            } while (cr.moveToNext());
        }
        cr.close();
        setContactRead(conversation,app);
        return messages;
    }

    private static void updateDbSchema(SQLiteDatabase database) {
        String tmpName = "temp_message";
        String createTemp = getMessageTableSqlCreate().replace("message",tmpName);
        database.execSQL(createTemp);
        int tmpColNum = getNumberOfColumns(tmpName,database);
        int oldColNum = getNumberOfColumns("message",database);
        String emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
        database.execSQL("INSERT INTO "+tmpName+getMessageColumns()+" SELECT *"+emptyCols+" FROM message;");
        database.execSQL("DROP TABLE message;");
        database.execSQL("ALTER TABLE "+tmpName+" RENAME TO message");

        tmpName = "temp_contact";
        createTemp = getContactTableSqlCreate().replace("contact",tmpName);
        database.execSQL(createTemp);
        tmpColNum = getNumberOfColumns(tmpName,database);
        oldColNum = getNumberOfColumns("contact",database);
        emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
        database.execSQL("INSERT INTO "+tmpName+getContactColumns()+" SELECT *"+emptyCols+" FROM contact;");
        database.execSQL("DROP TABLE contact;");
        database.execSQL("ALTER TABLE "+tmpName+" RENAME TO contact");
    }

//    public static boolean saveMessage(UserMessage msg, DxApplication app, String conversation, boolean received){
//        SQLiteDatabase database = app.getDb();
//        database.execSQL(DbHelper.getMessageTableSqlCreate());
//        database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,"",""));
//        return true;
//    }

    public static boolean saveMessage(QuotedUserMessage msg, DxApplication app, String conversation, boolean received){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getQuotedMessage(),msg.getQuoteSender()));
        return true;
    }

    public static int getNumberOfColumns(String tableName, SQLiteDatabase database) {
        Cursor cursor = database.query(tableName, null, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getColumnCount() > 0) {
                return cursor.getColumnCount();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static String giveMeNulls(int qty){
        String nulls = "";
        if(qty==0){return nulls;}
        for (int i=0;i<qty;i++){
            nulls = nulls.concat(",null");
        }
        return nulls;
    }

}
