package com.dx.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.dx.anonymousmessenger.DxAccount;
import com.dx.anonymousmessenger.DxApplication;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class ServerSocketViaTor {
    private static final Logger LOG = LoggerFactory.getLogger(ServerSocketViaTor.class);
    private static final int hiddenservicedirport = 5780;
    private static final int localport = 5780;
    private static CountDownLatch serverLatch = new CountDownLatch(2);
    private Context ctx;
    AndroidTorRelay node;

    public ServerSocketViaTor(Context ctx) {
        this.ctx = ctx;
    }

    public AndroidTorRelay getAndroidTorRelay(){
        return node;
    }

    public void init(DxApplication app) throws IOException, InterruptedException {
        if (ctx == null) {
            return;
        }

        if(node!=null){
            boolean exit = false;
            while (!exit){
                try {
                    node.shutDown();
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    node.initTor();
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
        TorServerSocket torServerSocket = node.createHiddenService(localport, hiddenservicedirport);
        app.setHostname(torServerSocket.getHostname());
        DxAccount account;
        if(app.getAccount()==null){
            account = new DxAccount();
        }else{
            account = app.getAccount();
        }
        account.setAddress(torServerSocket.getHostname());
        account.setPort(node.getSocksPort());
        app.setAccount(account);
        ServerSocket ssocks = torServerSocket.getServerSocket();
        Server server = new Server(ssocks,app);

        new Thread(server).start();

        serverLatch.await();
    }

    private static class Server implements Runnable {
        private final ServerSocket socket;
        private static final DateFormat df = new SimpleDateFormat("K:mm a, z", Locale.ENGLISH);
        private String Date;
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
                    try{
//                        this.Date = df.format(Calendar.getInstance().getTime());
//                        Log.d("Accepted Client"," at Address - " + sock.getRemoteSocketAddress()
//                                        + " on port " + sock.getLocalPort() + " at time " + this.Date);
                        new Thread(()->{
                            try{
                                DataOutputStream outputStream = new DataOutputStream(sock.getOutputStream());
                                DataInputStream in=new DataInputStream(sock.getInputStream());
                                String msg = in.readUTF();
                                while(!msg.equals("nuf"))
                                {
                                    final String rec = msg;
                                    new Thread(()->{MessageSender.messageReceiver(rec,app);}).start();
                                    outputStream.writeUTF("ack3");
                                    outputStream.flush();
                                    msg = in.readUTF();
                                }
                                outputStream.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }).start();
                        app.sendNotification("New Message!","you have a new secret message");
                    }catch (Exception e){
                        Log.d("Server error", Objects.requireNonNull(e.getMessage()));
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
                app.setServerReady(false);
            }
        }
    }
}
