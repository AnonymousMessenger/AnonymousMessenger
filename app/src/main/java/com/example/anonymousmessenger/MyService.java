package com.example.anonymousmessenger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyService extends Service {
    private final String DB_FILE_NAME = "dxam.db";
    private String signalProtocolStore;
    private String messageAdapter;
    private String dbAdapter;
    private String netAdapter;
    private String torAdapter;
    private String webAdapter;
    private String kj;

    public MyService() {
    }

    protected void createAccount(){

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return 1;
    }
}
