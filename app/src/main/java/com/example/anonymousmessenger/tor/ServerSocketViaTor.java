package com.example.anonymousmessenger.tor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.anonymousmessenger.DxAccount;
import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.R;
import com.example.anonymousmessenger.messages.MessageSender;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.controller.network.TorServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
        private static final DateFormat df = new SimpleDateFormat("K:mm a, z");
        private String Date;
        private DxApplication app;

        private Server(ServerSocket socket, DxApplication app) {
            this.socket = socket;
            this.app = app;
        }

        @Override
        public void run() {
            app.setServerReady(true);
            try {
                while (true) {
                    try{
                        Socket sock = socket.accept();
                        this.Date = df.format(Calendar.getInstance().getTime());
                        Log.d("Accepted Client",(count++) + " at Address - " + sock.getRemoteSocketAddress()
                                        + " on port " + sock.getLocalPort() + " at time " + this.Date);
                        BufferedReader in =
                                new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        String message = "";
                        int charsRead = 0;
                        char[] buffer = new char[2048];

                        while ((charsRead = in.read(buffer)) != -1) {
                            message += new String(buffer).substring(0, charsRead);
                        }
                        Log.d("Accepted Client",(count++) + " at Address - " + sock.getRemoteSocketAddress()
                                        + " on port " + sock.getLocalPort() + " at time " + this.Date);
                        Log.e("MESSAGE GETTER", "from tor");
                        app.sendNotification("New Message!","you have a new secret message");
                        MessageSender.messageReceiver(message,app);
                            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
                            oos.writeBytes("received");
                            oos.flush();

                        app.sendNotification("New Message!","you have a new secret message");
                        sock.close();
                    }catch (IOException e){
                        Log.d("Server error",e.getMessage());
                    }
                }
            } catch (Exception  e) {
                e.printStackTrace();
                app.setServerReady(false);
            }
        }
    }
}
