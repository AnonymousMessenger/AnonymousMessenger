package com.dx.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            Objects.requireNonNull(getSupportActionBar()).hide();
        }catch (Exception ignored){}

        setContentView(R.layout.activity_main);

        ((DxApplication) getApplication()).enableStrictMode();
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File file =  new File(getFilesDir(), "demo.db");
            if(file.exists()) //here's how to check
            {
                switchToAppView();
            }else{
                if(((DxApplication)getApplication()).isServiceRunningInForeground(this, MyService.class)&&(((DxApplication) getApplication()).getHostname()==null)&&!(((DxApplication) getApplication()).isServerReady())){
                    switchToSetupInProcess();
                }else{
                    runOnUiThread(()->{
                        Button next = findViewById(R.id.next);
                        next.setVisibility(View.VISIBLE);
                        next.setEnabled(true);
                    });
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

    public void switchToSetupInProcess(){
        Intent intent = new Intent(this, SetupInProcess.class);
        startActivity(intent);
        finish();
    }
}