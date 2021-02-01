package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.util.Hex;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.Objects;

public class VerifyIdentityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_verify_identity);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.action_verify_identity);
        getSupportActionBar().setSubtitle(R.string.verify_identity_explanation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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