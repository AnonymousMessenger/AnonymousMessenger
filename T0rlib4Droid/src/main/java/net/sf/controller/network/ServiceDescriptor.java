package net.sf.controller.network;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServiceDescriptor extends TorServerSocket {
    private static final String PROXY_LOCALHOST = "127.0.0.1";
    private final int localPort;

    public ServiceDescriptor(String serviceName, int localPort, int servicePort) throws IOException {
        super(serviceName, servicePort);
        this.localPort = localPort;
        this.serverSocket.bind(new InetSocketAddress(PROXY_LOCALHOST, localPort));
    }

    public int getLocalPort() {
        return localPort;
    }
}
