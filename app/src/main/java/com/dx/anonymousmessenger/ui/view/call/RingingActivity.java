package com.dx.anonymousmessenger.ui.view.call;

import android.os.Bundle;
import android.view.WindowManager;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;

import java.util.Objects;

public class RingingActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_ringing);
    }
}