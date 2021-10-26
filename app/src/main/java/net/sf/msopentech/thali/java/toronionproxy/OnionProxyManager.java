
package net.sf.msopentech.thali.java.toronionproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.service.DxService;

import net.sf.freehaven.tor.control.ConfigEntry;
import net.sf.freehaven.tor.control.TorControlConnection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


public class OnionProxyManager {
    private static final String[] EVENTS = {
            "CIRC", "ORCONN", "NOTICE", "WARN", "ERR"
    };
    private static final String OWNER = "__OwningControllerProcess";
    private static Process torProcess;
    protected final OnionProxyContext onionProxyContext;
    private volatile Socket controlSocket = null;
    private boolean stopping = false;

    // If controlConnection is not null then this means that a connection exists and the Tor OP will die when the connection fails.
    private volatile TorControlConnection controlConnection = null;
    private volatile int control_port;
    private final OnionProxyManagerEventHandler eventHandler;

    public OnionProxyContext getOnionProxyContext() {
        return onionProxyContext;
    }

    public OnionProxyManager(OnionProxyContext onionProxyContext) {
        this.onionProxyContext = onionProxyContext;
        eventHandler = new OnionProxyManagerEventHandler(onionProxyContext);
    }

    //another constructor to create hidden service keys even offline by tor
    public OnionProxyManager(OnionProxyContext onionProxyContext, int localHiddenServicePort) {
        this.onionProxyContext = onionProxyContext;
        eventHandler = new OnionProxyManagerEventHandler(onionProxyContext);
    }

    public boolean startWithoutRepeat(int hiddenServicePort, int localPort, int secondsBeforeTimeOut, List<String> bridges, boolean enableBridges, boolean enableSocks5Proxy, String socks5AddressAndPort, String socks5Username, String socks5Password, String excludeText, boolean excludeUnknown, boolean strictExclude) throws IOException, InterruptedException, RuntimeException {

        File hostnameFile = onionProxyContext.getHostNameFile();

        if (!Objects.requireNonNull(hostnameFile.getParentFile()).exists()
                && !hostnameFile.getParentFile().mkdirs()) {
            throw new RuntimeException("Could not create hostnameFile parent directory");
        }

        if (!hostnameFile.exists() && !hostnameFile.createNewFile()) {
            throw new RuntimeException("Could not create hostnameFile");
        }

        if (!installAndStartTorOp(hiddenServicePort,localPort,bridges, enableBridges, enableSocks5Proxy, socks5AddressAndPort, socks5Username, socks5Password, excludeText, excludeUnknown, strictExclude)) {
//            stop();
//            onionProxyContext.deleteAllFilesButHiddenServices();
            return false;
        }
        enableNetwork(true);
//        return true;

        // We will check every second to see if boot strapping has finally finished
        for (int secondsWaited = 0; secondsWaited < secondsBeforeTimeOut; ++secondsWaited) {
            if (!isBootstrapped()) {
                Thread.sleep(1000, 0);
            } else {
                return true;
            }
        }

        //Bootstrapping isn't over, we need to return false to indicate fail
        stop();
        onionProxyContext.deleteAllFilesButHiddenServices();
        return false;
    }

    public synchronized int getIPv4LocalHostSocksPort() throws IOException {
        if (!isRunning()) {
            throw new RuntimeException("Tor is not running!");
        }

        // This returns a set of space delimited quoted strings which could be Ipv4, Ipv6 or unix sockets
        String[] socksIpPorts = controlConnection.getInfo("net/listeners/socks").split(" ");

        for (String address : socksIpPorts) {
            if (address.contains("\"127.0.0.1:")) {
                // Remember, the last character will be a " so we have to remove that
                return Integer.parseInt(address.substring(address.lastIndexOf(":") + 1, address.length() - 1));
            }
        }

        throw new RuntimeException("We don't have an Ipv4 localhost binding for socks!");
    }

    public synchronized String publishHiddenService(int hiddenServicePort, int localPort) throws IOException {
        if (controlConnection == null) {
            throw new RuntimeException("Service is not running.");
        }

       /*
       List<ConfigEntry> currentHiddenServices = controlConnection.getConf("HiddenServiceOptions");
       if (!(currentHiddenServices.size() == 1
                && currentHiddenServices.get(0).key.compareTo("HiddenServiceOptions") == 0
                && currentHiddenServices.get(0).value.compareTo("") == 0)) {
           throw new RuntimeException("Sorry, only one hidden service to a customer and we already have one. Please send complaints to https://github.com/thaliproject/Tor_Onion_Proxy_Library/issues/5 with your scenario so we can justify fixing this.");
        }*/

        File hostnameFile = onionProxyContext.getHostNameFile();

        if (!Objects.requireNonNull(hostnameFile.getParentFile()).exists()
                && !hostnameFile.getParentFile().mkdirs()) {
            throw new RuntimeException("Could not create hostnameFile parent directory");
        }

        if (!hostnameFile.exists() && !hostnameFile.createNewFile()) {
            throw new RuntimeException("Could not create hostnameFile");
        }

        // Use the control connection to update the Tor config
        List<String> config = Arrays.asList(
                "HiddenServiceDir " + hostnameFile.getParentFile().getAbsolutePath(),
                "HiddenServicePort " + hiddenServicePort + " 127.0.0.1:" + localPort,
                "HiddenServiceMaxStreams " + 50);
        controlConnection.setConf(config);
        controlConnection.saveConf();

        return new String(FileUtilities.read(hostnameFile), StandardCharsets.UTF_8).trim();
    }

    public synchronized boolean installAndStartTorOp(int hiddenServicePort, int localPort, List<String> bridges, boolean enableBridges, boolean enableSocks5Proxy, String socks5AddressAndPort, String socks5Username, String socks5Password, String excludeText, boolean excludeUnknown, boolean strictExclude) throws IOException, InterruptedException {
        // The Tor OP will die if it looses the connection to its socket so if there is no controlSocket defined
        // then Tor is dead. This assumes, of course, that takeOwnership works and we can't end up with Zombies.
        if (controlConnection != null) {
            Log.d("ANONYMOUSMESSENGER","Tor is already running");
            return true;
        }

        // The code below is why this method is synchronized, we don't want two instances of it running at once
        // as the result would be a mess of screwed up files and connections.
        Log.d("ANONYMOUSMESSENGER","Tor is not running");

        onionProxyContext.installFiles();
        setExecutable(onionProxyContext.getTorExecutableFile());
        // write configuration to torrc file
        try (PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(onionProxyContext.getTorrcFile(), true)))) {
            printWriter.println("CookieAuthFile " + onionProxyContext.getCookieFile().getAbsolutePath());
            printWriter.println("DataDirectory " + onionProxyContext.getWorkingDirectory().getAbsolutePath());
            printWriter.println("GeoIPFile " + onionProxyContext.getGeoIpFile().getAbsolutePath());
            printWriter.println("GeoIPv6File " + onionProxyContext.getGeoIpv6File().getAbsolutePath());

            printWriter.println("HiddenServiceDir " + Objects.requireNonNull(onionProxyContext.getHostNameFile().getParentFile()).getAbsolutePath());
            printWriter.println("HiddenServicePort " + hiddenServicePort + " 127.0.0.1:" + localPort);
            printWriter.println("HiddenServiceMaxStreams " + 50);

            if(enableSocks5Proxy && socks5AddressAndPort!=null && !socks5AddressAndPort.isEmpty()){
                printWriter.println("Socks5Proxy " + socks5AddressAndPort);
                if(socks5Username!=null && !socks5Username.isEmpty()){
                    printWriter.println("Socks5ProxyUsername " + socks5Username);
                }
                if(socks5Password!=null && !socks5Password.isEmpty()){
                    printWriter.println("Socks5ProxyPassword " + socks5Password);
                }
            }
            if(excludeText!=null && !excludeText.isEmpty()){
                printWriter.println("ExcludeNodes " + excludeText);
                printWriter.println("GeoIPExcludeUnknown " + (excludeUnknown?"1":"0"));
                printWriter.println("StrictNodes " + (strictExclude?"1":"0"));
            }
            String obfs4proxyfilename = getOnionProxyContext().getTorExecutableFileName().replace("libtor", "obfs4proxy");
            File obfs4proxyfile = new File(getOnionProxyContext().ctx.getApplicationInfo().nativeLibraryDir + "/" + obfs4proxyfilename);
            if (obfs4proxyfile.exists()) {
                Log.d("ANONYMOUSMESSENGER","OBFS4PROXY EXISTS");
                printWriter.println("ClientTransportPlugin meek_lite,obfs2,obfs3,obfs4,scramblesuit exec " + obfs4proxyfile.getAbsolutePath());
                printWriter.println("UseBridges " + (enableBridges ? "1" : "0"));
                for (String bridge : bridges) {
                    printWriter.println("Bridge " + bridge);
                }
            }
        }

        Log.d("ANONYMOUSMESSENGER","Starting Tor");
//        Intent gcm_rec = new Intent("tor_status");
//        gcm_rec.putExtra("tor_status","Starting Tor");
//        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
        File cookieFile = onionProxyContext.getCookieFile();
        if (!Objects.requireNonNull(cookieFile.getParentFile()).exists()
                && !cookieFile.getParentFile().mkdirs()) {
            throw new RuntimeException("Could not create cookieFile parent directory");
        }

        if (!cookieFile.exists() && !cookieFile.createNewFile()) {
            throw new RuntimeException("Could not create cookieFile");
        }

        // Start a new Tor process
        String torPath = onionProxyContext.getTorExecutableFile().getAbsolutePath();
        String configDir = onionProxyContext.getTorrcFile().getAbsoluteFile().getParent();
        String configPath = onionProxyContext.getTorrcFile().getAbsolutePath();
        final File directory = new File(Objects.requireNonNull(configDir));

        //delete old torrc files that tor keeps copying
        final File[] files = directory.listFiles((dir, name) -> name.matches( "torrc.txt.orig.*" ));
        if (files != null) {
            for ( final File file : files ) {
                if ( !file.delete() ) {
                    System.err.println( "Can't remove " + file.getAbsolutePath() );
                }else{
                    Log.d("ANONYMOUSMESSENGER", "Removed " + file.getAbsolutePath() );
                }
            }
        }

        if(!new File(torPath).exists()){
            Log.d("ANONYMOUSMESSENGER",torPath+" doesn't exist !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }

        String pid = onionProxyContext.getProcessId();
        String[] cmd = {onionProxyContext.getTorExecutableFile().getAbsolutePath(), "-f", configPath, OWNER, pid};
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(new File(onionProxyContext.ctx.getApplicationInfo().nativeLibraryDir));
        processBuilder.redirectErrorStream(true);

        onionProxyContext.setEnvironmentArgsAndWorkingDirectoryForStart(processBuilder);
        if(torProcess!=null){
            torProcess.destroy();
        }
        torProcess = null;
        try {
            torProcess = processBuilder.start();
            eatStream(torProcess.getInputStream(), false);
            eatStream(torProcess.getErrorStream(), true);

            //for now we run as a daemon so we need to wait for the process to
            //detach, but soon we will change to using the process and not
            //letting tor run as a daemon to gain more control over the shutdown
            //process and to get tor output directly from i/o stream of process.
//            int exit = torProcess.waitFor();
//            torProcess = null;
//            if (exit != 0) {
//                Log.d("ANONYMOUSMESSENGER","Tor exited with value " + exit);
//                return false;
//            }

            Thread.sleep(500);

            controlSocket = new Socket("127.0.0.1", control_port);

            // Open a control connection and authenticate using the cookie file
            TorControlConnection controlConnection = new TorControlConnection(controlSocket);
            Log.d("ANONYMOUSMESSENGER","authenticating");
            controlConnection.authenticate(FileUtilities.read(cookieFile));
            Log.d("ANONYMOUSMESSENGER","authenticated");
            // Tell Tor to exit when the control connection is closed
            controlConnection.takeOwnership();
            controlConnection.resetConf(Collections.singletonList(OWNER));
            // Register to receive events from the Tor process
            controlConnection.setEventHandler(eventHandler);
            controlConnection.setEvents(Arrays.asList(EVENTS));
            // We only set the class property once the connection is in a known good state
            this.controlConnection = controlConnection;
            // Register to receive network status events
            NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
            IntentFilter filter = new IntentFilter(CONNECTIVITY_ACTION);
            onionProxyContext.ctx.registerReceiver(networkStateReceiver, filter);
            Log.d("ANONYMOUSMESSENGER","done with tor startup");
            return true;
        } catch (SecurityException e) {
            if (torProcess != null) {
                torProcess.destroy();
            }
            e.printStackTrace();
        } finally {
            if (controlConnection == null && torProcess != null) {
                // It's possible that something 'bad' could happen after we executed exec but before we takeOwnership()
                // in which case the Tor OP will hang out as a zombie until this process is killed. This is problematic
                // when we want to do things like
                torProcess.destroy();
            }
        }
        return false;
    }

    protected void eatStream(final InputStream inputStream, final boolean stdError) {
        new Thread() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(inputStream);
                try {
                    while (scanner.hasNextLine()) {
                        if (stdError) {
                            Log.d("ANONYMOUSMESSENGER",scanner.nextLine());
                            Intent gcm_rec = new Intent("tor_status");
                            gcm_rec.putExtra("tor_status",scanner.nextLine());
                            LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
                        } else {
                            String nextLine = scanner.nextLine();
                            // We need to find the line where it tells us what the control port is.
                            if (nextLine.contains("Control listener listening on port ")) {
                                control_port
                                        = Integer.parseInt(
                                        nextLine.substring(nextLine.lastIndexOf(" ") + 1, nextLine.length() - 1));
                            }
                            Log.d("ANONYMOUSMESSENGER",nextLine);
                            Intent gcm_rec = new Intent("tor_status");
                            gcm_rec.putExtra("tor_status",nextLine);
                            LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
                        }
                    }
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.d("ANONYMOUSMESSENGER","Couldn't close input stream in eatStream"+ e);
                    }
                }
            }
        }.start();
    }

    protected boolean setExecutable(File f) {
        return f.setExecutable(true, true);
    }

    public File getWorkingDirectory() {
        return onionProxyContext.getWorkingDirectory();
    }

    public synchronized void clearTorCache() throws IOException {
        if (controlConnection == null) {
            throw new RuntimeException("Tor is not running!");
        }
        controlConnection.signal("NEWNYM");
    }

    public void stop() throws IOException {
        if(stopping){
            Log.d("ANONYMOUSMESSENGER","already stopping");
            return;
        }
        stopping = true;
        Log.d("ANONYMOUSMESSENGER","starting stop");
        try {
            if(torProcess!=null){
                Log.d("ANONYMOUSMESSENGER","process not null so we are destroying it");
                torProcess.destroy();
                torProcess.waitFor();
                Log.d("ANONYMOUSMESSENGER","done with process destruction");
            }
            if (controlConnection != null) {
                Log.d("ANONYMOUSMESSENGER","control connection not null");
                controlConnection.setConf("DisableNetwork", "1");
                controlConnection.shutdownTor("TERM");
                System.err.println("SIGTERM SENT !!!!!!!!!!!!!!!!!!!");
                controlConnection = null;
                Log.d("ANONYMOUSMESSENGER","nullified control connection successfully");
            }

            Log.d("ANONYMOUSMESSENGER","pkilling tor");
            String[] cmd = {"pkill", "tor"};
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            try {
                torProcess = processBuilder.start();
                eatStream(torProcess.getInputStream(), false);
                eatStream(torProcess.getErrorStream(), true);
            }catch (Exception e){
                e.printStackTrace();
            }
            Log.d("ANONYMOUSMESSENGER","done with pkilling tor");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            controlConnection = null;
            if (controlSocket != null) {
                controlSocket.close();
            }
            torProcess = null;
            controlSocket = null;
        }
        stopping = false;
    }

    public synchronized boolean isRunning() throws IOException {
        return isBootstrapped() && isNetworkEnabled();
    }

    public synchronized void enableNetwork(boolean enable) throws IOException {
        if (controlConnection == null) {
            throw new RuntimeException("Tor is not running!");
        }
        controlConnection.setConf("DisableNetwork", enable ? "0" : "1");
//        LOG.info("Enabling network: " + enable);
//        Intent gcm_rec = new Intent("tor_status");
//        gcm_rec.putExtra("tor_status","Enabling network: " + enable);
//        LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
    }

    public synchronized boolean isNetworkEnabled() throws IOException {
        if (controlConnection == null) {
            throw new RuntimeException("Tor is not running!");
        }

        List<ConfigEntry> disableNetworkSettingValues = controlConnection.getConf("DisableNetwork");
        boolean result = false;
        // It's theoretically possible for us to get multiple values back, if even one is false then we will
        // assume all are false
        for (ConfigEntry configEntry : disableNetworkSettingValues) {
            if (configEntry.value.equals("1")) {
                return false;
            } else {
                result = true;
            }
        }
        return result;
    }

    public synchronized boolean isBootstrapped() {
        if (controlConnection == null) {
            return false;
        }

        String phase = null;
        try {
            phase = controlConnection.getInfo("status/bootstrap-phase");
//            Intent gcm_rec = new Intent("tor_status");
//            gcm_rec.putExtra("tor_status","Bootstrapped: "+phase);
//            LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
        } catch (IOException e) {
            Log.d("ANONYMOUSMESSENGER","Control connection is not responding properly to getInfo "+e);
        }

        return phase != null && phase.contains("PROGRESS=100");
    }

    public boolean isTorRunning(){
        try{
            isNetworkEnabled();
        }catch (Exception ignored){}
        return controlConnection != null;
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent i) {
//            try {
//                if(!isRunning()) return;
//            } catch (IOException e) {
//                Log.d("ANONYMOUSMESSENGER","Did someone call before Tor was ready?"+ e);
//                return;
//            }
            boolean online = !i.getBooleanExtra(EXTRA_NO_CONNECTIVITY, false);
            if(online) {
                // Some devices fail to set EXTRA_NO_CONNECTIVITY, double check
                Object o = ctx.getSystemService(CONNECTIVITY_SERVICE);
                ConnectivityManager cm = (ConnectivityManager) o;
                NetworkInfo net = cm.getActiveNetworkInfo();
                if(net == null || !net.isConnected()) online = false;
            }
            Log.d("ANONYMOUSMESSENGER","Online: " + online);
            try {
                enableNetwork(online);
                //todo: if tor is not running then start tor
                if(!DxApplication.isServiceRunningInForeground(ctx, DxService.class)){
                    return;
                }
                // broadcast to the service to start syncing
                Intent gcm_rec = new Intent("dx_service");
                gcm_rec.putExtra("start_syncing",1);
                LocalBroadcastManager.getInstance(onionProxyContext.ctx).sendBroadcast(gcm_rec);
            } catch(Exception e) {
                Log.d("ANONYMOUSMESSENGER",e.toString()+e);
            }
        }
    }

}