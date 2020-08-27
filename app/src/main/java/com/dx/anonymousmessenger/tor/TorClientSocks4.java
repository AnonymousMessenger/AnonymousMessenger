package com.dx.anonymousmessenger.tor;

import com.dx.anonymousmessenger.DxApplication;

import net.sf.msopentech.thali.java.toronionproxy.Utilities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.Socket;

public class TorClientSocks4 {
    public TorClientSocks4() {
    }

    public Socket getCallSocket(String OnionAddress, DxApplication app){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2 = "";

            outputStream.writeUTF("call");
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                outputStream.writeUTF(app.getHostname());
                outputStream.flush();
                if(msg2.contains("ok")){
                    return socket;
                }else{
                    return null;
                }
            }else{
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public boolean Init(String OnionAddress, DxApplication app, String msg) {
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());
//            socket = Utilities.Socks5connection(new Socks5Proxy("127.0.0.1",app.getRport()),OnionAddress,5780);
//            socket = Utilities.socks5rawSocketConnection(OnionAddress,5780,"127.0.0.1",app.getRport());

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
            return false;
        }
    }

    public boolean test(DxApplication app){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection("nraswjtnyrvywxk7.onion", 80, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());
            if(socket.isConnected()&&!socket.isClosed()){
                return true;
            }else{
                return false;
            }
//            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
//            DataInputStream in =new DataInputStream(socket.getInputStream());
//            outputStream.writeUTF("hello");
//            outputStream.flush();
//            Log.e("TEST CONN", String.valueOf(in.readUTF()));
//            outputStream.close();
//            socket.close();
//            return true;
        }catch (EOFException e){
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
