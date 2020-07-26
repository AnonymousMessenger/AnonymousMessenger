package com.example.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.msopentech.thali.java.toronionproxy.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TorClientSocks4 {
    private Context ctx = null;

    public TorClientSocks4() {
    }

    public TorClientSocks4(Context ctx) {

        this.ctx = ctx;
    }

    public String Init() throws IOException, InterruptedException {
        if(ctx==null){
            Log.e("TorTest", "Couldn't start Tor!");
            return "";
        }
        String fileLocation = "torfiles";
        // Start the Tor Onion Proxy
        AndroidTorRelay node = new AndroidTorRelay(ctx,fileLocation);
        int hiddenServicePort = 443;
        int localPort = node.getSocksPort();
        String OnionAdress = "a.fasicurrency.com";
        String localhost="127.0.0.1";

        Socket clientSocket = Utilities.socks4aSocketConnection(OnionAdress, hiddenServicePort, "127.0.0.1", localPort);

        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();

        out.writeObject("i am workingg");
        out.flush();

        BufferedReader in =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String message = "";
        int charsRead = 0;
        char[] buffer = new char[2048];

        while ((charsRead = in.read(buffer)) != -1) {
            message += new String(buffer).substring(0, charsRead);
        }
        return message;
    }
}
