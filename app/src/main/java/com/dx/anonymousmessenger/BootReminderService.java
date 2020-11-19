package com.dx.anonymousmessenger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BootReminderService extends Service {
    public BootReminderService() {
    }

    @Override
    public void onCreate() {
        DxApplication app = (DxApplication) getApplication();
        app.sendNotification(getString(R.string.decrypt_reminder_title),getString(R.string.decrypt_reminder_message),false);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}