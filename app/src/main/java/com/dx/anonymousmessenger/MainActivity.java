package com.dx.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.dx.anonymousmessenger.R;

import java.io.File;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setTheme(R.style.AppTheme);
        Objects.requireNonNull(getSupportActionBar()).hide();

        setContentView(R.layout.activity_main);

        //if service running we wait

        //check if account in storage, then change the next button text to login
        ((DxApplication) getApplication()).enableStrictMode();
        new Thread(() -> {
            File file =  new File(getFilesDir(), "demo.db");
            if(file.exists()) //here's how to check
            {
                switchToAppView();
            }else{
                if(((DxApplication)getApplication()).isServiceRunningInForeground(this, MyService.class)&&(((DxApplication) getApplication()).getHostname()==null)&&!(((DxApplication) getApplication()).isServerReady())){
                    Intent intent = new Intent(this, SetupInProcess.class);
                    startActivity(intent);
                    finish();
                }
            }
        }).start();
    }

    public void onNextClick(View view){
        Intent intent = new Intent(this, CreateUserActivity.class);
        startActivity(intent);
        finish();
    }

    public void switchToAppView(){
        Intent intent = new Intent(this, AppActivity.class);
        startActivity(intent);
        finish();
    }
}