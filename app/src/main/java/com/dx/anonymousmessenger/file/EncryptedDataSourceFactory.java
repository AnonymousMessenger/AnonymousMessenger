package com.dx.anonymousmessenger.file;

import androidx.annotation.NonNull;

import com.dx.anonymousmessenger.DxApplication;
import com.google.android.exoplayer2.upstream.DataSource;

public class EncryptedDataSourceFactory implements DataSource.Factory {
    private byte[] key;
    private DxApplication app;
    public EncryptedDataSourceFactory(byte[] key, DxApplication app){
        this.key = key;
        this.app = app;
    }

    @NonNull
    @Override
    public DataSource createDataSource(){
        return new EncryptedDataSource(key,app);
    }
}

