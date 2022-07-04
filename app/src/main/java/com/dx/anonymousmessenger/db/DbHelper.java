package com.dx.anonymousmessenger.db;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.account.DxAccount;
import com.dx.anonymousmessenger.crypto.DxSignalKeyStore;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class DbHelper {
    public static final int LOG_LIMIT = 150;

    /**
     * Contact related
     */

    private static final String CONTACT_SQL_INSERT = "INSERT INTO contact(nickname,address) VALUES(?,?)";
    private static final String CONTACT_COLUMNS = "(nickname,address,unread,profile_image_hash,profile_image_path,sent_profile_image_path)";
    private static final String CONTACT_SQL_UPDATE = "UPDATE contact SET nickname=? WHERE address=?";
    private static final String CONTACT_SQL_UPDATE_PROFILE_IMAGE_HASH = "UPDATE contact SET profile_image_hash=? WHERE address=?";
    private static final String CONTACT_SQL_UPDATE_PROFILE_IMAGE_PATH = "UPDATE contact SET profile_image_path=? WHERE address=?";
    private static final String CONTACT_SQL_UPDATE_SENT_PROFILE_IMAGE_PATH = "UPDATE contact SET sent_profile_image_path=? WHERE address=?";
    private static final String CONTACT_SQL_UPDATE_UNREAD = "UPDATE contact SET unread=? WHERE address=?";
    private static final String CONTACT_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS contact (nickname,address,unread,profile_image_hash,profile_image_path,sent_profile_image_path)";

    private static Object[] getContactSqlValuesUnread(String address){ return new Object[]{true,address}; }

    private static Object[] getContactSqlValues(String address){
        return new Object[]{"",address};
    }

    private static Object[] getContactSqlValues(String address, String nickname){
        return new Object[]{nickname,address};
    }

    public static String getFullAddress(String partialAddress, DxApplication app){
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getFullAddress(partialAddress,app);
        }
        partialAddress+="%";
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        Cursor cr = database.rawQuery("SELECT * FROM contact WHERE address LIKE ?",new String[]{partialAddress});
        if(cr.moveToFirst()){
            return cr.getString(1);
        }else{
            return null;
        }
    }

    public static List<String[]> getContactsList(DxApplication app) {
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getContactsList(app);
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
                    }else if(message.getType()!=null && message.getType().equals("file")){
                        msg = app.getString(R.string.file);
                    }else if(message.getType()!=null && message.getType().equals("call")){
                        msg = app.getString(R.string.dx_call);
                    }else{
                        msg = message.getMessage();
                    }

                    contacts.add(new String[]{cr.getString(0), address, cr.getInt(2) > 0 ? "unread" : "read",msg,message.getTo(), String.valueOf(message.getCreatedAt()),message.isReceived()?"true":"false", cr.getString(4)});
                }else{
                    cr2.close();
                    contacts.add(new String[]{cr.getString(0), address, cr.getInt(2) > 0 ? "unread" : "read","","","","", cr.getString(4)});
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
        if (app.getAccount()==null){
            return false;
        }

        SQLiteDatabase database = app.getDb();

        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return saveContact(address,app);
        }

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
        if (app.getAccount()==null){
            return false;
        }

        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return saveContact(address,nickname,app);
        }
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
        if (app.getAccount()==null){
            return;
        }
        clearConversation(address,app);
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            deleteContact(address,app);
            return;
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        database.execSQL("DELETE FROM contact WHERE address=?",new Object[]{address});
        app.getEntity().getStore().deleteSession(new SignalProtocolAddress(address,1));
        ((DxSignalKeyStore)app.getEntity().getStore()).removeIdentity(new SignalProtocolAddress(address,1));
    }

    public static void setContactNickname(String nickname, String address, DxApplication app){
        if (app.getAccount()==null){
            return;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setContactNickname(nickname,address,app);
            return;
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE,DbHelper.getContactSqlValues(address,nickname));
        }
        else
        {
            c.close();
        }
    }

    public static String getContactNickname(String address, DxApplication app) {
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getContactNickname(address,app);
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            @SuppressLint("Range") String nn = c.getString(c.getColumnIndex("nickname"));
            c.close();
            return nn;
        }
        else
        {
            c.close();
            return null;
        }
    }

    public static void setContactProfileImageHash(String profile_image_hash, String address, DxApplication app){
        if (app.getAccount()==null){
            return;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setContactProfileImageHash(profile_image_hash,address,app);
            return;
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_PROFILE_IMAGE_HASH,DbHelper.getContactSqlValues(address,profile_image_hash));
        }
        else
        {
            c.close();
        }
    }

    public static String getContactProfileImageHash(String address, DxApplication app) {
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getContactProfileImageHash(address,app);
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            @SuppressLint("Range") String nn = c.getString(c.getColumnIndex("profile_image_hash"));
            c.close();
            return nn;
        }
        else
        {
            c.close();
            return null;
        }
    }

    public static void setContactProfileImagePath(String profile_image_path, String address, DxApplication app){
        if (app.getAccount()==null){
            return;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setContactProfileImagePath(profile_image_path,address,app);
            return;
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_PROFILE_IMAGE_PATH,DbHelper.getContactSqlValues(address,profile_image_path));
        }
        else
        {
            c.close();
        }
    }

    public static String getContactProfileImagePath(String address, DxApplication app) {
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getContactProfileImagePath(address,app);
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            @SuppressLint("Range") String nn = c.getString(c.getColumnIndex("profile_image_path"));
            c.close();
            return nn;
        }
        else
        {
            c.close();
            return null;
        }
    }

    public static void setContactSentProfileImagePath(String sent_profile_image_path, String address, DxApplication app){
        if (app.getAccount()==null){
            return;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setContactSentProfileImagePath(sent_profile_image_path,address,app);
            return;
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_SENT_PROFILE_IMAGE_PATH,DbHelper.getContactSqlValues(address,sent_profile_image_path));
        }
        else
        {
            c.close();
        }
    }

    public static String getContactSentProfileImagePath(String address, DxApplication app) {
        if (app.getAccount()==null){
            return null;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getContactSentProfileImagePath(address,app);
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            @SuppressLint("Range") String nn = c.getString(c.getColumnIndex("sent_profile_image_path"));
            c.close();
            return nn;
        }
        else
        {
            c.close();
            return null;
        }
    }

    public static void setContactUnread(String address, DxApplication app) {
        if (app.getAccount()==null){
            return;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setContactUnread(address,app);
            return;
        }
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery(CONTACT_SQL_UPDATE_UNREAD, getContactSqlValuesUnread(address));
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_UNREAD,DbHelper.getContactSqlValuesUnread(address));
        }
        else
        {
            c.close();
        }

    }

    public static boolean setContactRead(String address, DxApplication app) {
        if (app.getAccount()==null){
            return false;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return setContactRead(address,app);
        }
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

    private static void setContactRead(String address, SQLiteDatabase database) {
        database.execSQL(DbHelper.CONTACT_TABLE_SQL_CREATE);
        android.database.Cursor c=database.rawQuery("SELECT * FROM contact WHERE address=?", new Object[]{address});
        if(c.moveToFirst())
        {
            c.close();
            database.execSQL(DbHelper.CONTACT_SQL_UPDATE_UNREAD,new Object[]{0,address});
        }
        else
        {
            c.close();
        }
    }

    public static boolean contactExists(String address, DxApplication app){
        if (app.getAccount()==null){
            return false;
        }
        SQLiteDatabase database = app.getDb();
        if (database.isDbLockedByOtherThreads()){
//            throw new IOException("DB locked, try again in a few mills");
            try {
                Thread.sleep(150);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return contactExists(address,app);
        }
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

    /**
     *  Settings related
     */

    private static String getSettingsColumns(){
        return "(bridgesEnabled,isAcceptingUnknownContactsEnabled,isAcceptingCallsAllowed,isReceivingFilesAllowed,checkAddress,fileSizeLimit,enableSocks5Proxy,socks5AddressAndPort,socks5Username,socks5Password,excludeText,excludeUnknown,strictExclude)";
    }

    private static String getSettingsTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS settings "+getSettingsColumns()+";";
    }

    private static String getSettingsSqlInsert(){
        return "INSERT INTO settings "+getSettingsColumns()+" VALUES(?"+giveMeQMarks(getSettingsColumns().split(",").length-1)+")";
    }

    private static Object[] getSettingsSqlValues(boolean bridgesEnabled, boolean isAcceptingUnknownContactsEnabled, boolean isAcceptingCallsAllowed, boolean isReceivingFilesAllowed, String checkAddress, String fileSizeLimit, boolean enableSocks5Proxy, String socks5AddressAndPort, String socks5Username, String socks5Password, String excludeText, boolean excludeUnknown, boolean strictExclude){
        return new Object[]{bridgesEnabled?1:0,isAcceptingUnknownContactsEnabled?1:0,isAcceptingCallsAllowed?1:0,isReceivingFilesAllowed?1:0,checkAddress,fileSizeLimit,enableSocks5Proxy?1:0,socks5AddressAndPort,socks5Username,socks5Password,excludeText,excludeUnknown?1:0,strictExclude?1:0};
    }

    public static void saveSettings(boolean bridgesEnabled, boolean isAcceptingUnknownContactsEnabled, boolean isAcceptingCallsAllowed, boolean isReceivingFilesAllowed, String checkAddress, String fileSizeLimit, boolean enableSocks5Proxy, String socks5AddressAndPort, String socks5Username, String socks5Password, String excludeText, boolean excludeUnknown, boolean strictExclude, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL(DbHelper.getSettingsSqlInsert(),DbHelper.getSettingsSqlValues(bridgesEnabled,isAcceptingUnknownContactsEnabled,isAcceptingCallsAllowed,isReceivingFilesAllowed,checkAddress,fileSizeLimit,enableSocks5Proxy,socks5AddressAndPort,socks5Username,socks5Password,excludeText,excludeUnknown,strictExclude));
    }

    public static void saveExcludeText(String excludeText, DxApplication app){
        //process text to add "{" and "}" for torrc
        String[] raw = excludeText.split(",");
        String processed = "";
        for (String v: raw){
            if(v.startsWith("{") && v.endsWith("}")){
                continue;
            }
            processed = processed.concat("{"+v+"},");
        }
        if(processed.endsWith(",")){
            processed = processed.substring(0,processed.length()-1);
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET excludeText=?", new Object[]{processed});
    }

    public static void saveExcludeUnknown(boolean excludeUnknown, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET excludeUnknown=?", new Object[]{excludeUnknown?1:0});
    }

    public static void saveStrictExclude(boolean strictExclude, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET strictExclude=?", new Object[]{strictExclude?1:0});
    }

    public static void saveSocks5Password(String socks5Password, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET socks5Password=?", new Object[]{socks5Password});
    }

    public static void saveSocks5Username(String socks5Username, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET socks5Username=?", new Object[]{socks5Username});
    }

    public static void saveSocks5AddressAndPort(String socks5AddressAndPort, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET socks5AddressAndPort=?", new Object[]{socks5AddressAndPort});
    }

    public static void saveEnableSocks5Proxy(boolean enableSocks5Proxy, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET enableSocks5Proxy=?", new Object[]{enableSocks5Proxy?1:0});
    }

    public static void saveEnableBridges(boolean bridgesEnabled, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET bridgesEnabled=?", new Object[]{bridgesEnabled?1:0});
    }

    public static void saveIsAcceptingUnknownContactsEnabled(boolean isAcceptingUnknownContactsEnabled, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET isAcceptingUnknownContactsEnabled=?", new Object[]{isAcceptingUnknownContactsEnabled?1:0});
    }

    public static void saveIsAcceptingCallsAllowed(boolean isAcceptingCallsAllowed, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET isAcceptingCallsAllowed=?", new Object[]{isAcceptingCallsAllowed?1:0});
    }

    public static void saveIsReceivingFilesAllowed(boolean isReceivingFilesAllowed, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET isReceivingFilesAllowed=?", new Object[]{isReceivingFilesAllowed?1:0});
    }

    public static void saveCheckAddress(String checkAddress, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET checkAddress=?", new Object[]{checkAddress});
    }

    public static void saveFileSizeLimit(String fileSizeLimit, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        database.execSQL("UPDATE settings SET fileSizeLimit=?", new Object[]{fileSizeLimit});
    }

    public static void deleteSettings(DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.beginTransaction();
        try{
            database.execSQL("DELETE FROM settings");
            database.setTransactionSuccessful();
        }catch (Exception e) {e.printStackTrace();}
        finally {
            database.endTransaction();
        }
    }

    public static Object[] getSettingsList(DxApplication app, boolean forUi){
        Object[] o = getSettingsList(app);
        //process text to remove {&}
        if (o != null) {
            o[10] = ((String)o[10]).replace("{","").replace("}","");
        }
        return o;
    }

    public static Object[] getSettingsList(DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getSettingsTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getSettingsColumns().trim().substring(1,getSettingsColumns().length()-1)+" FROM settings");
        }catch (Exception e){
            Log.w("getSettingsList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM settings ;",null);
        Object[] settings = null;
        if (cr.moveToFirst()) {
            settings = new Object[13];
            settings[0] = cr.getInt(0);//enable bridges
            settings[1] = cr.getInt(1);//isAcceptingUnknownContacts
            settings[2] = cr.getInt(2);//isAcceptingCallsAllowed
            settings[3] = cr.getInt(3);//isReceivingFilesAllowed
            settings[4] = cr.getString(4);//checkAddress
            settings[5] = cr.getString(5);//fileSizeLimit
            settings[6] = cr.getInt(6);//enableSocks5Proxy
            settings[7] = cr.getString(7);//socks5AddressAndPort
            settings[8] = cr.getString(8);//socks5Username
            settings[9] = cr.getString(9);//socks5Password
            settings[10] = cr.getString(10);//excludeText
            settings[11] = cr.getInt(11);//excludeUnknown
            settings[12] = cr.getInt(12);//strictExclude
        }
        cr.close();

        return settings;
    }

    /**
     * Bridge related
     */

    private static String getBridgeColumns(){
        return "(line)";
    }

    private static String getBridgeTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS bridge "+getBridgeColumns()+";";
    }

    private static String getBridgeSqlInsert(){
        return "INSERT INTO bridge "+getBridgeColumns()+" VALUES(?"+giveMeQMarks(getBridgeColumns().split(",").length-1)+")";
    }

    private static Object[] getBridgeSqlValues(String line){
        return new Object[]{line};
    }

    public static void saveBridge(String line, DxApplication app){
        if(line.startsWith("bridge ") || line.startsWith("Bridge ") || line.startsWith("BRIDGE ")){
            line = line.substring(6);
        }
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getBridgeTableSqlCreate());
        database.execSQL(DbHelper.getBridgeSqlInsert(),DbHelper.getBridgeSqlValues(line));
    }

    public static void deleteBridge(String line, DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.beginTransaction();
        try{
            database.execSQL("DELETE FROM bridge WHERE line=?", new Object[]{line});
            database.setTransactionSuccessful();
        }catch (Exception e) {e.printStackTrace();}
        finally {
            database.endTransaction();
        }
    }

    public static List<String> getBridgeList(DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getBridgeTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getBridgeColumns().trim().substring(1,getBridgeColumns().length()-1)+" FROM bridge");
        }catch (Exception e){
            Log.w("getBridgeList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM bridge ;",null);
        List<String> bridges = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                bridges.add(cr.getString(0));
            } while (cr.moveToNext());
        }
        cr.close();
        return bridges;
    }

    /**
     * Log related
     */

    private static String getLogColumns(){
        return "(msg,created_at,type)";
    }

    private static String getLogTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS log "+getLogColumns()+";";
    }

    private static String getLogSqlInsert(){
        return "INSERT INTO log "+getLogColumns()+" VALUES(?"+giveMeQMarks(getLogColumns().split(",").length-1)+")";
    }

    private static Object[] getLogSqlValues(String msg, long createdAt, String type){
        return new Object[]{msg,createdAt,type};
    }

    public static void saveLog(String msg, long createdAt, String type, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getLogTableSqlCreate());
        database.execSQL(DbHelper.getLogSqlInsert(),DbHelper.getLogSqlValues(msg,createdAt,type));
    }

    public static void deleteLog(long createdAt, String msg, DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.beginTransaction();
        try{
            database.execSQL("DELETE FROM log WHERE created_at=? AND msg=?", new Object[]{createdAt,msg});
            database.setTransactionSuccessful();
        }catch (Exception e) {e.printStackTrace();}
        finally {
            database.endTransaction();
        }
    }

    public static void deleteAllLogs(DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.beginTransaction();
        try{
            database.execSQL("DELETE FROM log",new Object[]{});
            database.setTransactionSuccessful();
        }catch (Exception e) {e.printStackTrace();}
        finally {
            database.endTransaction();
        }
    }

    public static List<Object[]> getLogList(DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getLogTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getLogColumns().trim().substring(1,getLogColumns().length()-1)+" FROM log");
        }catch (Exception e){
            Log.w("getLogList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM log WHERE type<>? ;",new Object[]{"NOTICE"});
        List<Object[]> logs = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                if ((new Date().getTime() - cr.getLong(1)) < app.getTime2delete()) {
                    logs.add(new Object[]{
                            cr.getString(0),
                            cr.getLong(1),
                            cr.getString(2)
                    });
                }else{
                    deleteLog(cr.getLong(1),cr.getString(0),app);
                }

            } while (cr.moveToNext());
        }
        cr.close();
        return logs;
    }

    public static List<Object[]> getLogListWithNotice(DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getLogTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getLogColumns().trim().substring(1,getLogColumns().length()-1)+" FROM log");
        }catch (Exception e){
            Log.w("getLogList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM log ;",null);
        List<Object[]> logs = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                if ((new Date().getTime() - cr.getLong(1)) < app.getTime2delete()) {
                    logs.add(new Object[]{
                            cr.getString(0),
                            cr.getLong(1),
                            cr.getString(2)
                    });
                }else{
                    deleteLog(cr.getLong(1),cr.getString(0),app);
                }

            } while (cr.moveToNext());
        }
        cr.close();
        return logs;
    }

    public static void reduceLog(DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getLogTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getLogColumns().trim().substring(1,getLogColumns().length()-1)+" FROM log");
        }catch (Exception e){
            Log.w("getLogList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        int count = getNumberOfRows("log",database);
        if(count>LOG_LIMIT){
            database.execSQL("DELETE FROM log WHERE rowid IN ( SELECT rowid FROM log LIMIT "+(count-LOG_LIMIT)+" )");
        }
    }

    /**
     * Notepad related
     */

    private static String getNotepadColumns(){
        return "(msg,created_at,filename,path,type)";
    }

    private static String getNotepadTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS notepad "+getNotepadColumns()+";";
    }

    private static String getNotepadSqlInsert(){
        return "INSERT INTO notepad "+getNotepadColumns()+" VALUES(?"+giveMeQMarks(getNotepadColumns().split(",").length-1)+")";
    }

    private static Object[] getNotepadSqlValues(String msg, long createdAt, String filename, String path, String type){
        return new Object[]{msg,createdAt,filename,path,type};
    }

    public static void saveNote(String msg, long createdAt, String filename, String path, String type, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getNotepadTableSqlCreate());
        database.execSQL(DbHelper.getNotepadSqlInsert(),DbHelper.getNotepadSqlValues(msg,createdAt,filename,path,type));
    }

    public static void deleteNote(long createdAt, DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.beginTransaction();
        try{
            database.execSQL("DELETE FROM notepad WHERE created_at=?", new Object[]{createdAt});
            database.setTransactionSuccessful();
        }catch (Exception e) {e.printStackTrace();}
        finally {
            database.endTransaction();
        }
    }

    public static List<Object[]> getNotepadList(DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getNotepadTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getNotepadColumns().trim().substring(1,getNotepadColumns().length()-1)+" FROM notepad");
        }catch (Exception e){
            Log.w("getNotepadList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM notepad ;",null);
        List<Object[]> notes = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                notes.add(new Object[]{cr.getString(0),
                        cr.getLong(1),
                        cr.getString(2),
                        cr.getString(3),
                        cr.getLong(4)});
            } while (cr.moveToNext());
        }
        cr.close();
        return notes;
    }

    /**
     * Message related
     */

    private static String getMessageTableSqlCreate(){
        return "CREATE TABLE IF NOT EXISTS message "+getMessageColumns()+";";
    }

    private static String getMessageSqlInsert(){
        return "INSERT INTO message "+getMessageColumns()+" VALUES(?"+giveMeQMarks(getMessageColumns().split(",").length-1)+")";
    }

    private static String getMessageColumns(){
        return "(send_from,send_to,number,msg,sender,created_at,conversation,received,quote,quote_sender,pinned,filename,path,type)";
    }

    private static Object[] getMessageSqlValues(String from, String to, String number, String msg, String sender, long createdAt, String conversation, boolean received, String quote, String quoteSender){
        return new Object[]{from,to,number,msg,sender,createdAt,conversation,received,quote,quoteSender,false,null,null,null};
    }

    @SuppressWarnings("SameParameterValue")
    private static Object[] getFullMessageSqlValues(String from, String to, String number, String msg, String sender, long createdAt, String conversation, boolean received, String quote, String quoteSender, String filename, String path, String type){
        return new Object[]{from,to,number,msg,sender,createdAt,conversation,received,quote,quoteSender,false,filename,path,type};
    }

//    private static Object[] getMediaMessageSqlValues(String from, String to, String number, String sender, long createdAt, String conversation, boolean received, String filename, String path, String type){
//        return new Object[]{from,to,number,"",sender,createdAt,conversation,received,"","",false,filename,path,type};
//    }

    public static List<QuotedUserMessage> getMessageList(DxApplication app, String conversation){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getMessageColumns().trim().substring(1,getMessageColumns().length()-1)+" FROM message LIMIT 1");
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

    public static void deleteOldMessages(DxApplication app, String conversation){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getMessageColumns().trim().substring(1,getMessageColumns().length()-1)+" FROM message LIMIT 1");
//            database.query("SELECT "+DbHelper.getMessageColumns()+" FROM message");
        }catch (Exception e){
            Log.w("getMessageList", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT * FROM message WHERE conversation=? AND pinned=0 AND created_at<?;",new Object[]{conversation,(new Date().getTime() - app.getTime2delete())});
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
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("delete",message.getCreatedAt());
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
            } while (cr.moveToNext());
        }
        cr.close();
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

                //commented out to allow resending without having to wait
//                if ((new Date().getTime() - message.getCreatedAt()) < (60*1000)) {
//                    continue;
//                }

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
//                    if(message.getAddress().equals(app.getHostname()) && message.isProfileImage() && !message.isReceived()){
//
//                        continue;
//                    }
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

    public static void saveMessage(QuotedUserMessage msg, DxApplication app, String conversation, boolean received){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        if(msg.getType()==null || msg.getType().equals("")){
            database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getQuotedMessage(),msg.getQuoteSender()));
        }else{
            database.execSQL(DbHelper.getMessageSqlInsert(),DbHelper.getFullMessageSqlValues(msg.getAddress(),msg.getTo(),"1",msg.getMessage(),msg.getSender(),msg.getCreatedAt(),conversation,received,msg.getQuotedMessage(),msg.getQuoteSender(),msg.getFilename(),msg.getPath(),msg.getType()));
        }

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
            if(msg.getAddress()!=null){
                database.execSQL("DELETE FROM message WHERE send_to=? AND sender=? AND msg=? AND created_at=? AND send_from=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt(),msg.getAddress()});
            }else{
                database.execSQL("DELETE FROM message WHERE send_to=? AND sender=? AND msg=? AND created_at=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt()});
            }
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

    public static void setMessageReceived(QuotedUserMessage msg, DxApplication app, String conversation, boolean received){
        SQLiteDatabase database = app.getDb();
        database.execSQL("UPDATE message SET received=? WHERE send_to=? AND created_at=? AND send_from=? AND msg=? AND conversation=?", new Object[]{received,msg.getTo(),msg.getCreatedAt(),msg.getAddress(),msg.getMessage(),conversation});
    }

    public static boolean getMessageReceived(QuotedUserMessage msg, DxApplication app, String conversation){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        try{
            database.query("SELECT "+DbHelper.getMessageColumns().trim().substring(1,getMessageColumns().length()-1)+" FROM message");
        }catch (Exception e){
            Log.w("getMessageReceived", "updating DbSchema");
            e.printStackTrace();
            updateDbSchema(database);
        }
        Cursor cr = database.rawQuery("SELECT received FROM message WHERE send_to=? AND created_at=? AND send_from=? AND msg=? AND conversation=?", new Object[]{msg.getTo(),msg.getCreatedAt(),msg.getAddress(),msg.getMessage(),conversation});
        boolean received = false;
        if (cr.moveToFirst()) {
            do {
                received = cr.getInt(0)>0;
            } while (cr.moveToNext());
        }
        cr.close();
        return received;
    }

    public static void pinMessage(QuotedUserMessage msg, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL("UPDATE message SET pinned = 1 WHERE send_to=? AND sender=? AND msg=? AND created_at=? AND send_from=? AND received=? AND quote=? AND quote_sender=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt(),msg.getAddress(),msg.isReceived(),msg.getQuotedMessage(),msg.getQuoteSender()});
    }

    public static void unPinMessage(QuotedUserMessage msg, DxApplication app) {
        SQLiteDatabase database = app.getDb();
        database.execSQL("UPDATE message SET pinned = 0 WHERE send_to=? AND sender=? AND msg=? AND created_at=? AND send_from=? AND received=? AND quote=? AND quote_sender=?", new Object[]{msg.getTo(),msg.getSender(),msg.getMessage(),msg.getCreatedAt(),msg.getAddress(),msg.isReceived(),msg.getQuotedMessage(),msg.getQuoteSender()});
    }

    public static List<String> getAllReferencedFiles(DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
//        try{
//            //use a latest query that is only in the latest schema here
//            database.query("SELECT profile_image_path FROM contact");
//        }catch (Exception e){
//            Log.w("getAllReferencedFiles", "updating DbSchema");
//            e.printStackTrace();
            updateDbSchema(database);
//        }
        Cursor cr = database.rawQuery("SELECT path FROM message",null);
        List<String> paths = new ArrayList<>();
        if (cr.moveToFirst()) {
            do {
                paths.add(cr.getString(0));
            } while (cr.moveToNext());
        }
        cr.close();
        //also add the profile picture file
        database.execSQL(DxAccount.CREATE_ACCOUNT_TABLE_SQL);
        Cursor cr2 = database.rawQuery("SELECT profile_image_path FROM account",null);
        if (cr2.moveToFirst()) {
            do {
                paths.add(cr2.getString(0));
            } while (cr2.moveToNext());
        }
        cr2.close();
        //also add the contacts profile image files
        database.execSQL(DxAccount.CREATE_ACCOUNT_TABLE_SQL);
        Cursor cr3 = database.rawQuery("SELECT profile_image_path FROM contact",null);
        if (cr3.moveToFirst()) {
            do {
                paths.add(cr3.getString(0));
            } while (cr3.moveToNext());
        }
        cr3.close();

        return paths;
    }

    public static void clearConversation(String address, DxApplication app){
        SQLiteDatabase database = app.getDb();
        database.execSQL(DbHelper.getMessageTableSqlCreate());
        Cursor cr = database.rawQuery("SELECT * FROM message WHERE conversation=?;",new Object[]{address});
        if (cr.moveToFirst()) {
            do {
                try{
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
                }catch (Exception e){
                e.printStackTrace();
            }
            } while (cr.moveToNext());
        }
        cr.close();
    }

    /**
     * General & utils
     */

    private static void updateDbSchema(SQLiteDatabase database) {
        int tries = 0;
        Log.d("starting the db update","same");
        while(tries<2){
            tries++;
            try{
                database.beginTransaction();
                String tmpName = "temp_message ";
                String createTemp = getMessageTableSqlCreate().replace("message",tmpName);
                database.execSQL(createTemp);
                String createIfNotExist = getMessageTableSqlCreate();
                database.execSQL(createIfNotExist);
                int tmpColNum = getNumberOfColumns(tmpName,database);
                int oldColNum = getNumberOfColumns("message",database);
                String emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                String colNames = getColumnNames("message",database);
                database.execSQL("INSERT INTO "+tmpName+getMessageColumns()+" SELECT "+colNames+emptyCols+" FROM message;");
                database.execSQL("DROP TABLE message;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO message");
                database.setTransactionSuccessful();
                database.endTransaction();

                database.beginTransaction();
                tmpName = "temp_contact ";
                createTemp = CONTACT_TABLE_SQL_CREATE.replace("contact",tmpName);
                database.execSQL(createTemp);
                createIfNotExist = CONTACT_TABLE_SQL_CREATE;
                database.execSQL(createIfNotExist);
                tmpColNum = getNumberOfColumns(tmpName,database);
                oldColNum = getNumberOfColumns("contact",database);
                emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                colNames = getColumnNames("contact",database);
                database.execSQL("INSERT INTO "+tmpName+CONTACT_COLUMNS+" SELECT "+colNames+emptyCols+" FROM contact;");
                database.execSQL("DROP TABLE contact;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO contact");
                database.setTransactionSuccessful();
                database.endTransaction();

                database.beginTransaction();
                tmpName = "temp_notepad ";
                createTemp = getNotepadTableSqlCreate().replace("notepad",tmpName);
                database.execSQL(createTemp);
                createIfNotExist = getNotepadTableSqlCreate();
                database.execSQL(createIfNotExist);
                tmpColNum = getNumberOfColumns(tmpName,database);
                oldColNum = getNumberOfColumns("notepad",database);
                emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                colNames = getColumnNames("notepad",database);
                database.execSQL("INSERT INTO "+tmpName+getNotepadColumns()+" SELECT "+colNames+emptyCols+" FROM notepad;");
                database.execSQL("DROP TABLE notepad;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO notepad");
                database.setTransactionSuccessful();
                database.endTransaction();

                database.beginTransaction();
                tmpName = "temp_log ";
                createTemp = getLogTableSqlCreate().replace("log",tmpName);
                database.execSQL(createTemp);
                createIfNotExist = getLogTableSqlCreate();
                database.execSQL(createIfNotExist);
                tmpColNum = getNumberOfColumns(tmpName,database);
                oldColNum = getNumberOfColumns("log",database);
                emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                colNames = getColumnNames("log",database);
                database.execSQL("INSERT INTO "+tmpName+getLogColumns()+" SELECT "+colNames+emptyCols+" FROM log;");
                database.execSQL("DROP TABLE log;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO log");
                database.setTransactionSuccessful();
                database.endTransaction();

                database.beginTransaction();
                tmpName = "temp_bridge ";
                createTemp = getBridgeTableSqlCreate().replace("bridge",tmpName);
                database.execSQL(createTemp);
                createIfNotExist = getBridgeTableSqlCreate();
                database.execSQL(createIfNotExist);
                tmpColNum = getNumberOfColumns(tmpName,database);
                oldColNum = getNumberOfColumns("bridge",database);
                emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                colNames = getColumnNames("bridge",database);
                database.execSQL("INSERT INTO "+tmpName+getBridgeColumns()+" SELECT "+colNames+emptyCols+" FROM bridge;");
                database.execSQL("DROP TABLE bridge;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO bridge");
                database.setTransactionSuccessful();
                database.endTransaction();

                database.beginTransaction();
                tmpName = "temp_settings ";
                createTemp = getSettingsTableSqlCreate().replace("settings",tmpName);
                database.execSQL(createTemp);
                createIfNotExist = getSettingsTableSqlCreate();
                database.execSQL(createIfNotExist);
                tmpColNum = getNumberOfColumns(tmpName,database);
                oldColNum = getNumberOfColumns("settings",database);
                emptyCols = tmpColNum>oldColNum?giveMeNulls(tmpColNum-oldColNum):"";
                colNames = getColumnNames("settings",database);
                database.execSQL("INSERT INTO "+tmpName+getSettingsColumns()+" SELECT "+colNames+emptyCols+" FROM settings;");
                database.execSQL("DROP TABLE settings;");
                database.execSQL("ALTER TABLE "+tmpName+" RENAME TO settings");
                database.setTransactionSuccessful();
                database.endTransaction();

                Log.d("finishing the db update","good bye");
            }catch (SQLiteException e){
                Log.e("ERROR UPDATING DB","THERE WAS AN ERROR UPDATING THE D B YO!");
                e.printStackTrace();
            }
        }
    }

    private static String getColumnNames(String table, SQLiteDatabase database) {
        Cursor dbCursor = database.query(table, null, null, null, null, null, null);
        String[] columnNames = dbCursor.getColumnNames();
        StringBuilder result = new StringBuilder();
        for (String col:columnNames) {
            result.append(col).append(",");
        }
        if(result.length()>0){
            result.deleteCharAt(result.length()-1);
        }
        return result.toString();
    }

    private static int getNumberOfRows(@SuppressWarnings("SameParameterValue") String tableName, SQLiteDatabase database) {
        Cursor cursor = database.query(tableName, null, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                int count = cursor.getCount();
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

    private static int getNumberOfColumns(String tableName, SQLiteDatabase database) {
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

    private static String giveMeNulls(int qty){
        String nulls = "";
        if(qty==0){return nulls;}
        for (int i=0;i<qty;i++){
            nulls = nulls.concat(",null");
        }
        return nulls;
    }

    private static String giveMeQMarks(int qty){
        String marks = "";
        if(qty==0){return marks;}
        for (int i=0;i<qty;i++){
            marks = marks.concat(",?");
        }
        return marks;
    }

}
