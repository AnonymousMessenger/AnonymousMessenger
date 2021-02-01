package com.dx.anonymousmessenger.qr;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.Result;

import org.whispersystems.libsignal.SignalProtocolAddress;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SimpleScannerActivity extends Activity implements ZXingScannerView.ResultHandler {
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
        mScannerView.setBorderColor(getColor(R.color.dx_night_940));
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

        new Thread(()->{
            //add the contact if valid
            if(s.equals(((DxApplication)getApplication()).getHostname())){
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.cant_add_self,Snackbar.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }
            if(s.trim().length()<56 || !s.trim().endsWith(".onion")){
                runOnUiThread(()->{
                    mScannerView.resumeCameraPreview(this);
                });
                return;
            }
            try{
                if(DbHelper.contactExists(s,(DxApplication)getApplication())){
                    runOnUiThread(()->{
                        Snackbar.make(mScannerView, R.string.contact_exists,Snackbar.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                boolean b = DbHelper.saveContact(s.trim(), ((DxApplication) getApplication()));
                if(!b){
                    Log.e("FAILED TO SAVE CONTACT", "SAME " );
                    runOnUiThread(()->{
                        Snackbar.make(mScannerView, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.contact_added,Snackbar.LENGTH_SHORT).show();
                    if (getParent() == null) {
                        setResult(Activity.RESULT_OK);
                    } else {
                        getParent().setResult(Activity.RESULT_OK);
                    }
                });
                if(TorClientSocks4.testAddress((DxApplication)getApplication(),s.trim())){
                    if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(s.trim(),1))){
                        MessageSender.sendKeyExchangeMessage((DxApplication)getApplication(),s.trim());
                    }
                }
                finish();
                return;
            }catch (Exception e){
                e.printStackTrace();
                Log.e("FAILED TO SAVE CONTACT", "SAME " );
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                    finish();
                });
            }
            // If you would like to resume scanning, call this method below:
            mScannerView.resumeCameraPreview(this);
        }).start();
    }
}

