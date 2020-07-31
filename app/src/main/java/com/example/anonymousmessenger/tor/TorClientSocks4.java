package com.example.anonymousmessenger.tor;

import android.content.Context;
import android.util.Log;

import com.example.anonymousmessenger.DxApplication;
import com.example.anonymousmessenger.messages.MessageSender;
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
import java.util.concurrent.atomic.AtomicReference;

public class TorClientSocks4 {
    public TorClientSocks4() {
    }

    public boolean Init(String OnionAddress, DxApplication app, String msg) {
        Socket socket;
            try {
                socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",
                        app.getRport());

                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream in =new DataInputStream(socket.getInputStream());
                String msg2 = "";
                boolean result = false;

                while(!msg.equals("nuf"))
                {
                    outputStream.writeUTF(msg);
                    outputStream.flush();
                    msg2 = in.readUTF();
                    if(msg2.contains("ack3")){
                        outputStream.writeUTF("nuf");
                        outputStream.flush();
                        msg = "nuf";
                        result = true;
                    }
                }
                outputStream.close();
                socket.close();
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        return false;
    }
}
