package com.dx.anonymousmessenger.tor;

import static com.dx.anonymousmessenger.file.FileHelper.IV_LENGTH;

import android.content.Context;
import android.content.Intent;
import android.os.StatFs;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.call.CallController;
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageEncryptor;
import com.dx.anonymousmessenger.messages.MessageReceiver;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.util.Hex;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.controller.network.ServiceDescriptor;
import net.sf.msopentech.thali.java.toronionproxy.FileUtilities;
import net.sf.msopentech.thali.java.toronionproxy.OnionProxyContext;
import net.sf.msopentech.thali.java.toronionproxy.OnionProxyManager;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ServerSocketViaTor {
    private static final int TOTAL_SEC_PER_STARTUP = 300;//these are secs not millis
    private static final int hiddenservicedirport = 5780;
    private int localport = 5780;
    private OnionProxyManager node;
    private Thread jobsThread;
    private ServiceDescriptor torServerSocket;
    private Thread serverThread;

    public ServerSocketViaTor() {
    }

    public OnionProxyManager getOnionProxyManager(){
        return node;
    }

    public synchronized void init(DxApplication app) {

        if(node!=null){
            tryKill();
            node = null;
        }
        /*

        meek_lite 0.0.2.0:3 97700DFE9F483596DDA6264C4D7DF7641E1E39CE url=https://meek.azureedge.net/ front=ajax.aspnetcdn.com

        obfs4 2.202.156.114:41902 9BA4CF70177E315D0F1CBF2DC8DED4FF761A5AB6 cert=8ru1uyWl5C1w9r/+BQ1TArNVzAEahiNTZUIUdNIcPxg3lrgl+y7NnoiH5Bt+j7aivw2uAQ iat-mode=0

        obfs4 185.185.251.73:443 EEE6E19E6C5E572D7830A9CCB5A9588623A37D63 cert=TIKryCwDjEiNnuq7pFymYqq8V1iACyBTMq8kPjV6WP1YxDq7nXvkms8x1sXMDp7U58ZTUw iat-mode=0

        obfs4 185.82.202.15:443 07E3239A6C8C589318FF2E8DFD3D1CEE496B1C13 cert=yT8pfL2Nr8m3Z7NFWq2cENOS4ZHZsZBugepxUbemM6+2su2+4QSjB6AR+cblRIGGjWORVw iat-mode=0

        */

        try {
            try{
                app.deleteAnyOldFiles();
            }catch (Exception ignored){}

            Entity myEntity = new Entity(app);
            app.setEntity(myEntity);

            node = new OnionProxyManager(new OnionProxyContext(app.getApplicationContext(), "torfiles"));

            while(torServerSocket==null){
                try{
                    //generate tor address/keys before tor start for the first time to make it possible to use the app offline and add other users too
                    this.torServerSocket = new ServiceDescriptor(localport, hiddenservicedirport);
                }catch (IOException ignored){
                    try {
                        if(this.torServerSocket!=null && this.torServerSocket.getServerSocket()!=null && this.torServerSocket.getServerSocket().isBound()){
                            this.torServerSocket.getServerSocket().close();
                        }
                    } catch (Exception ignored2) {}
                    this.torServerSocket=null;
                    localport++;
                }
            }

            if (!node.startWithoutRepeat(hiddenservicedirport, localport, TOTAL_SEC_PER_STARTUP, DbHelper.getBridgeList(app), app.isBridgesEnabled(), app.isEnableSocks5Proxy(), app.getSocks5AddressAndPort(), app.getSocks5Username(), app.getSocks5Password(), app.getExcludeText(), app.isExcludeUnknown(), app.isStrictExclude())) {
                Log.d("ANONYMOUSMESSENGER","Could not Start Tor.");
                tryKill();
                throw new IOException("Could not Start Tor.");
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                    Log.d("ANONYMOUSMESSENGER","shutdown hook");
                    app.resetTorStartTime();
                    tryKill();
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.d("ANONYMOUSMESSENGER","exception with tor start");
            tryKill();
            // tell user about the error
            Intent gcm_rec = new Intent("tor_status");
            gcm_rec.putExtra("tor_status","tor_error");
            LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
//            app.restartTor();
//            init(app);
            return;
        }

        try{
            if(node == null){
                Log.d("ANONYMOUSMESSENGER","error after tor start");
                tryKill();
                // tell user about the error
                Intent gcm_rec = new Intent("tor_status");
                gcm_rec.putExtra("tor_status","tor_error");
                LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
                return;
            }
            app.setHostname(new String(FileUtilities.read(node.getOnionProxyContext().getHostNameFile()), StandardCharsets.UTF_8).trim());
        }catch (Exception e){
            e.printStackTrace();
            Log.d("ANONYMOUSMESSENGER","cannot start a hiddenservice");
            tryKill();
            // tell user about the error
            Intent gcm_rec = new Intent("tor_status");
            gcm_rec.putExtra("tor_status","tor_error");
            LocalBroadcastManager.getInstance(app).sendBroadcast(gcm_rec);
            return;
        }

        ServerSocket ssocks = torServerSocket.getServerSocket();
        Server server = new Server(ssocks, app);

        this.jobsThread = new Thread(()->{
            FileHelper.cleanDir(Objects.requireNonNull(app.getExternalCacheDir()));
            FileHelper.cleanDir(Objects.requireNonNull(app.getCacheDir()));
            recurseJobs(app);
        });
        jobsThread.start();

        this.serverThread = new Thread(server);
        serverThread.start();
    }

    public void recurseJobs(DxApplication app){
        //send queued messages and update online statuses and delete due messages
        if(TorClient.test(app)){
            DbHelper.reduceLog(app);
            app.queueAllUnsentMessages();
        }
        try{
            Thread.sleep(10*60*1000);
        }catch (Exception ignored) {
            return;
        }
        recurseJobs(app);
    }

    public void tryKill(){
        if(jobsThread!=null){
            try{
                jobsThread.interrupt();
                jobsThread = null;
            }catch (Exception ignored){}
        }
        if(node!=null){
            try {
                Log.d("ANONYMOUSMESSENGER","starting node stop");
                node.stop();
                Log.d("ANONYMOUSMESSENGER","done with node stop");
            }catch (Exception e){
                e.printStackTrace();
                Log.d("ANONYMOUSMESSENGER","exception with node stop");
            }
            node = null;
        }
        if(torServerSocket!=null){
            try {
                torServerSocket.getServerSocket().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            torServerSocket = null;
        }
        if(serverThread != null){
            try {
                serverThread.interrupt();
            }catch (Exception e){
                e.printStackTrace();
            }
            serverThread = null;
        }
    }

    private static class Server implements Runnable {
        private static final long MAX_BUFFER_LENGTH = 7*1024*1024;
        private final ServerSocket socket;
        private final DxApplication app;
        private final static int ALLOWED_CONCURRENT_CONNECTIONS = 50;

        private Server(ServerSocket socket, DxApplication app) {
            this.socket = socket;
            this.app = app;
        }

        @Override
        public void run() {
            app.setServerReady(true);
            try {
                AtomicInteger sockets = new AtomicInteger();
                while (true) {
                    try{
                        if(sockets.get() >= ALLOWED_CONCURRENT_CONNECTIONS){
                            Log.e("TOO MANY SOCKETS","open sockets: "+sockets);
                            //noinspection BusyWait
                            Thread.sleep(200);
                            continue;
                        }
                        Socket sock = socket.accept();
                        if(sockets.get() >= ALLOWED_CONCURRENT_CONNECTIONS){
                            sock.close();
                            Log.e("TOO MANY SOCKETS","open sockets: "+sockets);
                            continue;
                        }
                        if(!sock.getInetAddress().toString().contains("127.0.0.1")){
                            sock.close();
                            continue;
                        }
                        sockets.getAndIncrement();
                        Log.d("SERVER CONNECTION", "RECEIVING SOMETHING");

                        new Thread(()->{
                            try{
                                DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
                                DataInputStream in=new DataInputStream(sock.getInputStream());

                                String msg = in.readUTF();

                                if(msg.startsWith("hello-")){
                                    handleHello(msg,sock,sockets);
                                    return;
                                }else if(msg.equals("call")){
                                    try {
                                        if(!app.isAcceptingCallsAllowed()){
                                            //no bueno
                                            refuseSocket(outputStream,sock,sockets);
                                            DbHelper.saveLog("REFUSED CALL BECAUSE RECEIVING CALLS IS DISABLED",new Date().getTime(),"NOTICE",app);
                                            return;
                                        }
                                        CallController.callReceiveHandler(sock,app);
                                    } catch (Exception e) {
                                        refuseSocket(outputStream,sock,sockets);
                                        DbHelper.saveLog("REFUSED CALL BECAUSE OF ERROR",new Date().getTime(),"NOTICE",app);
                                        e.printStackTrace();
                                    }
                                    return;
                                }else if(msg.equals("media")){
                                    handleMedia(outputStream,in,sock,sockets,false);
                                    return;
                                }else if(msg.equals("file")){
                                    handleFile(outputStream,in,sock,sockets);
                                    return;
                                }
                                else if(msg.equals("profile_image")){
                                    handleMedia(outputStream,in,sock,sockets,true);
                                    return;
                                }

                                new Thread(()-> MessageReceiver.messageReceiver(msg,app)).start();
                                outputStream.writeUTF("ack3");
                                outputStream.flush();
                                outputStream.close();
                                sockets.getAndDecrement();
                                DbHelper.saveLog("RECEIVED MESSAGE",new Date().getTime(),"NOTICE",app);
                            }catch (Exception e){
                                sockets.getAndDecrement();
                                if(!sock.isClosed()){
                                    try {
                                        sock.close();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }
                                }
                                e.printStackTrace();
                                DbHelper.saveLog("ERROR WHILE RECEIVING MESSAGE: "+ Arrays.toString(e.getStackTrace()),new Date().getTime(),"NOTICE",app);
                            }
                        }).start();
                    }catch (SocketException se){
                        DbHelper.saveLog("LOCAL SERVER CRASHED DUE TO LOCAL SOCKET CLOSED ",new Date().getTime(),"SEVERE",app);
                        break;
                    }catch (Exception e){
                        sockets.getAndDecrement();
                        Log.e("SERVER ERROR", "ERROR");
                        e.printStackTrace();
                        DbHelper.saveLog("LOCAL SERVER ERROR "+e.getMessage(),new Date().getTime(),"SEVERE",app);
                        break;
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
                Log.e("SERVER STOPPED","LOCAL SERVER NOT READY");
                app.setServerReady(false);
                DbHelper.saveLog("LOCAL SERVER STOPPED "+e.getMessage(),new Date().getTime(),"SEVERE",app);
                //app.restartTor();
            }
        }

        private void refuseSocket(DataOutputStream outputStream, Socket sock, AtomicInteger sockets) throws IOException {
            outputStream.writeUTF("nuf");
            outputStream.flush();
            sock.close();
            sockets.getAndDecrement();
        }

        private void handleHello(String msg, Socket sock, AtomicInteger sockets) throws IOException {
            sock.close();
            sockets.getAndDecrement();
            if(Utils.isValidAddress(msg.replace("hello-","")) && DbHelper.contactExists(msg.replace("hello-",""),app)){
                DbHelper.saveLog("PING FROM: "+msg.replace("hello-",""),new Date().getTime(),"NOTICE",app);
                app.addToOnlineList(msg.replace("hello-",""));
                app.queueUnsentMessages(msg.replace("hello-",""));
            }
            else{
                DbHelper.saveLog("PING FROM UNKNOWN CONTACT: "+msg.replace("hello-",""),new Date().getTime(),"SEVERE",app);
            }
        }

        private void handleFile(DataOutputStream outputStream, DataInputStream in, Socket sock, AtomicInteger sockets){
            String address = null;
            String eFilename = "";
            try {
                if(!app.isReceivingFilesAllowed()){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED FILE BECAUSE RECEIVING FILES IS DISABLED",new Date().getTime(),"NOTICE",app);
                    return;
                }
                outputStream.writeUTF("ok");
                outputStream.flush();
                String msg = in.readUTF();
                if(!Utils.isValidAddress(msg)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED FILE FROM: "+msg,new Date().getTime(),"NOTICE",app);
                    return;
                }
                address= msg.trim();
                if(!DbHelper.contactExists(address,app)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED FILE FROM: "+address,new Date().getTime(),"NOTICE",app);
                    return;
                }
                outputStream.writeUTF("ok");
                outputStream.flush();
                byte[] buffer = new byte[8];
                in.read(buffer,0,buffer.length);
                long length = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong();
                //maximum file size 3GB or set by user or storage limit
                //3L*1024*1024*1024 old value
                if(
                        length
                                >
                                Math.min(
                                        new StatFs(app.getFilesDir().getAbsolutePath()).getAvailableBytes(),
                                        app.getFileSizeLimit())
                ){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED LARGE FILE FROM: "+address+" SIZE: "+length,new Date().getTime(),"NOTICE",app);
                    return;
                }
                outputStream.writeUTF("ok");
                outputStream.flush();
                msg = in.readUTF();

                QuotedUserMessage qum = QuotedUserMessage.fromEncryptedJson(msg,app);

                if(qum==null){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED FILE WITH BAD MESSAGE FROM: "+address+" SIZE: "+length,new Date().getTime(),"SEVERE",app);
                    return;
                }

                byte[] sha1b = app.getSha256();
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                eFilename = Hex.toStringCondensed(FileHelper.encrypt(sha1b,qum.getFilename().getBytes()));
                FileOutputStream fos = app.openFileOutput(eFilename,Context.MODE_PRIVATE);

                //need to set the new path
                qum.setPath(eFilename);

                outputStream.writeUTF("ok");
                outputStream.flush();
                // start receiving chunks

                int done = 0;
                while(true){
                    buffer = new byte[4];
                    in.read(buffer,0,buffer.length);
                    int chunkLength = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

//                    Log.d("ANONYMOUSMESSENGER","CHUNK LENGTH:::: "+chunkLength);
                    if(chunkLength==0){
                        break;
                    }
                    if(chunkLength>MAX_BUFFER_LENGTH || chunkLength<0){
                        //no bueno
                        refuseSocket(outputStream,sock,sockets);
                        FileHelper.deleteFile(eFilename,app);
                        DbHelper.saveLog("OVERSIZED FILE CHUNK SIZE FROM: "+address+" CHUNK SIZE: "+chunkLength+" MAX: "+MAX_BUFFER_LENGTH,new Date().getTime(),"NOTICE",app);
                        return;
                    }
                    buffer = new byte[chunkLength];
                    int read = 0;

                    if(done>(length+(length*0.20))){
                        //no bueno
                        refuseSocket(outputStream,sock,sockets);
                        FileHelper.deleteFile(eFilename,app);
                        DbHelper.saveLog("OVERSIZED FILE FROM: "+address+" SIZE: "+length+" DONE: "+done,new Date().getTime(),"NOTICE",app);
                        return;
                    }

                    int chunkDone = 0;
                    while (chunkDone<chunkLength){
                        read = in.read(buffer,chunkDone,chunkLength-chunkDone);
                        chunkDone+=read;
                    }

                    if(read==-1 || read==0){
                        break;
                    }

                    buffer = MessageEncryptor.decrypt(buffer,app.getEntity().getStore(),new SignalProtocolAddress(address,1));

                    byte[] iv = Utils.getSecretBytes(IV_LENGTH);

                    fos.write(iv);

                    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha1b, "AES"), new GCMParameterSpec(128, iv));
                    byte[] enc = cipher.doFinal(buffer);
                    fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(enc.length).array());
                    fos.write(enc);
                    fos.flush();
                    done += chunkDone;
                }

                if(done<length){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    FileHelper.deleteFile(eFilename,app);
                    DbHelper.saveLog("UNDERSIZED FILE FROM: "+address+" SIZE: "+length+" DONE: "+done,new Date().getTime(),"NOTICE",app);
                    return;
                }

                try{
                    outputStream.writeByte(1);
                }catch (Exception ignored){}
                try{
                    sock.close();
                }catch (Exception ignored){}
                sockets.getAndDecrement();

                DbHelper.saveMessage(qum,app,qum.getAddress(),true);
                DbHelper.setContactNickname(qum.getSender(), qum.getAddress(), app);
                DbHelper.setContactUnread(qum.getAddress(),app);

                app.sendNotification(app.getString(R.string.new_message),app.getString(R.string.you_have_message));
                Intent gcm_rec = new Intent("your_action");
                gcm_rec.putExtra("address",qum.getAddress().substring(0,10));
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                DbHelper.saveLog("RECEIVED FILE FROM: "+address+" SIZE: "+length,new Date().getTime(),"NOTICE",app);
            } catch (Exception e) {
                Log.e("RECEIVING FILE","ERROR BELOW");
                e.printStackTrace();
                FileHelper.deleteFile(eFilename,app);
                DbHelper.saveLog("ERROR WHILE RECEIVING FILE FROM: "+address+" "+ e.getMessage(),new Date().getTime(),"NOTICE",app);
            }
        }

        private void handleMedia(DataOutputStream outputStream, DataInputStream in, Socket sock, AtomicInteger sockets, boolean isProfileImage){
            String address = "";
            try {
                outputStream.writeUTF("ok");
                outputStream.flush();
                String msg = in.readUTF();
                if(!Utils.isValidAddress(msg)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED MEDIA FROM: "+msg+" REASON: BAD ADDRESS",new Date().getTime(),"SEVERE",app);
                    return;
                }
                address = msg.trim();
                if(!DbHelper.contactExists(msg.trim(),app)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED MEDIA FROM: "+address+" REASON: UNKNOWN CONTACT",new Date().getTime(),"SEVERE",app);
                    return;
                }
                outputStream.writeUTF("ok");
                outputStream.flush();
                msg = in.readUTF();
                String recMsg = msg;
                outputStream.writeUTF("ok");
                outputStream.flush();
                int fileSize = in.readInt();
                //maximum profile image size 2MB
                //maximum file size 30MB
                if(isProfileImage){
                    if(fileSize>(2*1024*1024)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED OVERSIZED PROFILE IMAGE FROM: "+address+" SIZE: "+fileSize+" MAX: "+(30*1024*1024),new Date().getTime(),"NOTICE",app);
                    return;
                    }
                }
                if(fileSize>(30*1024*1024)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED OVERSIZED MEDIA FROM: "+address+" SIZE: "+fileSize+" MAX: "+(30*1024*1024),new Date().getTime(),"NOTICE",app);
                    return;
                }
                outputStream.writeUTF("ok");
                outputStream.flush();
                byte[] buffer;
                ByteArrayOutputStream cache = new ByteArrayOutputStream();
                int total_read = 0;
                int read;
                while(total_read < fileSize){
                    if(in.available() < 1024){
                        if(in.available() == 0){
                            continue;
                        }
                        buffer = new byte[in.available()];
                    }else{
                        buffer = new byte[1024];
                    }
                    read = in.read(buffer,0,buffer.length);
                    total_read += read;
                    cache.write(buffer,0,buffer.length);
                }
                try{
                    outputStream.writeByte(1);
                }catch (Exception ignored){}
                try{
                    sock.close();
                }catch (Exception ignored){}
                sockets.getAndDecrement();
                if(isProfileImage){
                    MessageReceiver.mediaMessageReceiver(cache.toByteArray(),recMsg,app,true);
                    DbHelper.saveLog("RECEIVED PROFILE IMAGE FROM "+address+" SIZE "+fileSize,new Date().getTime(),"NOTICE",app);
                }else{
                    MessageReceiver.mediaMessageReceiver(cache.toByteArray(),recMsg,app,false);
                    DbHelper.saveLog("RECEIVED MEDIA FROM "+address+" SIZE "+fileSize,new Date().getTime(),"NOTICE",app);
                }

            } catch (Exception e) {
                try{
                    sock.close();
                }catch (Exception ignored){}
                sockets.getAndDecrement();
                Log.e("RECEIVING MEDIA MESSAGE","ERROR BELOW");
                e.printStackTrace();
                DbHelper.saveLog("ERROR WHILE RECEIVING MEDIA FROM: "+address+" ERROR: "+ Arrays.toString(e.getStackTrace()),new Date().getTime(),"NOTICE",app);
            }
        }
    }
}
