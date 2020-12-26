package com.dx.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.call.CallController;
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.controller.network.TorServerSocket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSocketViaTor {
    private static final int hiddenservicedirport = 5780;
    //todo make this random to have more instances running
    private static final int localport = 5780;
    private final Context ctx;
    AndroidTorRelay node;
    Thread serverThread;
    Thread jobsThread;
    private TorServerSocket torServerSocket;

    public ServerSocketViaTor(Context ctx) {
        this.ctx = ctx;
    }

    public AndroidTorRelay getAndroidTorRelay(){
        return node;
    }

//    public void setAndroidTorRelay(AndroidTorRelay atr) {this.node = atr;}

//    public void setServerThread(Thread thread) {this.serverThread = thread;}

//    public Thread getServerThread() {return serverThread;}

    public void init(DxApplication app) throws IOException {
        if (ctx == null) {
            return;
        }

        if(node!=null){
            if(serverThread!=null){
                try{
                    serverThread.interrupt();
                    serverThread = null;
                }catch (Exception ignored){}
            }
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
            node = new AndroidTorRelay(ctx, fileLocation);
        }catch (Exception e){
            e.printStackTrace();
            init(app);
            return;
        }

        this.torServerSocket = node.createHiddenService(localport, hiddenservicedirport);
        app.setHostname(torServerSocket.getHostname());
        Entity myEntity = new Entity(app);
        app.setEntity(myEntity);

        ServerSocket ssocks = torServerSocket.getServerSocket();
        Server server = new Server(ssocks, app);

        this.serverThread = new Thread(server);
        serverThread.start();

        this.jobsThread = new Thread(()->{
            recurseJobs(app);
        });
        jobsThread.start();

//        serverLatch.await();
    }

    public void recurseJobs(DxApplication app){
        //send queued messages and update online statuses and delete due messages
        if(TorClientSocks4.test(app)){
            new Thread(app::queueAllUnsentMessages).start();
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
            if(serverThread!=null){
                try{
                    serverThread.interrupt();
                    serverThread = null;
                }catch (Exception ignored){}
            }
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
        private final ServerSocket socket;
//        private static final DateFormat df = new SimpleDateFormat("K:mm a, z", Locale.ENGLISH);
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
//                    if(sockets.get() >= ALLOWED_CONCURRENT_CONNECTIONS){
//                        Log.e("TOO MANY SOCKETS","open sockets: "+sockets);
//                        Thread.sleep(200);
////                        socket.close();
//                        continue;
//                    }
                    Socket sock = socket.accept();
                    if(sockets.get() >= ALLOWED_CONCURRENT_CONNECTIONS){
                        Log.e("TOO MANY SOCKETS","open sockets: "+sockets);
//                        Thread.sleep(200);
                        sock.close();
                        continue;
                    }
                    sockets.getAndIncrement();
                    Log.d("SERVER CONNECTION", "RECEIVING SOMETHING");
                    try{
                        new Thread(()->{
                            try{
                                DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
                                DataInputStream in=new DataInputStream(sock.getInputStream());
                                String msg = in.readUTF();
                                System.out.println(msg);
                                if(msg.contains("hello-")){
                                    if(msg.replace("hello-","").length() > 54 && msg.replace("hello-","").endsWith(".onion") && DbHelper.contactExists(msg.replace("hello-",""),app)){
                                        app.addToOnlineList(msg.replace("hello-",""));
                                        app.queueUnsentMessages(msg.replace("hello-",""));
                                    }
                                    sock.close();
                                    sockets.getAndDecrement();
                                    return;
                                }else if(msg.equals("call")){
                                    try {
                                        CallController.callReceiveHandler(sock,app);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return;
                                }else if(msg.equals("media")){
                                    try {
                                        outputStream.writeUTF("ok");
                                        outputStream.flush();
                                        msg = in.readUTF();
                                        if(msg.length() < 54 || !msg.trim().endsWith(".onion")){
                                            //no bueno
                                            outputStream.writeUTF("nuf");
                                            outputStream.flush();
                                            sock.close();
                                            sockets.getAndDecrement();
                                            return;
                                        }
//                                        String address= msg.trim();
                                        if(!DbHelper.contactExists(msg.trim(),app)){
                                            //no bueno
                                            outputStream.writeUTF("nuf");
                                            outputStream.flush();
                                            sock.close();
                                            sockets.getAndDecrement();
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
                                            outputStream.writeUTF("nuf");
                                            outputStream.flush();
                                            sock.close();
                                            sockets.getAndDecrement();
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
                                        Log.d("FILE RECEIVER", "TOTAL BYTES READ : "+total_read);
                                        Log.d("FILE RECEIVER", "FILE SIZE : "+fileSize);
                                        MessageSender.mediaMessageReceiver(cache.toByteArray(),recMsg,app);
                                    } catch (Exception e) {
                                        Log.e("RECEIVING MEDIA MESSAGE","ERROR BELOW");
                                        e.printStackTrace();
                                    }
                                    return;
                                }

                                //todo fix this vulnerability which allows attacker to send long utf to dos maybe
                                final String rec = msg;
                                new Thread(()-> MessageSender.messageReceiver(rec,app)).start();
                                outputStream.writeUTF("ack3");
                                outputStream.flush();
                                outputStream.close();
                                sockets.getAndDecrement();
                            }catch (Exception e){
                                sockets.getAndDecrement();
                                e.printStackTrace();
                            }
                        }).start();

                    }catch (Exception e){
                        sockets.getAndDecrement();
                        Log.e("SERVER ERROR", "ERROR");
                        e.printStackTrace();
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
                Log.e("SERVER STOPPED","RESTARTING SERVER");
                app.setServerReady(false);

//                run();
                app.restartTor();
            }
        }
    }
}
