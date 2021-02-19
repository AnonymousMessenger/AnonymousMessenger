package com.dx.anonymousmessenger.tor;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageEncrypter;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.msopentech.thali.java.toronionproxy.Utilities;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TorClientSocks4 {
    public TorClientSocks4() {
    }

    /**
     * for getting a ready socket to call a peer
     * @param OnionAddress address to get a socket connected to
     * @param app an instance of DxApplication
     * @param type type of call socket (constant in call service)
     * @return returns a socket ready to send voice data to
     */
    public static Socket getCallSocket(String OnionAddress, DxApplication app, String type){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;

            outputStream.writeUTF("call");
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                outputStream.writeUTF(app.getHostname());
                outputStream.flush();
                msg2 = in.readUTF();
                if(msg2.contains("ok")){
                    outputStream.writeUTF(type);
                    outputStream.flush();
                    return socket;
                }else{
                    return null;
                }
            }else{
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean sendMedia(String onion, DxApplication app, String msg, byte[] media){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(onion, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;

            outputStream.writeUTF("media");
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                outputStream.writeUTF(app.getHostname());
                outputStream.flush();
                msg2 = in.readUTF();
                if(msg2.contains("ok")){
                    outputStream.writeUTF(msg);
                    outputStream.flush();
                    msg2 = in.readUTF();
                    if(msg2.contains("ok")){
                        outputStream.writeInt(media.length);
                        outputStream.flush();
                        msg2 = in.readUTF();
                        if(msg2.contains("ok")){
                            ByteArrayInputStream bais = new ByteArrayInputStream(media);
                            byte[] buffer;
                            while(bais.available()>0){
                                //System.out.println("sending part");
                                if(bais.available()<1024){
                                    buffer = new byte[bais.available()];
                                }else{
                                    buffer = new byte[1024];
                                }
                                bais.read(buffer,0,buffer.length);
                                outputStream.write(buffer,0,buffer.length);
                            }
                            outputStream.flush();
                            outputStream.close();
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendFile(String onion, DxApplication app, String msg, FileInputStream fis, long length){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(onion, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;

            out.writeUTF("file");
            out.flush();
            in.readUTF();
            out.writeUTF(app.getHostname());
            out.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                byte[] approxSize = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(length).array();
                out.write(approxSize);
                out.flush();
                msg2 = in.readUTF();
                if(msg2.contains("ok")){
                    out.writeUTF(msg);
                    out.flush();
                    msg2 = in.readUTF();
                    if(msg2.contains("ok")){
                        byte[] sha1b = app.getSha256();
                        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                        int done = 0;

                        app.sendNotificationWithProgress(app.getString(R.string.sending_file),app.getString(R.string.sending_part_one),done);

                        while(true){
                            byte[] iv = new byte[FileHelper.IV_LENGTH];
                            fis.read(iv,0,FileHelper.IV_LENGTH);
                            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha1b, "AES"), new GCMParameterSpec(128, iv));
                            byte[] chunkSize = new byte[4];
                            fis.read(chunkSize,0,chunkSize.length);
                            int casted = ByteBuffer.wrap(chunkSize).order(ByteOrder.LITTLE_ENDIAN).getInt();
                            if(casted==0){
                                break;
                            }
                            byte[] buf = new byte[casted];
                            int read;
                            if(length-done >= buf.length){
                                read = fis.read(buf,0,buf.length);
                            }else{
                                read = fis.read(buf);
                            }
                            if(read==-1){
                                break;
                            }

                            if(read==0){
                                out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array());
                                out.flush();
                                break;
                            }

                            buf = cipher.doFinal(buf,0,buf.length);
                            buf = MessageEncrypter.encrypt(buf,app.getEntity().getStore(),new SignalProtocolAddress(onion,1));

                            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(buf.length).array());
                            out.flush();
                            long time = new Date().getTime();
                            out.write(buf);
                            out.flush();
                            done=done+read;

                            app.sendNotificationWithProgress
                                    (app.getString(R.string.sending_file),
                                    Utils.humanReadableSpeed(buf.length,time),
                                    ((int) (((double) done/(double) length)*100.0)));
                        }
                        out.flush();
                        out.close();
                        fis.close();
                        app.clearNotification(2);
                        return true;
                    }
                }
            }
            out.close();
            fis.close();
            app.clearNotification(2);
            return false;
        } catch (Exception e) {
            app.clearNotification(2);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendMessage(String OnionAddress, DxApplication app, String msg) {
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;
            boolean result = false;

            outputStream.writeUTF(msg);
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ack3")){
                result = true;
            }
            outputStream.close();
            socket.close();
            return result;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean test(DxApplication app){
        Socket socket;
        try {
            //this is th ddg onion v2 site to check if we are online
            //todo put address in settings
            socket = Utilities.socks4aSocketConnection("3g2upl4pq6kufc4m.onion", 80, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());
            boolean b = socket.isConnected() && !socket.isClosed();
            socket.close();
            return b;
        }catch (Exception e){
            return false;
        }
    }

    public static boolean testAddress(DxApplication app, String address){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(address, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("hello-"+app.getHostname());
            outputStream.flush();
            return true;
        }catch (EOFException e){
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
