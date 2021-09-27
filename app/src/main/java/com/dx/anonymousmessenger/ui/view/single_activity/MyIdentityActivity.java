package com.dx.anonymousmessenger.ui.view.single_activity;

import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Hex;

public class MyIdentityActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_my_identity);
        try{
            setTitle(R.string.action_my_identity);
            setSubtitle(R.string.my_identity_explanation);
            setBackEnabled(true);
        }catch (Exception ignored){}
        TextView tv = findViewById(R.id.txt_identity_key);
        new Thread(()->{
            try{
                String identity = Hex.toString(((DxApplication) getApplication()).getEntity().getStore().getIdentityKeyPair().getPublicKey().serialize());
                runOnUiThread(()-> tv.setText(identity));
            }catch (Exception ignored) {
                runOnUiThread(()-> tv.setText(R.string.identity_key_fail));
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}