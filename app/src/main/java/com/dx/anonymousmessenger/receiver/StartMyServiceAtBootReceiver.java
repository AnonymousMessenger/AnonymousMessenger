package com.dx.anonymousmessenger.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.TaskStackBuilder;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.MainActivity;

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction()==null){
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
//            Intent serviceIntent = new Intent(context, BootReminderService.class);
//            context.startService(serviceIntent);

            Intent contentIntent = new Intent(context, MainActivity.class);
            String title = context.getString(R.string.decrypt_reminder_title);
            String msg = context.getString(R.string.decrypt_reminder_message);
            String CHANNEL_ID = "status_messages";
            Intent resultIntent = new Intent(context, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            PendingIntent resultPendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                resultPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_MUTABLE);
            }else
            {
                resultPendingIntent = PendingIntent.getActivity
                        (context, 0, contentIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            }
//            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = new Notification.Builder(context,CHANNEL_ID)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setContentIntent(resultPendingIntent)
                        .setAutoCancel(true)
                        .setChannelId(CHANNEL_ID).build();
            }else{
                notification = new Notification.Builder(context)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setContentIntent(resultPendingIntent)
                        .setAutoCancel(true)
                        .build();
            }
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                        CHANNEL_ID,NotificationManager.IMPORTANCE_HIGH));
            }
            // Issue the notification.
            mNotificationManager.notify(2 , notification);
        }
    }
}
