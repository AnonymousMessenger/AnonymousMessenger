package com.dx.anonymousmessenger.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;

import java.io.File;

public class BootReminderService extends Service {
    public BootReminderService() {
    }

    @Override
    public void onCreate() {
        DxApplication app = (DxApplication) getApplication();
        try{
            File databaseFile = new File(getFilesDir(), "demo.db");
            if(databaseFile.exists()){
                app.sendNotification(getString(R.string.decrypt_reminder_title),getString(R.string.decrypt_reminder_message),false, R.drawable.ic_baseline_lock_24);
            }
        }catch (Exception ignored) {}
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}