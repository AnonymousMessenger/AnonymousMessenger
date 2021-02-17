package com.dx.anonymousmessenger.ui.view.single_activity;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.R;

import java.text.MessageFormat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        setContentView(R.layout.activity_about);

        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.action_about);
            }
        }catch (Exception ignored){}

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView version = findViewById(R.id.txt_version);
            version.setText(MessageFormat.format("v{0} - {1}", pInfo.versionName, pInfo.versionCode));
        }catch (Exception ignored){}
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}