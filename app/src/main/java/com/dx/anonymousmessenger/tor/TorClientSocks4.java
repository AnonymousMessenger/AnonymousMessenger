package com.dx.anonymousmessenger.tor;

import com.dx.anonymousmessenger.DxApplication;

import net.sf.msopentech.thali.java.toronionproxy.Utilities;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.Socket;

public class TorClientSocks4 {
    public TorClientSocks4() {
    }

    public Socket getCallSocket(String OnionAddress, DxApplication app, String type){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(OnionAddress, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;

            outputStream.writeUTF("call");
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                outputStream.writeUTF(app.getHostname());
                outputStream.flush();
                msg2 = in.readUTF();
                if(msg2.contains("ok")){
                    outputStream.writeUTF(type);
                    outputStream.flush();
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

    public boolean sendMedia(String onion, DxApplication app, String msg, byte[] media){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(onion, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());

            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream in =new DataInputStream(socket.getInputStream());
            String msg2;

            outputStream.writeUTF("media");
            outputStream.flush();
            msg2 = in.readUTF();
            if(msg2.contains("ok")){
                outputStream.writeUTF(app.getHostname());
                outputStream.flush();
                msg2 = in.readUTF();
                if(msg2.contains("ok")){
                    outputStream.writeUTF(msg);
                    outputStream.flush();
                    msg2 = in.readUTF();
                    if(msg2.contains("ok")){
                        outputStream.writeInt(media.length);
                        outputStream.flush();
                        msg2 = in.readUTF();
                        if(msg2.contains("ok")){
                            ByteArrayInputStream bais = new ByteArrayInputStream(media);
                            byte[] buffer;
                            while(bais.available()>0){
                                System.out.println("sending part");
                                if(bais.available()<1024){
                                    buffer = new byte[bais.available()];
                                }else{
                                    buffer = new byte[1024];
                                }
                                bais.read(buffer,0,buffer.length);
                                outputStream.write(buffer,0,buffer.length);
                            }
                            outputStream.flush();
                            outputStream.close();
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
            String msg2;
            boolean result = false;

//            while(!msg.equals("nuf"))
//            {
                outputStream.writeUTF(msg);
                outputStream.flush();
                msg2 = in.readUTF();
                if(msg2.contains("ack3")){
//                    outputStream.writeUTF("nuf");
//                    outputStream.flush();
//                    msg = "nuf";
                    result = true;
                }
//            }
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
            boolean b = socket.isConnected() && !socket.isClosed();
            socket.close();
            return b;
        }catch (Exception e){
            return false;
        }
    }

    public static boolean testAddress(DxApplication app, String address){
        Socket socket;
        try {
            socket = Utilities.socks4aSocketConnection(address, 5780, "127.0.0.1",app.getAndroidTorRelay().getSocksPort());
//            boolean b = socket.isConnected() && !socket.isClosed();
//            socket.close();
//            return b;
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
//            DataInputStream in =new DataInputStream(socket.getInputStream());
            outputStream.writeUTF("hello-"+app.getHostname());
            outputStream.flush();
//            String s = in.readUTF();
//            outputStream.close();
//            socket.close();
            return true;
        }catch (EOFException e){
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
