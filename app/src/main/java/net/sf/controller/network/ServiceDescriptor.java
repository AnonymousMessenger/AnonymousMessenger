package net.sf.controller.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class ServiceDescriptor {
    private static final String PROXY_LOCALHOST = "127.0.0.1";
    private final int localPort;

    protected String hostname;
    protected final int servicePort;
    protected final ServerSocket serverSocket;

    public ServiceDescriptor(String serviceName, int localPort, int servicePort) throws IOException {
        this.hostname = serviceName;
        this.servicePort = servicePort;
        this.localPort = localPort;
        this.serverSocket = new ServerSocket();
        this.serverSocket.bind(new InetSocketAddress(PROXY_LOCALHOST, localPort));
    }

    public ServiceDescriptor(int localPort, int servicePort) throws IOException {
        this.servicePort = servicePort;
        this.localPort = localPort;
        this.serverSocket = new ServerSocket();
        this.serverSocket.bind(new InetSocketAddress(PROXY_LOCALHOST, localPort));
    }

    public void addHostname(String hostname){
        this.hostname = hostname;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getHostname() {
        return hostname;
    }

    public int getServicePort() {
        return servicePort;
    }

    public String getFullAddress() {
        return hostname + ":" + servicePort;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
