
package net.sf.msopentech.thali.java.toronionproxy;

import net.sf.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import net.sf.runjva.sourceforge.jsocks.protocol.SocksSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class Utilities {
    private static final int READ_TIMEOUT_MILLISECONDS = 30000;
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;
    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    private Utilities() {
    }

    public static Socket socks4aSocketConnection(String networkHost, int networkPort, String socksHost, int socksPort)
            throws IOException {
        //check if tor is in good shape or maybe wait for it
        //DNS LEAK IF YOU USE THIS //hopefully fixed like this
        SocketAddress proxyAddress = InetSocketAddress.createUnresolved(socksHost,socksPort);
        SocketAddress onion = InetSocketAddress.createUnresolved(networkHost,networkPort);
        java.net.Proxy proxy = new java.net.Proxy(Proxy.Type.SOCKS,proxyAddress);
        Socket socket = new Socket(proxy);
        socket.connect(onion);
        return socket;
    }


    public static Socket socks5rawSocketConnection(String networkHost, int networkPort, String socksHost, int socksPort)
            throws IOException {

        int bytesRead;
        boolean end = false;
        String messageString = "";
        final byte[] messageByte = new byte[1000];

        Socket socket = new Socket();
        socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
        SocketAddress socksAddress = new InetSocketAddress(socksHost, socksPort);
        socket.connect(socksAddress, CONNECT_TIMEOUT_MILLISECONDS);

        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.write((byte) 0x05);
        outputStream.write((byte) 0x01);
        outputStream.write((byte) 0x00);
        outputStream.write((byte) 0x01);
        outputStream.write(networkHost.getBytes());
        outputStream.writeShort((short) networkPort);

        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        messageByte[0] = inputStream.readByte();
        messageByte[1] = inputStream.readByte();
        if (messageByte[0] != (byte) 0x05 || messageByte[1] != (byte) 0x00) {
            socket.close();
            throw new IOException("SOCKS4a connect failed, got " + messageByte[0] + " - " + messageByte[1] +
                    ", but expected 0x00 - 0x5a");
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(messageByte, 0, 2);

        int bytesToRead = byteBuffer.getShort();
        LOG.info("About to read " + bytesToRead + " octets");

        while (!end) {
            bytesRead = inputStream.read(messageByte);
            messageString += new String(messageByte, 0, bytesRead);
            if (messageString.length() == bytesToRead) {
                end = true;
            }
        }

        return socket;

    }

    public static Socket Socks5connection(Socks5Proxy proxy, String onionUrl, int HiddenServicePort) throws IOException {
        Socket ssock = new SocksSocket(proxy, onionUrl, HiddenServicePort);
        ssock.setTcpNoDelay(true);
        return ssock;
    }

    public static Socket Socks5connection(String networkHost, int networkPort, String socksHost, int socksPort) throws IOException {
        return Socks5connection(new Socks5Proxy(socksHost,socksPort),networkHost, networkPort);
    }

}
