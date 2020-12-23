package com.dx.anonymousmessenger.file;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.util.Hex;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.msopentech.thali.java.toronionproxy.FileUtilities;

import org.whispersystems.libsignal.InvalidKeyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileHelper {
    private static final int IV_LENGTH = 12;

    public static byte[] encrypt(byte[] key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv     = Utils.getSecretBytes(IV_LENGTH);

            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(data);

            return Utils.join(iv, ciphertext);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] decrypt(byte[] key, byte[] data) throws InvalidKeyException {
        try {
            Cipher   cipher     = Cipher.getInstance("AES/GCM/NoPadding");
            byte[][] split      = Utils.split(data, IV_LENGTH, data.length - IV_LENGTH);
            byte[]   iv         = split[0];
            byte[]   cipherText = split[1];

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(cipherText);
        } catch (java.security.InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new InvalidKeyException(e);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e);
        }
    }

    public static CipherInputStream decryptToStream(byte[] key, FileInputStream data){
        try {
            Cipher   cipher     = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            data.read(iv,0,IV_LENGTH);

            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new CipherInputStream(data,cipher);
        } catch (java.security.InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CipherInputStream getFileStream(String path, DxApplication app){
        try{
            //decrypt this shit
            MessageDigest crypt = MessageDigest.getInstance("SHA-256");
            crypt.reset();
            crypt.update(app.getAccount().getPassword());
            byte[] sha1b = crypt.digest();
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return null;
            }
            return decryptToStream(sha1b, new FileInputStream(f));
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static FileInputStream getSharedFileStream(String path, DxApplication app){
        try{
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return null;
            }
            return new FileInputStream(f);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /*
    * this function takes the path and app
    * returns a decrypted byte array representation of the file
    * */
    public static byte[] getFile(String path, DxApplication app){
        try{
            //decrypt this shit
            MessageDigest crypt = MessageDigest.getInstance("SHA-256");
            crypt.reset();
            crypt.update(app.getAccount().getPassword());
            byte[] sha1b = crypt.digest();
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return null;
            }
            byte[] read = FileUtilities.read(f);
            return decrypt(sha1b, read);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static String saveFile(byte[] data, DxApplication app, String filename) throws NoSuchAlgorithmException {
        MessageDigest crypt = MessageDigest.getInstance("SHA-256");
        crypt.reset();
        crypt.update(app.getAccount().getPassword());
        byte[] sha1b = crypt.digest();
        byte[] encrypted = encrypt(sha1b, data);
        String eFilename = Hex.toStringCondensed(encrypt(sha1b,filename.getBytes()));
        try{
            FileOutputStream out = app.openFileOutput(eFilename,Context.MODE_PRIVATE);
            out.write(encrypted);
            out.close();
            //return its "path"
            return eFilename;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteFile(String path, DxApplication app){
        try{
            //no need to use crypto to delete
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return;
            }
            f.delete();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getFileName(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getFileSize(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
