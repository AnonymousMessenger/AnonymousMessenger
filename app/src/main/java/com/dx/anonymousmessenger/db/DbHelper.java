package com.dx.anonymousmessenger.db;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.crypto.DxSignalKeyStore;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DbHelper {
    public static final String CONTACT_SQL_INSERT = "INSERT INTO contact(nickname,address) VALUES(?,?)";
    public static final String CONTACT_COLUMNS = "(nickname,address,unread)";
    public static final String CONTACT_SQL_UPDATE = "UPDATE contact SET nickname=? WHERE address=?";
    public static final String CONTACT_SQL_UPDATE_UNREAD = "UPDATE contact SET unread=? WHERE address=?";
    public static final String CONTACT_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS contact (nickname,address,unread)";

    public static Object[] getContactSqlValuesUnread(String address){ return new Object[]{true,address}; }

    public static Object[] getContactSqlValues(String address){
        return new Object[]{"",address};
    }

    public static Object[] getContactSqlValues(String address, String nickname){
        return new Object[]{nickname,address};
    }

    public static List<String[]> getContactsList(DxApplication app) {
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        while (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        Cursor cr = database.rawQuery("SELECT * FROM contact ORDER BY unread DESC;",null);
        List<String[]> contacts = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                String address = cr.getString(1);
                database.execSQL(DbHelper.getMessageTableSqlCreate());
                Cursor cr2 = database.rawQuery("SELECT * FROM message WHERE conversation=? ORDER BY rowid DESC LIMIT 1;",new Object[]{address});
                if (cr2.moveToFirst()) {
                    QuotedUserMessage message = new QuotedUserMessage(cr2.getString(9),
                            cr2.getString(8),
                            cr2.getString(0),
                            cr2.getString(3),
                            cr2.getString(4),
                            cr2.getLong(5),
                            cr2.getInt(7)>0,
                            cr2.getString(1),
                            cr2.getInt(10)>0,
                            cr2.getString(11),
                            cr2.getString(12),
                            cr2.getString(13));
                    if ((new Date().getTime() - message.getCreatedAt()) >= app.getTime2delete()) {
                        if(!message.isPinned()) {
                            deleteMessage(message, app);
//                            cr2.close();
//                            cr.close();
//                            return getContactsList(app);
                        }
                    }
                    cr2.close();

                    String msg;
                    if(message.getType()!=null && message.getType().equals("audio")){
                        msg = app.getString(R.string.audio_message);
                    }else if(message.getType()!=null && message.getType().equals("image")){
                        msg = app.getString(R.string.media_message);
                    }else{
                        msg = message.getMessage();
                    }

                    contacts.add(new String[]{cr.getString(0), address, cr.getInt(2) > 0 ? "unread" : "read",msg,message.getTo(), String.valueOf(message.getCreatedAt()),message.isReceived()?"true":"false"});
                }else{
                    cr2.close();
                    contacts.add(new String[]{cr.getString(0), address, cr.getInt(2) > 0 ? "unread" : "read","","","",""});
                }
            } while (cr.moveToNext());
        }
        cr.close();
        java.util.Collections.sort(contacts, (o1, o2) -> {

            if(o1[2].equals("unread")){
                if(!o2[2].equals("unread")){
                    return -1;
                }
            }else if(o2[2].equals("unread")){
                    return 1;
            }

            if(o1[5].equals("")){
                if(!o2[5].equals("")){
                    return 1;
                }else{
                    return 0;
                }
            }else if(o2[5].equals("")){ return -1; }

            if(Long.parseLong(o1[5]) > Long.parseLong(o2[5])){
                return -1;
            }else if(Long.parseLong(o1[5]) < Long.parseLong(o2[5])){
                return 1;
            }

            return 0;
        });
        return contacts;
    }

    public static boolean saveContact(String address, DxApplication app) {
        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            return false;
        }
        else
        {
            c.close();
//            c = null;
//            database.execSQL(DbHelper.CONTACT_SQL_INSERT,DbHelper.getContactSqlValues(address));
            database.execSQL(CONTACT_SQL_INSERT,getContactSqlValues(address));
            return true;
        }
    }

    public static boolean saveContact(String address, String nickname, DxApplication app) {
        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            return false;
        }
        else
        {
            database.execSQL(DbHelper.CONTACT_SQL_INSERT,DbHelper.getContactSqlValues(address,nickname));
            c.close();
            return true;
        }
    }

    public static void deleteContact(String address, DxApplication app) {
        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        clearConversation(address,app);
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        database.execSQL("DELETE FROM contact WHERE address=?",new Object[]{address});
        app.getEntity().getStore().deleteSession(new SignalProtocolAddress(address,1));
        ((DxSignalKeyStore)app.getEntity().getStore()).removeIdentity(new SignalProtocolAddress(address,1));
    }

    public static boolean setContactNickname(String nickname, String address, DxApplication app){
        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE,DbHelper.getContactSqlValues(address,nickname));
            return true;
        }
        else
        {
            c.close();
            return false;
        }
    }

    public static String getContactNickname(String address, DxApplication app) {
        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            String nn = c.getString(c.getColumnIndex("nickname"));
            c.close();
            return nn;
        }
        else
        {
            c.close();
            return null;
        }
    }

    public static boolean setContactUnread(String address, DxApplication app) {

        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery(CONTACT_SQL_UPDATE_UNREAD, getContactSqlValuesUnread(address));
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_UNREAD,DbHelper.getContactSqlValuesUnread(address));
            return true;
        }
        else
        {
            c.close();
            return false;
        }

    }

    public static boolean setContactRead(String address, DxApplication app) {

        while (app.getAccount()==null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_UNREAD,new Object[]{0,address});
            return true;
        }
        else
        {
            c.close();
            return false;
        }

    }

    public static boolean setContactRead(String address, SQLiteDatabase database) {
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_UNREAD,new Object[]{0,address});
            return true;
        }
        else
        {
            c.close();
            return false;
        }
    }

    public static boolean contactExists(String address, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            return true;
        }
        else
        {
            c.close();
            return false;
        }
    }

    public static String getMessageTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS message "+getMessageColumns()+";";
    }

    public static String getMessageSqlInsert(){
        return "INSERT INTO message "+getMessageColumns()+" VALUES(?"+giveMeQMarks(getMessageColumns().split(",").length-1)+")";
    }

    public static String getMessageColumns(){
        return "(send_from,send_to,number,msg,sender,created_at,conversation,received,quote,quote_sender,pinned,filename,path,type)";
    }

    public static Object[] getMessageSqlValues(String from, String to, String number, String msg, String sender, long createdAt, String conversation, boolean received, String quote, String quoteSender){
        return new Object[]{from,to,number,msg,sender,createdAt,conversation,received,quote,quoteSender,false,null,null,null};
    }

    public static Object[] getFullMessageSqlValues(String from, String to, String number, String msg, String sender, long createdAt, String conversation, boolean received, String quote, String quoteSender, String filename, String path, String type){
        return new Object[]{from,to,number,msg,sender,createdAt,conversation,received,quote,quoteSender,false,filename,path,type};
    }

    public static Object[] getMediaMessageSqlValues(String from, String to, String number, String sender, long createdAt, String conversation, boolean received, String filename, String path, String type){
        return new Object[]{from,to,number,"",sender,createdAt,conversation,received,"","",false,filename,path,type};
    }

    public static List<QuotedUserMessage> getMessageList(DxApplication app, String conversation){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getMessageColumns().trim().substring(1,getMessageColumns().length()-1)+" FROM message");
//            database.query("SELECT "+DbHelper.getMessageColumns()+" FROM message");
        }catch (Exception e){
            Log.w("getMessageList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM message WHERE conversation=?;",new Object[]{conversation});
        List<QuotedUserMessage> messages = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                QuotedUserMessage message = new QuotedUserMessage(cr.getString(9),
                        cr.getString(8),
                        cr.getString(0),
                        cr.getString(3),
                        cr.getString(4),
                        cr.getLong(5),
                        cr.getInt(7)>0,
                        cr.getString(1),
                        cr.getInt(10)>0,
                        cr.getString(11),
                        cr.getString(12),
                        cr.getString(13));

                if ((new Date().getTime() - message.getCreatedAt()) < app.getTime2delete()) {
                    messages.add(message);
                } else {
                    if(!message.isPinned()) {
                        deleteMessage(message, app);
                        Intent gcm_rec = new Intent("your_action");
                        gcm_rec.putExtra("delete",message.getCreatedAt());
                        LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                    }
                    else {
                        messages.add(message);
                    }
                }
            } while (cr.moveToNext());
        }
        cr.close();
        setContactRead(conversation,database);
        return messages;
    }

    public static List<QuotedUserMessage> getUndeliveredMessageList(DxApplication app, String conversation){
        if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(conversation,1)) ||
                app.getEntity().getStore().getIdentity(new SignalProtocolAddress(conversation,1)) == null){
            if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(conversation,1))){
                new Thread(()-> MessageSender.sendKeyExchangeMessage(app,conversation)).start();
            }
            return new ArrayList<>();
        }
        if(app.getEntity().getStore().loadSession(new SignalProtocolAddress(conversation,1)).getSessionState().hasPendingKeyExchange()){
            new Thread(()-> MessageSender.sendKeyExchangeMessage(app,conversation)).start();
            return new ArrayList<>();
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getMessageColumns().trim().substring(1,getMessageColumns().length()-1)+" FROM message LIMIT 1");
        }catch (Exception e){
            Log.w("getMessageList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM message WHERE conversation=? AND send_from=? AND received=?;",new Object[]{conversation,app.getHostname(),false});
        List<QuotedUserMessage> messages = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                QuotedUserMessage message = new QuotedUserMessage(cr.getString(9),cr.getString(8),cr.getString(0),cr.getString(3),cr.getString(4),
                        cr.getLong(5),cr.getInt(7)>0,cr.getString(1),cr.getInt(10)>0,cr.getString(11),cr.getString(12),cr.getString(13));

                if ((new Date().getTime() - message.getCreatedAt()) < (60*1000)) {
                    continue;
                }

                if(app.getEntity().getStore().loadSession(new SignalProtocolAddress(conversation,1)).getSessionState().hasPendingKeyExchange()){
                    if(message.getMessage().equals(app.getString(R.string.key_exchange_message))
                            ||
                            message.getMessage().equals(app.getString(R.string.resp_key_exchange))
                    ){
                        new Thread(()-> MessageSender.sendKeyExchangeMessage(app,conversation)).start();
                        deleteMessage(message,app);
                        return new ArrayList<>();
                    }
                }

//                if(cr.isLast() && message.getMessage().equals(app.getString(R.string.resp_key_exchange))){
//                    new Thread(()-> MessageSender.sendKeyExchangeMessage(app,conversation)).start();
//                    deleteMessage(message,app);
//                    return new ArrayList<>();
//                }

                if(message.getMessage().equals(app.getString(R.string.key_exchange_message))
                            ||
                            message.getMessage().equals(app.getString(R.string.resp_key_exchange))
                    ){
                        deleteMessage(message,app);
                        continue;
                    }

                if ((new Date().getTime() - message.getCreatedAt()) < app.getTime2delete()) {
                    messages.add(message);
                } else {
                    if(!message.isPinned()) deleteMessage(message, app);
                    else messages.add(message);
                }
            } while (cr.moveToNext());
        }
        cr.close();
//        setContactRead(conversation,database);
        if(messages.size()==0 && app.getEntity().getStore().loadSession(new SignalProtocolAddress(conversation,1)).getSessionState().hasPendingKeyExchange()){
            new Thread(()-> MessageSender.sendKeyExchangeMessage(app,conversation)).start();
        }
        return messages;
    }

    private static void updateDbSchema(SQLiteDatabase database) {
        int tries = 0;
        Log.e("starting the db update","same");
        while(tries<2){
            tries++;
            try{
                database.beginTransaction();
                String tmpName = "temp_message ";
                String createTemp = getMessageTableSqlCreate().replace("message",tmpName);
                database.execSQL(createTemp);
                int tmpColNum = getNumberOfColumns(tmpName,database);
                int oldColNum = getNumberOfColumns("message",database);
                String emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                database.execSQL("INSERT INTO "+tmpName+getMessageColumns()+" SELECT *"+emptyCols+" FROM message;");
                database.execSQL("DROP TABLE message;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO message");
                database.setTransactionSuccessful();
                database.endTransaction();

                database.beginTransaction();
                tmpName = "temp_contact ";
                createTemp = CONTACT_TABLE_SQL_CREATE.replace("contact",tmpName);
                database.execSQL(createTemp);
                tmpColNum = getNumberOfColumns(tmpName,database);
                oldColNum = getNumberOfColumns("contact",database);
                emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                database.execSQL("INSERT INTO "+tmpName+CONTACT_COLUMNS+" SELECT *"+emptyCols+" FROM contact;");
                database.execSQL("DROP TABLE contact;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO contact");
                database.setTransactionSuccessful();
                database.endTransaction();
                Log.e("finishing the db update","good bye");
            }catch (SQLiteException e){
                Log.e("ERROR UPDATING DB","THERE WAS AN ERROR UPDATING THE D B YO!");
                e.printStackTrace();
            }
        }
    }

//    public static boolean saveMessage(UserMessage msg, DxApplication app, String conversation, boolean received){
//        SQLiteDatabase database = app.getDb();
//        database.execSQL(DbHelper.getMessageTableSqlCreate());
//        database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,"",""));
//        return true;
//    }

//    public static void updateReceivedMessage(QuotedUserMessage msg, DxApplication app, String conversation, boolean received){
//        SQLiteDatabase database = app.getDb();
//        database.execSQL(DbHelper.getMessageTableSqlCreate());
//        database.execSQL(DbHelper.getMessageSqlUpdate(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getQuotedMessage(),msg.getQuoteSender()));
//    }

    public static boolean saveMessage(QuotedUserMessage msg, DxApplication app, String conversation, boolean received){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        if(msg.getType()==null || msg.getType().equals("")){
            database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getQuotedMessage(),msg.getQuoteSender()));
        }else{
            database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getFullMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getQuotedMessage(),msg.getQuoteSender(),msg.getFilename(),msg.getPath(),msg.getType()));
//            database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMediaMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getFilename(),msg.getPath(),msg.getType()));
        }

        return true;
    }

    public static void saveMessage(QuotedUserMessage msg, DxApplication app){
        String conversation = !msg.getAddress().equals(app.getHostname()) ?msg.getTo():app.getHostname();
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,false,msg.getQuotedMessage(),msg.getQuoteSender()));
    }

    public static void deleteMessage(QuotedUserMessage msg, DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.beginTransaction();
        try{
            database.execSQL("DELETE FROM message WHERE send_to=? AND sender=? AND msg=? AND created_at=? AND send_from=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt(),msg.getAddress()});
            database.setTransactionSuccessful();
        }catch (Exception e) {e.printStackTrace();}
        finally {
            database.endTransaction();
        }



        try{
            if(msg.getPath()!=null && !msg.getPath().equals("")){
                FileHelper.deleteFile(msg.getPath(),app);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void clearConversation(String address, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        Cursor cr = database.rawQuery("SELECT * FROM message WHERE conversation=?;",new Object[]{address});
        if (cr.moveToFirst()) {
            do {
                QuotedUserMessage message = new QuotedUserMessage(cr.getString(9),
                        cr.getString(8),
                        cr.getString(0),
                        cr.getString(3),
                        cr.getString(4),
                        cr.getLong(5),
                        cr.getInt(7)>0,
                        cr.getString(1),
                        cr.getInt(10)>0,
                        cr.getString(11),
                        cr.getString(12),
                        cr.getString(13));
                deleteMessage(message, app);
            } while (cr.moveToNext());
        }
        cr.close();
    }

    public static int getNumberOfColumns(String tableName, SQLiteDatabase database) {
        Cursor cursor = database.query(tableName, null, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getColumnCount() > 0) {
                int count = cursor.getColumnCount();
                cursor.close();
                return count;
            } else {
                cursor.close();
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

    public static String giveMeQMarks(int qty){
        String marks = "";
        if(qty==0){return marks;}
        for (int i=0;i<qty;i++){
            marks = marks.concat(",?");
        }
        return marks;
    }

    public static void setMessageReceived(QuotedUserMessage msg, DxApplication app, String conversation, boolean received){
        SQLiteDatabase database = app.getDb();
        database.execSQL("UPDATE message SET received=? WHERE send_to=? AND created_at=? AND send_from=? AND msg=? AND conversation=?", new Object[]{received,msg.getTo(),msg.getCreatedAt(),msg.getAddress(),msg.getMessage(),conversation});
    }

    public static void pinMessage(QuotedUserMessage msg, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL("UPDATE message SET pinned = 1 WHERE send_to=? AND sender=? AND msg=? AND created_at=? AND send_from=? AND received=? AND quote=? AND quote_sender=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt(),msg.getAddress(),msg.isReceived(),msg.getQuotedMessage(),msg.getQuoteSender()});
    }

    public static void unPinMessage(QuotedUserMessage msg, DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.execSQL("UPDATE message SET pinned = 0 WHERE send_to=? AND sender=? AND msg=? AND created_at=? AND send_from=? AND received=? AND quote=? AND quote_sender=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt(),msg.getAddress(),msg.isReceived(),msg.getQuotedMessage(),msg.getQuoteSender()});
    }
}
