package net.sf.msopentech.thali.java.toronionproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class ProxyServer implements Runnable {
    private static final int READ_TIMEOUT_MILLISECONDS = 60000;
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;


    protected int serverPortVal;
    protected int BackLog;
    protected String host;

    protected ServerSocket serverSocketVal = null;
    protected volatile boolean hasStopped = false;
    protected Thread movingThread = null;

    public ProxyServer(int Localport, int Backlog, String host) {
        this.serverPortVal = Localport;
        this.BackLog = BackLog;
        this.host = host;
    }

    public void run() {
        synchronized (this) {
            this.movingThread = Thread.currentThread();
        }
        opnSvrSocket();
        while (!hasStopped()) {
            Socket clntSocket = null;
            try {
                System.out.println("wait for connections");
                clntSocket = this.serverSocketVal.accept();
                System.out.println("Client Accepted" + clntSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                if (hasStopped()) {
                    System.out.println("Server has Stopped...Please check");
                    return;
                }
                throw new RuntimeException(
                        "Client cannot be connected - Error", e);
            }
            new Thread(
                    new WrkrRunnable(
                            clntSocket, "This is a multithreaded Server")
            ).start();
        }
        System.out.println("Server has Stopped...Please check");
    }

    private synchronized boolean hasStopped() {
        return this.hasStopped;
    }

    public synchronized void stop() {
        this.hasStopped = true;
        try {
            this.serverSocketVal.close();
        } catch (IOException e) {
            throw new RuntimeException("Server can not be closed - Please check error", e);
        }
    }

    private void opnSvrSocket() {
        try {
            this.serverSocketVal = new ServerSocket();
          //  this.serverSocketVal.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
            SocketAddress socksAddress = new InetSocketAddress(host, serverPortVal);
            this.serverSocketVal.bind(socksAddress, CONNECT_TIMEOUT_MILLISECONDS);
        } catch (IOException e) {
            throw new RuntimeException("Not able to open the port " + serverPortVal + " ", e);
        }
    }

}
