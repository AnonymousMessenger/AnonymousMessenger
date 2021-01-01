package com.dx.anonymousmessenger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.db.DbHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class ContactProfileActivity extends AppCompatActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_contact_profile);

        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.action_my_profile);
            }
        }catch (Exception ignored){}

        final TextView address = findViewById(R.id.txt_myaddress);
        final TextView nickname = findViewById(R.id.txt_nickname);
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
                    if(getSupportActionBar()!=null){
                        getSupportActionBar().setTitle(nickname1);
                    }
                }catch (Exception ignored){}
            });
        }).start();

        address.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", address.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Snackbar.make(address, R.string.copied_address, Snackbar.LENGTH_LONG).show();
        });
    }
}