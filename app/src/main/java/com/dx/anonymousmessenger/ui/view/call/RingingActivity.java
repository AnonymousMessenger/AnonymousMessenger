package com.dx.anonymousmessenger.ui.view.call;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.R;

import java.util.Objects;

public class RingingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_ringing);
    }
}