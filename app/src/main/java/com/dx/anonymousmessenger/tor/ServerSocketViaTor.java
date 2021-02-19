package com.dx.anonymousmessenger.tor;

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
import com.dx.anonymousmessenger.messages.MessageEncrypter;
import com.dx.anonymousmessenger.messages.MessageReceiver;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.util.Hex;
import com.dx.anonymousmessenger.util.Utils;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.controller.network.TorServerSocket;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.dx.anonymousmessenger.file.FileHelper.IV_LENGTH;

public class ServerSocketViaTor {
    private static final int hiddenservicedirport = 5780;
    private int localport = 5780;
    AndroidTorRelay node;
    Thread jobsThread;
    private TorServerSocket torServerSocket;

    public ServerSocketViaTor() {
    }

    public AndroidTorRelay getAndroidTorRelay(){
        return node;
    }

    public void init(DxApplication app) throws IOException {

        if(node!=null){
            if(torServerSocket!=null){
                torServerSocket.getServerSocket().close();
                torServerSocket = null;
            }
            try {
                node.shutDown();
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }
            node = null;
        }

        String fileLocation = "torfiles";
        try {
            node = new AndroidTorRelay(app.getApplicationContext(), fileLocation);
        }catch (Exception e){
            e.printStackTrace();
            try {
                if(node!=null){
                    node.shutDown();
                }
                Thread.sleep(3000);
            } catch (Exception ignored) {}
            app.restartTor();
            return;
        }

        while(torServerSocket==null){
            try{
                this.torServerSocket = node.createHiddenService(localport, hiddenservicedirport);
            }catch (IOException ignored){
                localport++;
            }
        }

        app.setHostname(torServerSocket.getHostname());
        Entity myEntity = new Entity(app);
        app.setEntity(myEntity);

        ServerSocket ssocks = torServerSocket.getServerSocket();
        Server server = new Server(ssocks, app);

        this.jobsThread = new Thread(()->{
            FileHelper.cleanDir(Objects.requireNonNull(app.getExternalCacheDir()));
            FileHelper.cleanDir(Objects.requireNonNull(app.getCacheDir()));
            recurseJobs(app);
        });
        jobsThread.start();

        server.run();
    }

    public void recurseJobs(DxApplication app){
        //send queued messages and update online statuses and delete due messages
        if(TorClientSocks4.test(app)){
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
        if(node!=null){
            boolean exit = false;
            if(jobsThread!=null){
                try{
                    jobsThread.interrupt();
                    jobsThread = null;
                }catch (Exception ignored){}
            }
            if(torServerSocket!=null){
                try {
                    torServerSocket.getServerSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                torServerSocket = null;
            }
            while (!exit){
                try {
                    node.shutDown();
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    node = null;
                    exit = true;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
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

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            app.setServerReady(true);
            try {
                AtomicInteger sockets = new AtomicInteger();
                while (true) {
                    try{
                        if(sockets.get() >= ALLOWED_CONCURRENT_CONNECTIONS){
                            Log.e("TOO MANY SOCKETS","open sockets: "+sockets);
                            Thread.sleep(200);
                            continue;
                        }
                        Socket sock = socket.accept();
                        if(sockets.get() >= ALLOWED_CONCURRENT_CONNECTIONS){
                            sock.close();
                            Log.e("TOO MANY SOCKETS","open sockets: "+sockets);
                            continue;
                        }
                        sockets.getAndIncrement();
                        Log.d("SERVER CONNECTION", "RECEIVING SOMETHING");

                        new Thread(()->{
                            try{
                                DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
                                DataInputStream in=new DataInputStream(sock.getInputStream());

                                String msg = in.readUTF();

                                if(msg.contains("hello-")){
                                    handleHello(msg,sock,sockets);
                                    return;
                                }else if(msg.equals("call")){
                                    try {
                                        CallController.callReceiveHandler(sock,app);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return;
                                }else if(msg.equals("media")){
                                    handleMedia(outputStream,in,sock,sockets);
                                    return;
                                }else if(msg.equals("file")){
                                    handleFile(outputStream,in,sock,sockets);
                                    return;
                                }

                                //todo fix this vulnerability which allows attacker to send long utf to dos maybe
                                final String rec = msg;
                                new Thread(()-> MessageReceiver.messageReceiver(rec,app)).start();
                                outputStream.writeUTF("ack3");
                                outputStream.flush();
                                outputStream.close();
                                sockets.getAndDecrement();
                                DbHelper.saveLog("RECEIVED MESSAGE",new Date().getTime(),"NOTICE",app);
                            }catch (Exception e){
                                sockets.getAndDecrement();
                                e.printStackTrace();
                                DbHelper.saveLog("ERROR WHILE RECEIVING MESSAGE: "+ Arrays.toString(e.getStackTrace()),new Date().getTime(),"NOTICE",app);
                            }
                        }).start();
                    }catch (Exception e){
                        sockets.getAndDecrement();
                        Log.e("SERVER ERROR", "ERROR");
                        e.printStackTrace();
                        DbHelper.saveLog("LOCAL SERVER ERROR "+e.getMessage(),new Date().getTime(),"SEVERE",app);
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
                Log.e("SERVER STOPPED","RESTARTING SERVER");
                app.setServerReady(false);
                DbHelper.saveLog("LOCAL SERVER STOPPED "+e.getMessage(),new Date().getTime(),"SEVERE",app);
                //app.restartTor();
            }
        }

        private boolean isValidAddress(String address){
            return address.length() >= 54 && address.trim().endsWith(".onion");
        }

        private void refuseSocket(DataOutputStream outputStream, Socket sock, AtomicInteger sockets) throws IOException {
            outputStream.writeUTF("nuf");
            outputStream.flush();
            sock.close();
            sockets.getAndDecrement();
        }

        private void handleHello(String msg, Socket sock, AtomicInteger sockets) throws IOException {
            if(isValidAddress(msg.replace("hello-","")) && DbHelper.contactExists(msg.replace("hello-",""),app)){
                app.addToOnlineList(msg.replace("hello-",""));
                app.queueUnsentMessages(msg.replace("hello-",""));
            }
            sock.close();
            sockets.getAndDecrement();
            DbHelper.saveLog("PING FROM: "+msg.replace("hello-",""),new Date().getTime(),"NOTICE",app);
        }

        private void handleFile(DataOutputStream outputStream, DataInputStream in, Socket sock, AtomicInteger sockets){
            String address = null;
            try {
                outputStream.writeUTF("ok");
                outputStream.flush();
                String msg = in.readUTF();
                if(!isValidAddress(msg)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED FILE FROM: "+address,new Date().getTime(),"NOTICE",app);
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
                //maximum file size 3GB or storage limit
                if(
                        length
                                >
                                Math.min(
                                        new StatFs(app.getFilesDir().getAbsolutePath()).getAvailableBytes(),
                                        3L*1024*1024*1024)
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
                    DbHelper.saveLog("REFUSED FILE WITH BAD MESSAGE FROM: "+address+" SIZE: "+length,new Date().getTime(),"NOTICE",app);
                    return;
                }

                byte[] sha1b = app.getSha256();
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                String eFilename = Hex.toStringCondensed(FileHelper.encrypt(sha1b,qum.getFilename().getBytes()));
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

                    System.out.println("CHUNK LENGTH:::: "+chunkLength);
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

                    buffer = MessageEncrypter.decrypt(buffer,app.getEntity().getStore(),new SignalProtocolAddress(address,1));

                    byte[] iv = Utils.getSecretBytes(IV_LENGTH);

                    fos.write(iv);

                    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha1b, "AES"), new GCMParameterSpec(128, iv));
                    byte[] enc = cipher.doFinal(buffer);
                    fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(enc.length).array());
                    fos.write(enc);
                    fos.flush();
                    done += chunkDone;
                }
                in.close();
                fos.close();
                sockets.getAndDecrement();

                DbHelper.saveMessage(qum,app,qum.getAddress(),true);
                DbHelper.setContactNickname(qum.getSender(), qum.getAddress(), app);
                DbHelper.setContactUnread(qum.getAddress(),app);

                app.sendNotification(app.getString(R.string.new_message),app.getString(R.string.you_have_message));
                Intent gcm_rec = new Intent("your_action");
                LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(gcm_rec);
                DbHelper.saveLog("RECEIVED FILE FROM: "+address+" SIZE: "+length,new Date().getTime(),"NOTICE",app);
            } catch (Exception e) {
                Log.e("RECEIVING FILE","ERROR BELOW");
                e.printStackTrace();
                DbHelper.saveLog("ERROR WHILE RECEIVING FILE FROM: "+address+" "+ e.getMessage(),new Date().getTime(),"NOTICE",app);
            }
        }

        private void handleMedia(DataOutputStream outputStream, DataInputStream in, Socket sock, AtomicInteger sockets){
            String address = "";
            try {
                outputStream.writeUTF("ok");
                outputStream.flush();
                String msg = in.readUTF();
                if(!isValidAddress(msg)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED MEDIA FROM: "+msg+" REASON: BAD ADDRESS",new Date().getTime(),"NOTICE",app);
                    return;
                }
                address = msg.trim();
                if(!DbHelper.contactExists(msg.trim(),app)){
                    //no bueno
                    refuseSocket(outputStream,sock,sockets);
                    DbHelper.saveLog("REFUSED MEDIA FROM: "+address+" REASON: UNKNOWN CONTACT",new Date().getTime(),"NOTICE",app);
                    return;
                }
                outputStream.writeUTF("ok");
                outputStream.flush();
                msg = in.readUTF();
                String recMsg = msg;
                outputStream.writeUTF("ok");
                outputStream.flush();
                int fileSize = in.readInt();
                //maximum file size 30MB
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
                in.close();
                sockets.getAndDecrement();
                MessageReceiver.mediaMessageReceiver(cache.toByteArray(),recMsg,app);
                DbHelper.saveLog("RECEIVED MEDIA FROM "+address+" SIZE "+fileSize,new Date().getTime(),"NOTICE",app);
            } catch (Exception e) {
                Log.e("RECEIVING MEDIA MESSAGE","ERROR BELOW");
                e.printStackTrace();
                DbHelper.saveLog("ERROR WHILE RECEIVING MEDIA FROM: "+address+" ERROR: "+ Arrays.toString(e.getStackTrace()),new Date().getTime(),"NOTICE",app);
            }
        }
    }
}
