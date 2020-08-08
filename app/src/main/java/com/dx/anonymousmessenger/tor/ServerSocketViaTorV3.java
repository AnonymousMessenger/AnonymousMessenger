//package com.example.anonymousmessenger.tor;
//
//import android.content.Context;
//import android.util.Log;
//
//import com.example.anonymousmessenger.DxApplication;
//import com.jrummyapps.android.shell.CommandResult;
//import com.jrummyapps.android.shell.Shell;
//
//import net.sf.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
//
//import org.torproject.android.binary.TorResourceInstaller;
//
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.Socket;
//import java.net.SocketAddress;
//import java.net.SocketException;
//
//public class ServerSocketViaTorV3 {
//    private static final int READ_TIMEOUT_MILLISECONDS = 30000;
//    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;
//    public void moin(Context ctx){
//    try {
//        TorResourceInstaller torResourceInstaller = new TorResourceInstaller(ctx , ctx.getFilesDir());
//
//        File fileTorBin = torResourceInstaller.installResources();
//        File fileTorRc = torResourceInstaller.getTorrcFile();
//
//        boolean success = fileTorBin != null && fileTorBin.canExecute();
//
//        String message = "Tor install success? " + success;
//        logNotice(message);
//
//        if (success) {
//            runTorShellCmd(fileTorBin, fileTorRc, ctx);
////            torResourceInstaller.updateTorConfigCustom()
//        }
//
//
//
//    } catch (Exception e) {
//        e.printStackTrace();
//        logNotice(e.getMessage());
//    }
//}
//
//    public Socket socks4aConnection(String networkHost,int networkPort,String socksHost,int socksPort) throws IOException {
////        String socksHost = "127.0.0.1";
////        int socksPort = 9050;
////        String networkHost = "";
////        int networkPort = 5780;
//        Socket socket = new Socket();
//        socket.setSoTimeout(READ_TIMEOUT_MILLISECONDS);
//        SocketAddress socksAddress = new InetSocketAddress(socksHost, socksPort);
//        socket.connect(socksAddress, CONNECT_TIMEOUT_MILLISECONDS);
//
//        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
//        outputStream.write((byte) 0x04);
//        outputStream.write((byte) 0x01);
//        outputStream.writeShort((short) networkPort);
//        outputStream.writeInt(0x01);
//        outputStream.write((byte) 0x00);
//        outputStream.write(networkHost.getBytes());
//        outputStream.write((byte) 0x00);
//
//        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
//        byte firstByte = inputStream.readByte();
//        byte secondByte = inputStream.readByte();
//        if (firstByte != (byte) 0x00 || secondByte != (byte) 0x5a) {
//            socket.close();
//            throw new IOException("SOCKS4a connect failed, got " + firstByte + " - " + secondByte +
//                    ", but expected 0x00 - 0x5a");
//        }
//        inputStream.readShort();
//        inputStream.readInt();
//        return socket;
//    }
//
//    private void logNotice(String notice) {
//        Log.d(notice, notice);
//    }
//
//    private void logNotice(String notice, Exception e) {
//        logNotice(notice);
//        Log.e("SampleTor", "error occurred", e);
//    }
//
//    private boolean runTorShellCmd(File fileTor, File fileTorrc, Context ctx) throws Exception {
//        File appCacheHome = ctx.getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA, DxApplication.MODE_PRIVATE);
//
//        if (!fileTorrc.exists()) {
//            logNotice("torrc not installed: " + fileTorrc.getCanonicalPath());
//            return false;
//        }
//
//        String torCmdString = fileTor.getCanonicalPath()
//                + " DataDirectory " + appCacheHome.getCanonicalPath()
//                + " --defaults-torrc " + fileTorrc;
//
//        int exitCode = -1;
//
//        try {
//            exitCode = exec(torCmdString + " --verify-config", true);
//        } catch (Exception e) {
//            logNotice("Tor configuration did not verify: " + e.getMessage(), e);
//            return false;
//        }
//
//        try {
//            exitCode = exec(torCmdString, true);
//        } catch (Exception e) {
//            logNotice("Tor was unable to start: " + e.getMessage(), e);
//            return false;
//        }
//
//        if (exitCode != 0) {
//            logNotice("Tor did not start. Exit:" + exitCode);
//            return false;
//        }
//
//        return true;
//    }
//
//
//    private int exec(String cmd, boolean wait) throws Exception {
//        CommandResult shellResult = Shell.run(cmd);
//
//        //  debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());
//
//        if (!shellResult.isSuccessful()) {
//            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
//        }
//
//        return shellResult.exitCode;
//    }
//}
