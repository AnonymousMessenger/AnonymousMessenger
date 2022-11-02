package com.dx.anonymousmessenger.ui.view.single_activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class DonateActivity extends DxActivity {

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
        setContentView(R.layout.activity_donate);

        try{
            setTitle(R.string.action_donate);
            setBackEnabled(true);
        }catch (Exception ignored){}

        TextView btcAddress = findViewById(R.id.txt_bitcoin_address);
        btcAddress.setOnClickListener((l) -> {
            try{
                ClipboardManager clipboard = ContextCompat.getSystemService(this, ClipboardManager.class);
                ClipData clip = ClipData.newPlainText("label", btcAddress.getText());
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                Snackbar.make(btcAddress.getRootView(), R.string.copied, Snackbar.LENGTH_SHORT).setAnchorView(btcAddress).show();
            }catch (Exception ignored) {}
        });
        TextView xmrAddress = findViewById(R.id.txt_monero_address);
        xmrAddress.setOnClickListener((l) -> {
            try{
                ClipboardManager clipboard = ContextCompat.getSystemService(this, ClipboardManager.class);
                ClipData clip = ClipData.newPlainText("label", xmrAddress.getText());
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                Snackbar.make(xmrAddress.getRootView(), R.string.copied, Snackbar.LENGTH_SHORT).setAnchorView(xmrAddress).show();
            }catch (Exception ignored) {}
        });
    }
}