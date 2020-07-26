package net.sf.controller.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPForwardServer {
    protected int SOURCE_PORT;
    protected String DESTINATION_HOST;
    protected int DESTINATION_PORT;

    public TCPForwardServer(int SOURCE_PORT, String DESTINATION_HOST, int DESTINATION_PORT) throws IOException {
        this.SOURCE_PORT = SOURCE_PORT;
        this.DESTINATION_HOST = DESTINATION_HOST;
        this.DESTINATION_PORT = DESTINATION_PORT;

        ServerSocket serverSocket = new ServerSocket(DESTINATION_PORT);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("client Accepted with "+clientSocket.getRemoteSocketAddress());
            ClientThread clientThread =
                    new ClientThread(clientSocket, DESTINATION_HOST, SOURCE_PORT);
            clientThread.start();
        }
    }
}


class ClientThread extends Thread {
    private Socket mClientSocket;
    private Socket mServerSocket;
    protected String DESTINATION_HOST;
    protected int DESTINATION_PORT;
    private boolean mForwardingActive = false;

    public ClientThread(Socket aClientSocket, String DESTINATION_HOST, int DESTINATION_PORT) {
        mClientSocket = aClientSocket;
        this.DESTINATION_HOST = DESTINATION_HOST;
        this.DESTINATION_PORT = DESTINATION_PORT;
    }

    public void run() {
        InputStream clientIn;
        OutputStream clientOut;
        InputStream serverIn;
        OutputStream serverOut;
        try {
            // Connect to the destination server
            mServerSocket = new Socket(
                    DESTINATION_HOST,
                    DESTINATION_PORT);

            // Turn on keep-alive for both the sockets
            mServerSocket.setKeepAlive(true);
            mClientSocket.setKeepAlive(true);

            // Obtain client & server input & output streams
            clientIn = mClientSocket.getInputStream();
            clientOut = mClientSocket.getOutputStream();
            serverIn = mServerSocket.getInputStream();
            serverOut = mServerSocket.getOutputStream();
        } catch (IOException ioe) {
            System.err.println("Can not connect to " +
                    DESTINATION_HOST + ":" +
                    DESTINATION_PORT);
            connectionBroken();
            return;
        }

        // Start forwarding data between server and client
        mForwardingActive = true;
        ForwardThread clientForward =
                new ForwardThread(this, clientIn, serverOut);
        clientForward.start();
        ForwardThread serverForward =
                new ForwardThread(this, serverIn, clientOut);
        serverForward.start();

        System.out.println("TCP Forwarding " +
                mClientSocket.getInetAddress().getHostAddress() +
                ":" + mClientSocket.getPort() + " <--> " +
                mServerSocket.getInetAddress().getHostAddress() +
                ":" + mServerSocket.getPort() + " started.");
    }


    public synchronized void connectionBroken() {
        try {
            mServerSocket.close();
        } catch (Exception e) {
        }
        try {
            mClientSocket.close();
        } catch (Exception e) {
        }

        if (mForwardingActive) {
            System.out.println("TCP Forwarding " +
                    mClientSocket.getInetAddress().getHostAddress()
                    + ":" + mClientSocket.getPort() + " <--> " +
                    mServerSocket.getInetAddress().getHostAddress()
                    + ":" + mServerSocket.getPort() + " stopped.");
            mForwardingActive = false;
        }
    }
}

class ForwardThread extends Thread {
    private static final int BUFFER_SIZE = 8192;

    InputStream mInputStream;
    OutputStream mOutputStream;
    ClientThread mParent;

    public ForwardThread(ClientThread aParent, InputStream
            aInputStream, OutputStream aOutputStream) {
        mParent = aParent;
        mInputStream = aInputStream;
        mOutputStream = aOutputStream;
    }


    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (true) {
                int bytesRead = mInputStream.read(buffer);
                if (bytesRead == -1)
                    break; // End of stream is reached --> exit
                mOutputStream.write(buffer, 0, bytesRead);
                mOutputStream.flush();
            }
        } catch (IOException e) {
            // Read/write failed --> connection is broken
        }

        // Notify parent thread that the connection is broken
        mParent.connectionBroken();
    }
}
