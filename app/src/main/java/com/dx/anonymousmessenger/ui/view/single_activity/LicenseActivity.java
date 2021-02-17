package com.dx.anonymousmessenger.ui.view.single_activity;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.R;

public class LicenseActivity extends AppCompatActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_license);

        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.action_license);
            }
        }catch (Exception ignored){}
    }
}