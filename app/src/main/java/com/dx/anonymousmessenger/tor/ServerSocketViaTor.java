package com.dx.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.call.CallController;
import com.dx.anonymousmessenger.crypto.Entity;
import com.dx.anonymousmessenger.messages.MessageSender;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.controller.network.TorServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class ServerSocketViaTor {
    private static final Logger LOG = LoggerFactory.getLogger(ServerSocketViaTor.class);
    private static final int hiddenservicedirport = 5780;
    private static final int localport = 5780;
    private static CountDownLatch serverLatch = new CountDownLatch(2);
    private Context ctx;
    AndroidTorRelay node;
    Thread serverThread;
    private TorServerSocket torServerSocket;
    private Server server;

    public ServerSocketViaTor(Context ctx) {
        this.ctx = ctx;
    }

    public AndroidTorRelay getAndroidTorRelay(){
        return node;
    }

    public void setAndroidTorRelay(AndroidTorRelay atr) {this.node = atr;}

    public void setServerThread(Thread thread) {this.serverThread = thread;}

    public Thread getServerThread() {return serverThread;}

    public void init(DxApplication app) throws IOException, InterruptedException {
        if (ctx == null) {
            return;
        }

        if(node!=null){
            boolean exit = false;
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

        String fileLocation = "torfiles";
        while (node==null){
            try {
                node = new AndroidTorRelay(ctx, fileLocation);
            }catch (Exception e){
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                }catch (InterruptedException ie){ie.printStackTrace();}
            }
        }

        this.torServerSocket = node.createHiddenService(localport, hiddenservicedirport);
        app.setHostname(torServerSocket.getHostname());
        Entity myEntity = new Entity(app);
        app.setEntity(myEntity);

        ServerSocket ssocks = torServerSocket.getServerSocket();
        this.server = new Server(ssocks,app);

        this.serverThread = new Thread(server);
        serverThread.start();

//        serverLatch.await();
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
        private DxApplication app;

        private Server(ServerSocket socket, DxApplication app) {
            this.socket = socket;
            this.app = app;
        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            app.setServerReady(true);
            try {
                while (true) {
                    Socket sock = socket.accept();
                    Log.e("SERVER CONNECTION", "SOMETHING IS HAPPENING");
                    try{
                        new Thread(()->{
                            try{
                                DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
                                DataInputStream in=new DataInputStream(sock.getInputStream());
                                String msg = in.readUTF();
                                if(msg.equals("call")){
                                    try {
                                        CallController.callReceiveHandler(sock,app);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return;
                                }
                                while(!msg.equals("nuf"))
                                {
                                    final String rec = msg;
                                    new Thread(()->{
                                        MessageSender.messageReceiver(rec,app);
                                        app.sendNotification("New Message!","you have a new secret message");
                                    }).start();
                                    outputStream.writeUTF("ack3");
                                    outputStream.flush();
                                    msg = in.readUTF();
                                }
                                outputStream.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }).start();

                    }catch (Exception e){
                        Log.e("SERVER ERRORRRRR", "EROROROROROROROR");
                        Log.e("Server error", Objects.requireNonNull(e.getMessage()));
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
                app.setServerReady(false);
            }
        }
    }
}
