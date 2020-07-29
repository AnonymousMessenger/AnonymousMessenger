package com.example.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.messages.UserMessage;

import net.sf.controller.network.AndroidTorRelay;
import net.sf.msopentech.thali.java.toronionproxy.Utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TorClientSocks4 {
    private Context ctx;

    public TorClientSocks4(Context ctx) {
        this.ctx = ctx;
    }

    public boolean Init(String OnionAddress, DxApplication app, String msg) throws IOException,
            InterruptedException {
        Socket socket;
        try{
            socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",
                app.getRport());
        }catch (Exception e){e.printStackTrace();Thread.sleep(200);return Init(OnionAddress,app,msg);}
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeBytes(msg);
        outputStream.flush();
        outputStream.close();
        return true;
    }
}
