package com.example.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.example.anonymousmessenger.DxAccount;
import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.messages.MessageSender;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.controller.network.TorServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
        String fileLocation = "torfiles";
        node = new AndroidTorRelay(ctx, fileLocation);
        TorServerSocket torServerSocket = node.createHiddenService(localport, hiddenservicedirport);
        app.setHostname(torServerSocket.getHostname());
        app.setLport(torServerSocket.getServicePort());
        app.setRport(node.getSocksPort());
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
        private int count = 0;
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
                AtomicReference<BufferedReader> in = new AtomicReference<>();
                while (true) {
                    Socket sock = socket.accept();
                    try{
                        this.Date = df.format(Calendar.getInstance().getTime());
                        Log.e("Accepted Client",(count++) + " at Address - " + sock.getRemoteSocketAddress()
                                        + " on port " + sock.getLocalPort() + " at time " + this.Date);
                        new Thread(()->{
                            try{
                                in.set(new BufferedReader(new InputStreamReader(sock.getInputStream())));
                                String line = in.get().readLine();
                                String msg = "";
                                while( line != null )
                                {
                                    msg = msg.concat(line);
                                    System.out.println( line );
                                    line = in.get().readLine();
                                }
                                Log.e("RECEIVE STARTED",msg+":::::::::::::::::::::::");
                                MessageSender.messageReceiver(msg,app);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }).start();
                        app.sendNotification("New Message!","you have a new secret message");
//                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(sock.getOutputStream())), true);
//                        out.println("received");
//                        out.flush();
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
