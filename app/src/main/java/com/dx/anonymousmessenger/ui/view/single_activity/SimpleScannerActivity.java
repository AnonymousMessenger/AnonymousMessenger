package com.dx.anonymousmessenger.ui.view.single_activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.tor.TorClient;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.Result;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.Objects;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SimpleScannerActivity extends DxActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
//        mScannerView.setFlash(true);
//        mScannerView.setAutoFocus(true);
        mScannerView.setBorderColor(ContextCompat.getColor(this, R.color.dx_night_940));
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {
        String s = rawResult.getText();
        switch (Objects.requireNonNull(getIntent().getStringExtra("SCAN_MODE"))){
            case "ADD_CONTACT":
                addContact(s);
                break;
            case "ADD_BRIDGE":
                addBridge(s);
                break;
        }

    }
    private void addBridge(String s){
        if(Utils.isValidAddress(s)){
            Snackbar.make(mScannerView, R.string.invalid_bridge,Snackbar.LENGTH_SHORT).show();
            //continue scanning
            mScannerView.resumeCameraPreview(this);
            return;
        }

        Intent intent = new Intent();
        intent.putExtra("RESULT",s);
        setResult(Activity.RESULT_OK,intent);
        Snackbar.make(mScannerView, R.string.bridge_added,Snackbar.LENGTH_SHORT).show();
        new Thread(()->{
            try {
                Thread.sleep(500);
            } catch (Exception ignored) {}
            finish();
        }).start();

    }

    private void addContact(String s){
        new Thread(()->{
            //add the contact if valid
            if(s.equals(((DxApplication)getApplication()).getHostname())){
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.cant_add_self,Snackbar.LENGTH_SHORT).show();
                    mScannerView.resumeCameraPreview(this);
                });
                return;
            }
            if(s.trim().length()<56 || !s.trim().endsWith(".onion")){
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.invalid_address,Snackbar.LENGTH_SHORT).show();
                    //continue scanning
                    mScannerView.resumeCameraPreview(this);
                });
                return;
            }
            try{
                if(DbHelper.contactExists(s,(DxApplication)getApplication())){
                    runOnUiThread(()->{
                        Snackbar.make(mScannerView, R.string.contact_exists,Snackbar.LENGTH_SHORT).show();
                        mScannerView.resumeCameraPreview(this);
                    });
                    return;
                }

                boolean b = DbHelper.saveContact(s.trim(), ((DxApplication) getApplication()));
                if(!b){
                    Log.e("FAILED TO SAVE CONTACT", "SAME " );
                    runOnUiThread(()->{
                        Snackbar.make(mScannerView, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                        mScannerView.resumeCameraPreview(this);
                    });
                    return;
                }
                new Thread(()->{
                    if(TorClient.testAddress((DxApplication)getApplication(),s.trim())){
                        if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(s.trim(),1))){
                            MessageSender.sendKeyExchangeMessage((DxApplication)getApplication(),s.trim());
                        }
                    }
                }).start();
                runOnUiThread(()-> Snackbar.make(mScannerView, R.string.contact_added,Snackbar.LENGTH_SHORT).show());
                Thread.sleep(500);
                runOnUiThread(()->{
                    if (getParent() == null) {
                        setResult(Activity.RESULT_OK);
                    } else {
                        getParent().setResult(Activity.RESULT_OK);
                    }
                    finish();
                });
            }catch (Exception e){
                e.printStackTrace();
                Log.e("FAILED TO SAVE CONTACT", "SAME " );
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
}

