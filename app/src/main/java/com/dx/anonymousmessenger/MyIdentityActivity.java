package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.util.Hex;

import java.util.Objects;

public class MyIdentityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_my_identity);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.action_my_identity);
        getSupportActionBar().setSubtitle(R.string.my_identity_explanation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView tv = findViewById(R.id.txt_identity_key);
        new Thread(()->{
            try{
                String identity = Hex.toString(((DxApplication) getApplication()).getEntity().getStore().getIdentityKeyPair().getPublicKey().serialize());
                runOnUiThread(()->{
                    tv.setText(identity);
                });
            }catch (Exception ignored) {
                runOnUiThread(()->{
                    tv.setText(R.string.identity_key_fail);
                });
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}