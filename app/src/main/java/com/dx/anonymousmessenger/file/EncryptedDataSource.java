//package com.dx.anonymousmessenger.file;
//
//
//import android.net.Uri;
//
//import androidx.annotation.NonNull;
//
//import com.dx.anonymousmessenger.DxApplication;
//import com.google.android.exoplayer2.upstream.DataSource;
//import com.google.android.exoplayer2.upstream.DataSpec;
//import com.google.android.exoplayer2.upstream.TransferListener;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//
//import javax.crypto.Cipher;
//import javax.crypto.CipherInputStream;
//import javax.crypto.spec.GCMParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//
//public class EncryptedDataSource implements DataSource {
//    private static final int IV_LENGTH = 12;
//    private CipherInputStream inputStream = null;
//    private Uri uri;
//    private byte[] key;
//    private DxApplication app;
//
//    public EncryptedDataSource(byte[] key, DxApplication app){
//        this.app = app;
//        this.key = key;
//    }
//
//    @Override
//    public void addTransferListener(@NonNull TransferListener transferListener) {}
//
//    @Override
//    public long open(DataSpec dataSpec){
//        uri = dataSpec.uri;
//        try{
//            if(uri.getPath()==null){
//                return 0;
//            }
//            File file = new File(app.getFilesDir(),uri.getPath());
//            FileInputStream encryptedStream = new FileInputStream(file);
//            byte[] iv = new byte[IV_LENGTH];
//            encryptedStream.read(iv,0,IV_LENGTH);
//            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
//            inputStream = new CipherInputStream(encryptedStream, cipher);
//        }catch(Exception e) {
//            e.printStackTrace();
//        }
//        return dataSpec.length;
//    }
//
//    @Override
//    public int read(@NonNull byte[] buffer, int offset, int readLength) throws IOException {
//        if(readLength == 0){
//            return 0;
//        }else{
//            return inputStream.read(buffer, offset, readLength);
//        }
//    }
//
//    @Override
//    public Uri getUri(){
//        return uri;
//    }
//    @Override
//    public void close() throws IOException {
//        inputStream.close();
//    }
//}

