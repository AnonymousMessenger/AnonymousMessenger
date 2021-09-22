package com.dx.anonymousmessenger.ui.view.single_activity;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.setup.SetupSettingsFragment;

public class SettingsActivity extends DxActivity {

    @Override
    public boolean onSupportNavigateUp() {
        this.onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_settings);

        showNextFragment(SetupSettingsFragment.newInstance(false));
    }

    public void showNextFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, f)
                .commit();
    }
}