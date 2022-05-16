package com.dx.anonymousmessenger.file;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.RecoverySystem;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.util.Hex;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.msopentech.thali.java.toronionproxy.FileUtilities;

import org.whispersystems.libsignal.InvalidKeyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileHelper {
    public static final int IV_LENGTH = 12;

    /**
    * This function deletes a file from the app's internal storage
    * */
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

    /*
     * this part is for dealing with bytes
     * */

    /**
    * This function encrypts a byte array and returns the result
    * */
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


    /**
    * This function decrypts a byte array and returns the result
    * */
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

    /**
    * this function takes the path and app
    * returns a decrypted byte array representation of the file
    * */
    public static byte[] getFile(String path, DxApplication app){
        try{
            //decrypt this stuff
            byte[] sha1b = app.getSha256();
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return null;
            }
            byte[] read = FileUtilities.read(f);
            return decrypt(sha1b, read);
        }catch (Exception e){
//            e.printStackTrace();
        }
        return null;
    }


    /**
    * This function encrypts and saves a byte array into a file and returns it's path
    * */
    public static String saveFile(byte[] data, DxApplication app, String filename) throws NoSuchAlgorithmException {
        byte[] sha1b = app.getSha256();
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

    /*
     * this part deals with streaming
     * */

    /**
    * This function returns a raw FIS from encrypted app storage using the provided path
    * */
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

    /**
    * This function encrypts and saves a file from shared storage while reporting progress to a progressListener
    * */
    public static void saveToStorageWithProgress(String path, String filename, DxApplication app, RecoverySystem.ProgressListener progressListener){
        try{
            //decrypt this stuff
            byte[] sha1b = app.getSha256();
            File f = new File(app.getFilesDir(),path);
            String suffix = "."+filename.split("\\.")[filename.split("\\.").length-1];
            //to work on android 11 we have to save file to the downloads directory
            //on android 10 we rely on requestLegacyExternalStorage in manifest
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/AM",filename);
            Objects.requireNonNull(file.getParentFile()).mkdir();
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();
            Cipher   cipher     = Cipher.getInstance("AES/GCM/NoPadding");
            try (FileInputStream fis = new FileInputStream(f); FileOutputStream out = new FileOutputStream(file)) {
                progressListener.onProgress(0);
                int done = 0;
                while (true) {
                    byte[] iv = new byte[IV_LENGTH];
                    fis.read(iv, 0, IV_LENGTH);
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha1b, "AES"), new GCMParameterSpec(128, iv));
                    byte[] chunkSize = new byte[4];
                    fis.read(chunkSize, 0, chunkSize.length);
                    int casted = ByteBuffer.wrap(chunkSize).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    if (casted == 0) {
                        break;
                    }
                    byte[] buf = new byte[casted];
                    int read;
                    if (f.length() - done >= buf.length) {
                        read = fis.read(buf, 0, buf.length);
                    } else {
                        read = fis.read(buf);
                    }
                    if (read == -1) {
                        break;
                    }
                    out.write(cipher.doFinal(buf, 0, read));
                    out.flush();
                    done = done + read;
                    progressListener.onProgress(((int) (((double) done / (double) f.length()) * 100.0)));
                }
                progressListener.onProgress(100);
            }

            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix.replace(".",""));
            if(mime==null){
                mime = "*/*";
            }

            DownloadManager downloadManager = (DownloadManager) app.getSystemService(DOWNLOAD_SERVICE);
            downloadManager.addCompletedDownload(file.getName(), file.getName(), true, mime,file.getAbsolutePath(),file.length(),true);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
    * This function decrypts and saves a temp file using a file from the app's storage to be used by a third party app while reporting progress to a progressListener
    * */
    public static File getTempFileWithProgress(String path, String filename, DxApplication app, RecoverySystem.ProgressListener progressListener){
        try{
            //decrypt this stuff
            byte[] sha1b = app.getSha256();
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return null;
            }
            String suffix = "."+filename.split("\\.")[filename.split("\\.").length-1];
            if(new File(app.getExternalCacheDir(),filename).exists()){
                return new File(app.getExternalCacheDir(),filename);
            }
            cleanDir(Objects.requireNonNull(app.getExternalCacheDir()));
            cleanDir(Objects.requireNonNull(app.getCacheDir()));
            File tmp = File.createTempFile(filename.replace(suffix,""),suffix,app.getExternalCacheDir());
            Cipher   cipher     = Cipher.getInstance("AES/GCM/NoPadding");
            try (FileInputStream fis = new FileInputStream(f); FileOutputStream out = new FileOutputStream(tmp)) {
                progressListener.onProgress(0);
                int done = 0;
                while (true) {
                    byte[] iv = new byte[IV_LENGTH];
                    fis.read(iv, 0, IV_LENGTH);
//                    Log.d("ANONYMOUSMESSENGER","IV: "+ Arrays.toString(iv));
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha1b, "AES"), new GCMParameterSpec(128, iv));
                    byte[] chunkSize = new byte[4];
                    fis.read(chunkSize, 0, chunkSize.length);
                    int casted = ByteBuffer.wrap(chunkSize).order(ByteOrder.LITTLE_ENDIAN).getInt();
//                    Log.d("ANONYMOUSMESSENGER","length of next chapter: "+casted);
                    if (casted == 0) {
                        break;
                    }
                    byte[] buf = new byte[casted];
                    int read;
//                    Log.d("ANONYMOUSMESSENGER","avail: "+fis.available());
                    if (f.length() - done >= buf.length) {
//                        Log.d("ANONYMOUSMESSENGER","a lot more is left");
//                        while(fis.available()<buf.length){
//                            Log.d("ANONYMOUSMESSENGER","waiting for availability");
//                        }
                        read = fis.read(buf, 0, buf.length);
                    } else {
//                        Log.d("ANONYMOUSMESSENGER","not much is left");
                        read = fis.read(buf);
                    }
                    if (read == -1) {
//                        Log.d("ANONYMOUSMESSENGER","nothing is left");
                        break;
                    }
//                    Log.d("ANONYMOUSMESSENGER","read: "+read);
                    out.write(cipher.doFinal(buf, 0, read));
                    out.flush();
                    done = done + read;
//                    Log.d("ANONYMOUSMESSENGER","done: "+done);
//                    Log.d("ANONYMOUSMESSENGER","left: "+(f.length()-done));
                    progressListener.onProgress(((int) (((double) done / (double) f.length()) * 100.0)));
                }
                progressListener.onProgress(100);
            }
            tmp.deleteOnExit();
            return tmp;
        }catch (Exception e){
            e.printStackTrace();
            progressListener.onProgress(100);
        }
        return null;
    }

    /**
     * This function takes an InputStream and encrypts it and stores it in a new file
     * */
    public static String saveFile(InputStream in, DxApplication app, String filename, int length) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        byte[] sha1b = app.getSha256();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        String eFilename = Hex.toStringCondensed(encrypt(sha1b,filename.getBytes()));
        FileOutputStream fos = app.openFileOutput(eFilename,Context.MODE_PRIVATE);

        try{
            int done = 0;
            while(true){
                Runtime.getRuntime().gc();
                byte[] iv = Utils.getSecretBytes(IV_LENGTH);
//                Log.d("ANONYMOUSMESSENGER","iv:" + Arrays.toString(iv));
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha1b, "AES"), new GCMParameterSpec(128, iv));
                byte[] buf = new byte[1024*1024*5];
                int read;
//                Log.d("ANONYMOUSMESSENGER","done: "+done);
//                Log.d("ANONYMOUSMESSENGER","length: "+length);
//                Log.d("ANONYMOUSMESSENGER","avail: "+in.available());
                if(length-done >= buf.length){
//                    Log.d("ANONYMOUSMESSENGER","a lot is left");
                    if(in.available()>=buf.length){
//                        Log.d("ANONYMOUSMESSENGER","its more than enough");
                        read = in.read(buf,0,buf.length);
                    }else{
//                        Log.d("ANONYMOUSMESSENGER","its not enough");
                        continue;
                    }
                }else{
//                    Log.d("ANONYMOUSMESSENGER","not much is left");
                    read = in.read(buf);
                }
                if(read==-1){
//                    Log.d("ANONYMOUSMESSENGER","nothing is left");
                    break;
                }
                fos.write(iv);
                byte[] enc = cipher.doFinal(buf,0,read);

                fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(enc.length).array());
                fos.write(enc);
                fos.flush();
                done += read;
//                Log.d("ANONYMOUSMESSENGER","encrypted chunk size: "+enc.length);
//                Log.d("ANONYMOUSMESSENGER","done: "+done);
//                Log.d("ANONYMOUSMESSENGER","------- STARTING ALL OVER AGAIN ------");
            }

//            Log.d("ANONYMOUSMESSENGER",eFilename);
            fos.close();
            //return its "path"
            return eFilename;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
    * This function returns an InputStream from a uri belonging to a file in shared storage
     */
    public static InputStream getInputStreamFromUri(Uri uri, Context context) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(uri);
    }


    /*
     * Utility functions
     * */

    public static String getFileName(Uri uri, Context context) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(Math.max(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME), 0));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if(result == null){
                return null;
            }
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static String getFileSize(Uri uri, Context context) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            String[] projection = {
                    MediaStore.Files.FileColumns.SIZE,
            };
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(Math.max(cursor.getColumnIndex(OpenableColumns.SIZE), 0));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut;
            if (result != null) {
                cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    public static long getFileSize(String path, Context context){
        File f = new File(context.getFilesDir(),path);
        if(!f.exists()){
            return 0;
        }
        return f.length();
    }

    public static long getAudioFileLengthInSeconds(String path, Context context){
        return getFileSize(path,context)/16000;
    }

    public static void cleanDir(File dir) {
//        long bytesDeleted = 0;
        File[] files = dir.listFiles();
        for (File file : Objects.requireNonNull(files)) {
//            bytesDeleted += file.length();
            file.delete();
//            if (bytesDeleted >= bytes) {
//                break;
//            }
        }
    }

}
