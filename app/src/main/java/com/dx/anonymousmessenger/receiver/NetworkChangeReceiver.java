package com.dx.anonymousmessenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.dx.anonymousmessenger.util.NetworkUtils;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        String status = NetworkUtils.getConnectivityStatusString(context);

        Toast.makeText(context, status, Toast.LENGTH_LONG).show();
    }
}