package com.dx.anonymousmessenger;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dx.anonymousmessenger.tor.TorClientSocks4;

import java.io.IOException;

public class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {

    final NetworkRequest networkRequest;
    Context context;
    boolean connect = false;

    public ConnectionStateMonitor() {
        networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }

    public void enable(Context context) {
        this.context=context;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(networkRequest, this);
    }

    public void disable(){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.unregisterNetworkCallback(this);
    }
    // Likewise, you can have a disable method that simply calls ConnectivityManager.unregisterNetworkCallback(NetworkCallback) too.

    @Override
    public void onAvailable(Network network) {
        new Thread(()->{
            if(((DxApplication)context).getTorSocket()!=null&&((DxApplication)context).getTorSocket().getAndroidTorRelay()!=null){
                ((DxApplication)context).sendNotification("Restarting service","connection should be back soon",false);
                new Thread(()->{
                    while (!new TorClientSocks4().test((DxApplication) context)){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    ((DxApplication)context).sendNotification("Connection Restored","should be able to receive messages within 2 minutes",false);
                    Intent gcm_rec = new Intent("your_action");
                    LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(gcm_rec);
                }).start();
            }

            if(isServiceRunningInForeground(context,MyService.class)){
                Intent serviceIntent = new Intent(context, MyService.class);
                serviceIntent.putExtra("inputExtra", "reconnect now");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                }else{
                    context.startService(serviceIntent);
                }
            }else{
                Intent serviceIntent = new Intent(context, MyService.class);
                serviceIntent.putExtra("inputExtra", "cold start");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                }else{
                    context.startService(serviceIntent);
                }
            }
        }).start();
    }

    @Override
    public void onLost(@NonNull Network network) {
        new Thread(()->{
            ((DxApplication)context).sendNotification("Lost Connection","waiting for connectivity to restore",false);
            Intent gcm_rec = new Intent("your_action");
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(gcm_rec);
            if(((DxApplication)context).getTorSocket()!=null&&((DxApplication)context).getTorSocket().getAndroidTorRelay()!=null){
                try {
                    ((DxApplication)context).getTorSocket().getAndroidTorRelay().shutDown();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}
