package com.dx.anonymousmessenger.ui.view.single_activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class ContactProfileActivity extends DxActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_contact_profile);

        try{
            setTitle(R.string.action_my_profile);
            setBackEnabled(true);
        }catch (Exception ignored){}

        final TextView address = findViewById(R.id.txt_myaddress);
        final TextView nickname = findViewById(R.id.txt_nickname);
        final TextView verifyIdentity = findViewById(R.id.btn_verify_identity);
        new Thread(()->{
            final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                    "address"),
                    (DxApplication) getApplication());
            if(fullAddress == null){
                return;
            }
            String nickname1 = DbHelper.getContactNickname(fullAddress, (DxApplication) getApplication());
            new Handler(Looper.getMainLooper()).post(()->{
                try{
                    address.setText(fullAddress);
                    nickname.setText(nickname1);
                    setTitle(nickname1);
                }catch (Exception ignored){}
            });
        }).start();

        address.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", address.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Snackbar.make(address, R.string.copied_address, Snackbar.LENGTH_LONG).show();
        });

        verifyIdentity.setOnClickListener(v -> {
            Intent intent = new Intent(this, VerifyIdentityActivity.class);
            intent.putExtra("address", Objects.requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
            startActivity(intent);
        });
    }
}