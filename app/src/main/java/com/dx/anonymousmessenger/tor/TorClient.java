package com.dx.anonymousmessenger.tor;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageEncryptor;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.runjva.sourceforge.jsocks.protocol.Socks5Message;

import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TorClient {
    private static final int READ_TIMEOUT_MILLISECONDS = 30000;
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;

    public TorClient() {
    }

    /**
     * for getting a ready socket to call a peer
     * @param onionAddress address to get a socket connected to
     * @param app an instance of DxApplication
     * @param type type of call socket (constant in call service)
     * @return returns a socket ready to send voice data to
     */
    public static Socket getCallSocket(String onionAddress, DxApplication app, String type){
        Socket socket = null;
        try {
            if(!app.getEntity().getStore().containsSession(new SignalProtocolAddress(onionAddress,1))){
                return null;
            }
            socket = socks5SocketConnection(onionAddress, 5780, "127.0.0.1", app.getTorSocket().getOnionProxyManager().getIPv4LocalHostSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;

            outputStream.writeUTF("call");
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                outputStream.writeUTF(app.getHostname());
                byte[] msg = MessageEncryptor.encrypt(app.getHostname().getBytes(StandardCharsets.UTF_8),app.getEntity().getStore(),new SignalProtocolAddress(onionAddress,1));
                outputStream.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(msg.length).array());
                outputStream.write(msg);
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

    public static boolean sendMedia(String onion, DxApplication app, String msg, byte[] media, boolean isProfileImage){
        Socket socket = null;
        try {
            socket = socks5SocketConnection(onion, 5780, "127.0.0.1", app.getTorSocket().getOnionProxyManager().getIPv4LocalHostSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            String msg2;

            if(isProfileImage){
                outputStream.writeUTF("profile_image");
            }else{
                outputStream.writeUTF("media");
            }
            outputStream.flush();
            outputStream.writeUTF(app.getHostname());
            outputStream.flush();
            outputStream.writeUTF(msg);
            outputStream.flush();
            outputStream.writeInt(media.length);
            outputStream.flush();

            msg2 = in.readUTF();
            if(!msg2.contains("ok")){
                socket.close();
                return false;
            }
            msg2 = in.readUTF();
            if(!msg2.contains("ok")){
                socket.close();
                return false;
            }
            msg2 = in.readUTF();
            if(!msg2.contains("ok")){
                socket.close();
                return false;
            }
            msg2 = in.readUTF();
            if(!msg2.contains("ok")){
                socket.close();
                return false;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(media);
            byte[] buffer;
            while(true){
                Log.d("ANONYMOUSMESSENGER", "sending part");
                if(bais.available()<1024){
                    buffer = new byte[bais.available()];
                }else{
                    buffer = new byte[1024];
                }
                int read = bais.read(buffer,0,buffer.length);
                if(read<=0){
                    break;
                }
                outputStream.write(buffer,0,buffer.length);
            }
            outputStream.flush();
            int resp = in.readByte();
            if(resp!=1){
                //return false
                try{
                    outputStream.close();
                    socket.close();
                }catch (Exception ignored){}
                return false;
            }
            try{
                outputStream.close();
                socket.close();
            }catch (Exception ignored){}
            return true;
        } catch (Exception e) {
            if(socket!=null){
                try {
                    socket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendFile(String onion, DxApplication app, String msg, FileInputStream fis, long length){
        Socket socket;
        try {
            //todo: put in thread and keep reference to do cancel by the user from the notification
            socket = socks5SocketConnection(onion, 5780, "127.0.0.1", app.getTorSocket().getOnionProxyManager().getIPv4LocalHostSocksPort());

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
                            buf = MessageEncryptor.encrypt(buf,app.getEntity().getStore(),new SignalProtocolAddress(onion,1));

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
//                        out.flush();
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

    public static boolean sendMessage(String onionAddress, DxApplication app, String msg) {
        Socket socket;
        try {
            socket = socks5SocketConnection(onionAddress, 5780, "127.0.0.1", app.getTorSocket().getOnionProxyManager().getIPv4LocalHostSocksPort());

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
        Socket socket = null;
        try {
            socket = socks5SocketConnection(app.getTestAddress(), 80, "127.0.0.1", app.getTorSocket().getOnionProxyManager().getIPv4LocalHostSocksPort());
            boolean b = socket.isConnected() && !socket.isClosed();
            socket.close();
            return b;
        }catch (Exception e){
            try{
                if (socket != null) {
                    socket.close();
                }
            }catch (Exception ignored){}
            return false;
        }
    }

    public static boolean testAddress(DxApplication app, String address){
        Socket socket = null;
        try {
            socket = socks5SocketConnection(address, 5780, "127.0.0.1", app.getTorSocket().getOnionProxyManager().getIPv4LocalHostSocksPort());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("hello-"+app.getHostname());
            outputStream.flush();
            try{
                outputStream.close();
                socket.close();
            }catch (Exception ignored){}
            return true;
        }catch (EOFException e){
            try{
                if (socket != null) {
                    socket.close();
                }
            }catch (Exception ignored){}
            return true;
        }catch (Exception e){
            try{
                if (socket != null) {
                    socket.close();
                }
            }catch (Exception ignored){}
            return false;
        }
    }

    public static Socket socks5SocketConnection(String networkHost, int networkPort, String socksHost, int socksPort)
            throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
        SocketAddress socksAddress = new InetSocketAddress(socksHost, socksPort);
        socket.connect(socksAddress, CONNECT_TIMEOUT_MILLISECONDS);

        ///////////////////////////////////////////////////////////////
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.write((byte) 0x05);
        outputStream.write((byte) 0x01);
        outputStream.write((byte) 0x00);
        outputStream.flush();

        byte[] response = new byte[2];
        inputStream.readFully(response);
        // check if server responded with correct version and no-authentication method
        if (response[0] != (byte) 0x05 || response[1] != (byte) 0x00) {
            throw new IOException("SOCKS5 connect failed, got " + response[0] + " - " + response[1] +
                    ", but expected 0x05 - 0x00");
        }

        Socks5Message socks5Message = new Socks5Message(1,networkHost,networkPort);
        socks5Message.write(outputStream);
        outputStream.flush();
        ///////////////////////////////////////////////////////////////

        byte[] header = new byte[4];
        inputStream.readFully(header, 0, 4);

        if (header[1] != (byte) 0x00) {
            Log.d("ANONYMOUSMESSENGER","ERROR REQUEST NOT OK: "+header[1]);
            throw new IOException("SOCKS5 connect failed");
        }

        if (header[3] == (byte) 0x01) {
            // tor will respond with a 0.0.0.0 IP address
            byte[] addr = new byte[4];
            inputStream.readFully(addr, 0, 4);
            header = new byte[2];
            inputStream.readFully(header, 0, 2);
            return socket;
        }else if(header[3] == (byte) 0x03){
            Log.d("ANONYMOUSMESSENGER","GOT ADDRESS BACK");
            int len = header[1];
            byte[] host = new byte[len];
            inputStream.readFully(host, 0, len);
            header = new byte[2];
            inputStream.readFully(header, 0, 2);
            return  socket;
        }

        throw new IOException("SOCKS5 connect failed");
    }
}
