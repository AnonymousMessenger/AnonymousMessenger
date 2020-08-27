package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.util.Hex;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.Objects;

public class VerifyIdentityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_verify_identity);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.action_verify_identity);
        getSupportActionBar().setSubtitle(R.string.verify_identity_explanation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView tv = findViewById(R.id.txt_identity_key);
        //todo thread this shit
        byte[] myKey = ((DxApplication) getApplication()).getEntity().getStore().getIdentityKeyPair().getPublicKey().serialize();
        byte[] theirKey = ((DxApplication) getApplication()).getEntity().getStore().getIdentity(new SignalProtocolAddress(getIntent().getStringExtra("address"),1)).serialize();
        if(myKey==theirKey){
            tv.setText(Hex.toString(myKey));
        }else{
            tv.setText(Hex.toString(myKey,theirKey));
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}