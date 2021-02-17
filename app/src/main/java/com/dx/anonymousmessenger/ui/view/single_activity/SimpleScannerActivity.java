package com.dx.anonymousmessenger.ui.view.single_activity;

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
                    if(TorClientSocks4.testAddress((DxApplication)getApplication(),s.trim())){
                        if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(s.trim(),1))){
                            MessageSender.sendKeyExchangeMessage((DxApplication)getApplication(),s.trim());
                        }
                    }
                }).start();
                runOnUiThread(()->{
                    Snackbar.make(mScannerView, R.string.contact_added,Snackbar.LENGTH_SHORT).show();
                });
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

