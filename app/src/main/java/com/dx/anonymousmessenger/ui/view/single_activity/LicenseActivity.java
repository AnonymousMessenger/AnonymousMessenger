package com.dx.anonymousmessenger.ui.view.single_activity;

import android.os.Bundle;
import android.view.WindowManager;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;

public class LicenseActivity extends DxActivity {

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
            setTitle(R.string.action_license);
            setBackEnabled(true);
        }catch (Exception ignored){}
    }
}