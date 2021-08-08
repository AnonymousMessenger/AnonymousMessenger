package com.dx.anonymousmessenger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.service.DxService;

public class NotificationHiderReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
            .putExtra(Settings.EXTRA_CHANNEL_ID, DxService.SERVICE_NOTIFICATION_CHANNEL)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        Toast.makeText(context, R.string.hide_notification, Toast.LENGTH_LONG).show();
    }
}
