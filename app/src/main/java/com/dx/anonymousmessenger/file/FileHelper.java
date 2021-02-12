package com.dx.anonymousmessenger.file;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RecoverySystem;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.crypto.CipherInputStream;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.content.Context.DOWNLOAD_SERVICE;

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

    public static CipherInputStream encryptToStream(byte[] key, byte[] iv, InputStream data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new CipherInputStream(data,cipher);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
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

    public static com.dx.anonymousmessenger.crypto.CipherInputStream decryptToStream(byte[] key, FileInputStream data) throws IOException, InvalidAlgorithmParameterException, java.security.InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher   cipher     = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[IV_LENGTH];
        data.read(iv,0,IV_LENGTH);

        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return new com.dx.anonymousmessenger.crypto.CipherInputStream(data,cipher);
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

    public static void saveToStorageWithProgress(String path, String filename, DxApplication app, RecoverySystem.ProgressListener progressListener){
        try{
            //decrypt this stuff
            MessageDigest crypt = MessageDigest.getInstance("SHA-256");
            crypt.reset();
            crypt.update(app.getAccount().getPassword());
            byte[] sha1b = crypt.digest();
            File f = new File(app.getFilesDir(),path);
            if(!f.exists()){
                return;
            }
            String suffix = "."+filename.split("\\.")[filename.split("\\.").length-1];
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix.replace(".",""));
            if(mime==null){
                mime = "*/*";
            }
            File file = File.createTempFile(filename.replace(suffix,""),suffix,app.getExternalCacheDir());
            file.createNewFile();
            FileUtilities.copyWithProgress(decryptToStream(sha1b,new FileInputStream(f)),new FileOutputStream(file),progressListener,f.length());
            DownloadManager downloadManager = (DownloadManager) app.getSystemService(DOWNLOAD_SERVICE);
            downloadManager.addCompletedDownload(file.getName(), file.getName(), true, mime,file.getAbsolutePath(),file.length(),true);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static File getTempFileWithProgress(String path, String filename, DxApplication app, RecoverySystem.ProgressListener progressListener){
        try{
            //decrypt this stuff
            MessageDigest crypt = MessageDigest.getInstance("SHA-256");
            crypt.reset();
            crypt.update(app.getAccount().getPassword());
            byte[] sha1b = crypt.digest();
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
            FileUtilities.copyWithProgress(decryptToStream(sha1b,new FileInputStream(f)),new FileOutputStream(tmp),progressListener,f.length());
            tmp.deleteOnExit();
            return tmp;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getUnencryptedFile(String path, DxApplication app){
        try{
            File f = new File(path);
            if(!f.exists()){
                System.out.println("file doesn't exist");
                return null;
            }
            return FileUtilities.read(f);
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

    public static String saveFile(InputStream data, DxApplication app, String filename) throws NoSuchAlgorithmException {
        MessageDigest crypt = MessageDigest.getInstance("SHA-256");
        crypt.reset();
        crypt.update(app.getAccount().getPassword());
        byte[] sha1b = crypt.digest();
        byte[] iv     = Utils.getSecretBytes(IV_LENGTH);
        CipherInputStream encrypted = encryptToStream(sha1b, iv, data);
        String eFilename = Hex.toStringCondensed(encrypt(sha1b,filename.getBytes()));
        try{
            FileOutputStream out = app.openFileOutput(eFilename,Context.MODE_PRIVATE);
            out.write(iv);
            FileUtilities.copy(encrypted,out);
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
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
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

    public static String getFilePath(Uri uri, Context context) {
//        return getRealPathFromURI_API19(context, uri);
//        context.getContentResolver().
        return getPath(context,uri);
    }

    public static InputStream getInputStreamFromUri(Uri uri, Context context) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(uri);
    }

    public static String getFileSize(Uri uri, Context context) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            String[] projection = {
                    MediaStore.Files.FileColumns.SIZE,
            };
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
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

    public static String getRealPathFromURI_API19(Context context, Uri uri){
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);
        String id;

        if(wholeID.contains(":")){
            // Split at colon, use second item in the array
            id = wholeID.split(":")[1];
        }else{
            id = wholeID.split("/")[wholeID.split("/").length-1];
        }

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Files.FileColumns._ID + "=?";

        Cursor cursor = context.getContentResolver().query(uri,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    public static void cleanDir(File dir) {
//        long bytesDeleted = 0;
        File[] files = dir.listFiles();
        for (File file : files) {
//            bytesDeleted += file.length();
            file.delete();
//            if (bytesDeleted >= bytes) {
//                break;
//            }
        }
    }

    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
//        Log.i("URI",uri+"");
        String result = uri+"";
        // DocumentProvider
        //  if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        if (isKitKat && (result.contains("media.documents"))) {
            String[] ary = result.split("/");
            int length = ary.length;
            String imgary = ary[length-1];
            final String[] dat = imgary.split("%3A");
            final String docId = dat[1];
            final String type = dat[0];
            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
            } else if ("audio".equals(type)) {
            }
            final String selection = "_id=?";
            final String[] selectionArgs = new String[] {
                    dat[1]
            };
            return getDataColumn(context, contentUri, selection, selectionArgs);
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                String data = cursor.getString(column_index);
                if (cursor != null){
                    cursor.close();
                }
                return data;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
