package com.dx.anonymousmessenger.crypto;

import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.util.Base64;

import net.sqlcipher.database.SQLiteDatabase;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.IOException;
import java.util.List;



public class DxSignalKeyStore implements SignalProtocolStore {
    private IdentityKeyPair ikp;
    private int registrationId;
    private final DxApplication app;

    public DxSignalKeyStore(IdentityKeyPair ikp, int registrationId, DxApplication app){
        this.app = app;
        if(getIdentityKeyPair()==null){
            this.ikp = ikp;
            this.registrationId = registrationId;
            saveIdentityKeyPair();
        }
    }

    @Override
    public void storeSession(SignalProtocolAddress address, SessionRecord record) {
        String addressName=address.getName();
        int deviceId=address.getDeviceId();
        byte[] serializedRecord=record.serialize();

        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS session (address,device_id,session_record)");
        if (containsSession(address)) {
            database.execSQL("DELETE FROM session WHERE address=?", new Object[]{addressName});
        }
        database.execSQL("INSERT INTO session (address,device_id,session_record) VALUES(?,?,?)", new Object[]{addressName,deviceId, serializedRecord});

    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress address) {
        String addressName=address.getName();
        int deviceId=address.getDeviceId();
        //use both of the above variables to get SessionRecord

        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS session (address,device_id,session_record)");
        android.database.Cursor c=database.rawQuery("SELECT * FROM session WHERE address=? AND device_id=?",new Object[]{addressName,deviceId});
        if(c.moveToFirst())
        {
            try {
                SessionRecord sr = new SessionRecord(c.getBlob(2));
                c.close();
                return sr;
            } catch (IOException e) {
                c.close();
                e.printStackTrace();
//                return null;
                return new SessionRecord();
            }
        }else{
            //instead return a new session and save it
            return new SessionRecord();
//            return null;
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress address) {
        //take address.getName() and address.getDeviceId() and check if a row exists in the database or not
        String addressName=address.getName();
        int deviceId=address.getDeviceId();

        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS session (address,device_id,session_record)");
        android.database.Cursor c=database.rawQuery("SELECT session_record FROM session WHERE address=? AND device_id=?",new Object[]{addressName,deviceId});
        if(c.moveToFirst())
        {
            c.close();
            return true;
        }else{
            c.close();
            return false;
        }
    }

    @Override
    public void deleteSession(SignalProtocolAddress address) {
        //take address.getName() and address.getDeviceId() and and delete the row
        String addressName=address.getName();
        int deviceId=address.getDeviceId();

        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS session (address,device_id,session_record)");
        database.execSQL("DELETE FROM session WHERE address=? AND device_id=?",new Object[]{addressName,deviceId});
    }

    @Override
    public void deleteAllSessions(String name) {
        //take name and delete the row
        //wtf is name!!!
        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS session (address,device_id,session_record)");
        database.execSQL("DELETE FROM session");
    }

    public void saveIdentityKeyPair(){
        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS my_identity (identity_key_pair,reg_id)");
        android.database.Cursor c=database.rawQuery("SELECT * FROM my_identity WHERE identity_key_pair=?"
                ,new Object[]{ikp.serialize()});
        if(c.moveToFirst())
        {
//            database.execSQL("UPDATE my_identity SET identity_key_pair=?,reg_id=?", new Object[]{ikp.serialize(),registrationId});
            c.close();
            getIdentityKeyPair();
        }else{
            c.close();
            database.execSQL("INSERT INTO my_identity (identity_key_pair,reg_id) VALUES(?,?)", new Object[]{ikp.serialize(),registrationId});
        }
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS my_identity (identity_key_pair,reg_id)");
        android.database.Cursor c=database.rawQuery("SELECT * FROM my_identity",null);
        if(c.moveToFirst())
        {
            try {
                IdentityKeyPair identityKeyPair = new IdentityKeyPair(c.getBlob(0));
                int registrationId = c.getInt(1);
                c.close();
                ikp = identityKeyPair;
                this.registrationId = registrationId;
                return identityKeyPair;
            } catch (InvalidKeyException e) {
                c.close();
                e.printStackTrace();
                return null;
            }
        }else{
            c.close();
            return null;
        }
        /*
            When you generate your own IdentityKeyPair, store them in a shared preference something like this:

            save(context, IDENTITY_PUBLIC_KEY_PREF, sample_templates.Base64.encodeBytes(djbIdentityKey.serialize()));
            save(context, IDENTITY_PRIVATE_KEY_PREF, sample_templates.Base64.encodeBytes(djbPrivateKey.serialize()));

            and then over here,return them like this:

            IdentityKey publicKey=new IdentityKey(sample_templates.Base64.decode(retrieve(context, IDENTITY_PUBLIC_KEY_PREF)), 0);
            ECPrivateKey privateKey = Curve.decodePrivatePoint(sample_templates.Base64.decode(retrieve(context, IDENTITY_PRIVATE_KEY_PREF)));
            return new IdentityKeyPair(publicKey, privateKey);

            All this stuff is present in IdentityKeyUtil.java file in original repo
        */
    }

    @Override
    public int getLocalRegistrationId() {

        //store the registration Id generated through KeyHelper.generateRegistrationId(); in a SharedPreference and return that here.

        return registrationId;
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        String addressName=address.getName();
        String identityKeyString = Base64.encodeBytes(identityKey.serialize());

        //save these both in the database
        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS identity (address,identity_key)");
        database.execSQL("INSERT INTO identity (address,identity_key) VALUES(?,?)", new Object[]{addressName,identityKeyString});
        return true;
    }

    @Override
    public IdentityKey getIdentity(SignalProtocolAddress address) {

        String serializedIdentity; //get this from the database

        String addressName=address.getName();
        //use addressName to get the serialized Identity from the database
        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS identity (address,identity_key)");
        android.database.Cursor c=database.rawQuery("SELECT identity_key FROM identity WHERE address=?",new Object[]{addressName});
        if(c.moveToFirst())
        {
            serializedIdentity = c.getString(Math.max(c.getColumnIndex("identity_key"),0));
            c.close();

        }else{
            c.close();
            return null;
        }


        IdentityKey identityKey;
        try {
            identityKey=new IdentityKey(Base64.decode(serializedIdentity), 0);
        } catch (InvalidKeyException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return identityKey;
    }

    public boolean removeIdentity(SignalProtocolAddress address){
        SQLiteDatabase database = app.getDb();
        database.execSQL("CREATE TABLE IF NOT EXISTS identity (address,identity_key)");
        database.execSQL("DELETE FROM identity WHERE address=?", new Object[]{address.getName()});
        return true;
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {

        return getIdentity(address) == null || getIdentity(address).getFingerprint().equals(identityKey.getFingerprint());
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {

        //use String name to get the corresponding deviceId

        return null;
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {

        /*
            ECPublicKey  publicKey  = Curve.decodePoint(sample_templates.Base64.decode(<public key from db>, 0);
            ECPrivateKey privateKey = Curve.decodePrivatePoint(sample_templates.Base64.decode(<private key from db>));

            return new PreKeyRecord(keyId, new ECKeyPair(publicKey, privateKey));
        */
        Log.e("same", "PreKey: " );
        return null;
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        String publicKey= Base64.encodeBytes(record.getKeyPair().getPublicKey().serialize());
        String privateKey= Base64.encodeBytes(record.getKeyPair().getPrivateKey().serialize());
        Log.e("same", "PreKey: " );
        //insert preKeyId, publicKey, privateKey into the database
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        //check if the database contains the preKey for the following Id
        Log.e("same", "PreKey: " );
        return false;
    }

    @Override
    public void removePreKey(int preKeyId) {
        Log.e("same", "PreKey: " );
        //delete the prekey from the database for the following Id
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        Log.e("same", "PreKey: " );
        /*
            ECPublicKey  publicKey  = Curve.decodePoint(sample_templates.Base64.decode(<serialized public key from database>)), 0);
            ECPrivateKey privateKey = Curve.decodePrivatePoint(sample_templates.Base64.decode(<serialized private key from database>));
            byte[]       signature  = sample_templates.Base64.decode(<serialized signature from database>);
            long         timestamp  = <timestamp from database>;

            return new SignedPreKeyRecord(keyId, timestamp, new ECKeyPair(publicKey, privateKey), signature);
        */

        return null;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        Log.e("same", "PreKey: " );
        //get all from database and return it, select * from SignedPreKeyDatabase
        return null;
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
//        int keyId=signedPreKeyId;
//        String publicKey= Base64.encodeBytes(record.getKeyPair().getPublicKey().serialize());
//        String privateKey= Base64.encodeBytes(record.getKeyPair().getPrivateKey().serialize());
//        String signature= Base64.encodeBytes(record.getSignature());
//        long timestamp=record.getTimestamp();
        Log.e("same", "PreKey: " );
        //store all these in the database
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        //check if the row of the corresponding signedPreKeyId is present or not and return
        Log.e("same", "PreKey: " );
        return false;
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        //delete row from the database
        Log.e("same", "PreKey: " );
    }
}
