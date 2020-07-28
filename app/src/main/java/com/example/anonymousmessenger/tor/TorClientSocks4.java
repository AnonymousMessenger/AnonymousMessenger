package com.example.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.messages.UserMessage;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.msopentech.thali.java.toronionproxy.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TorClientSocks4 {
    private Context ctx = null;

    public TorClientSocks4(Context ctx) {
        this.ctx = ctx;
    }

    public String Init(String OnionAddress, DxApplication app, byte[] msg) throws IOException,
            InterruptedException {

        Socket clientSocket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",
                app.getRport());

        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();

        out.writeObject(msg);
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
