package com.dx.anonymousmessenger.ui.view.single_activity;

import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Hex;

import org.whispersystems.libsignal.SignalProtocolAddress;

public class VerifyIdentityActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_verify_identity);
        setTitle(R.string.action_verify_identity);
        setSubtitle(R.string.verify_identity_explanation);
        ((TextView)findViewById(R.id.txt_identity_info)).setText(R.string.verify_identity_how_to);
        setBackEnabled(true);
        TextView tv = findViewById(R.id.txt_identity_key);
        new Thread(()->{
            try{
                final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                        "address"),
                        (DxApplication) getApplication());
                if(fullAddress == null){
                    return;
                }
                byte[] myKey = ((DxApplication) getApplication()).getEntity().getStore().getIdentityKeyPair().getPublicKey().serialize();
                byte[] theirKey =
                        ((DxApplication) getApplication()).getEntity().getStore().getIdentity(new SignalProtocolAddress(fullAddress,1)).serialize();
                if(myKey==theirKey){
                    runOnUiThread(()-> tv.setText(Hex.toString(myKey)));
                }else{
                    runOnUiThread(()-> tv.setText(Hex.toString(myKey,theirKey)));
                }
            }catch (Exception ignored) {
                runOnUiThread(()-> tv.setText(R.string.identity_verification_fail));
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}