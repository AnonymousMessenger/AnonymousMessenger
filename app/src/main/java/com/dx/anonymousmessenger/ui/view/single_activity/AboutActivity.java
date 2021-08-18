package com.dx.anonymousmessenger.ui.view.single_activity;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;

import java.text.MessageFormat;

public class AboutActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_about);

        try{
            setTitle(R.string.action_about);
            setBackEnabled(true);
        }catch (Exception ignored){}

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView version = findViewById(R.id.txt_version);
            version.setText(MessageFormat.format("v{0} - {1}", pInfo.versionName, pInfo.versionCode));

            TextView sysInfo = findViewById(R.id.txt_sysinfo);
            String info = getString(R.string.running_on)+System.getProperty("os.arch")+" "+getString(R.string.with)+" "+Runtime.getRuntime().availableProcessors()+" "+getString(R.string.processors)+" "+getString(R.string.and)+" "+Utils.humanReadableByteCount(Runtime.getRuntime().freeMemory())+" "+getString(R.string.free_memory);
            sysInfo.setText(info);

        }catch (Exception ignored){}
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }


}