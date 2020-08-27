package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class CallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_call);

        TextView name = findViewById(R.id.name);
        name.setText(getIntent().getStringExtra("nickname"));
        TextView phone = findViewById(R.id.phoneNumber);
        phone.setText(getIntent().getStringExtra("address"));
        TextView state = findViewById(R.id.callStateLabel);
        state.setText(R.string.connecting);
        TextView timer = findViewById(R.id.elapsedTime);
        timer.setText("00:00");
        FloatingActionButton hangup = findViewById(R.id.hangup_fab);
        hangup.setOnClickListener(v -> {
            ((DxApplication)getApplication()).getCm().stop();
            finish();
        });
    }
}