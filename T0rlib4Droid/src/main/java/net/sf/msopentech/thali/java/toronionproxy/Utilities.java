
package net.sf.msopentech.thali.java.toronionproxy;

import net.sf.runjva.sourceforge.jsocks.protocol.Socks5Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class Utilities {
    private static final int READ_TIMEOUT_MILLISECONDS = 30000;
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;

    private Utilities() {
    }

    public static Socket socks4aSocketConnection(String networkHost, int networkPort, String socksHost, int socksPort)
            throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
        SocketAddress socksAddress = new InetSocketAddress(socksHost, socksPort);
        socket.connect(socksAddress, CONNECT_TIMEOUT_MILLISECONDS);

        ///////////////////////////////////////////////////////////////
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.write((byte) 0x05);
        outputStream.write((byte) 0x01);
        outputStream.write((byte) 0x00);
        outputStream.flush();

        byte[] response = new byte[2];
        inputStream.readFully(response);
        // check if server responded with correct version and no-authentication method
        if (response[0] != (byte) 0x05 || response[1] != (byte) 0x00) {
            throw new IOException("SOCKS5 connect failed, got " + response[0] + " - " + response[1] +
                    ", but expected 0x05 - 0x00");
        }

        Socks5Message socks5Message = new Socks5Message(1,networkHost,networkPort);
        socks5Message.write(outputStream);
        outputStream.flush();
        ///////////////////////////////////////////////////////////////

        byte[] header = new byte[4];
        inputStream.readFully(header, 0, 4);

        if (header[1] != (byte) 0x00) {
            System.out.println("ERROR REQUEST NOT OK: "+header[1]);
            throw new IOException("SOCKS5 connect failed");
        }

        if (header[3] == (byte) 0x01) {
            System.out.println("GOT IP ADDRESS BACK");
            byte[] addr = new byte[4];
            inputStream.readFully(addr, 0, 4);
            header = new byte[2];
            inputStream.readFully(header, 0, 2);
            return socket;
        }else if(header[3] == (byte) 0x03){
            System.out.println("GOT ADDRESS BACK");
            int len = header[1];
            byte[] host = new byte[len];
            inputStream.readFully(host, 0, len);
            header = new byte[2];
            inputStream.readFully(header, 0, 2);
            return  socket;
        }

        throw new IOException("SOCKS5 connect failed");
    }

}
