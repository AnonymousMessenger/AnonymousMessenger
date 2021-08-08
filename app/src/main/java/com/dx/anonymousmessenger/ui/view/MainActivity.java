package com.dx.anonymousmessenger.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.service.DxService;
import com.dx.anonymousmessenger.ui.view.app.AppActivity;
import com.dx.anonymousmessenger.ui.view.setup.CreateUserActivity;
import com.dx.anonymousmessenger.ui.view.setup.SetupInProcess;

import java.io.File;

public class MainActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        setContentView(R.layout.activity_main);

        ((DxApplication) getApplication()).enableStrictMode();
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                File file =  new File(getFilesDir(), "demo.db");
                //already has an account
                if(file.exists()) //here's how to check
                {
                    switchToAppView();
                }else{
                    //still setting up account
                    if(((DxApplication)getApplication()).isServiceRunningInForeground(this, DxService.class)){
                        switchToSetupInProcess();
                    }else{
                        //first time user
                        runOnUiThread(()->{
                            Button next = findViewById(R.id.next);
                            next.setVisibility(View.VISIBLE);
                            next.setEnabled(true);
                        });
                    }
                }
            }catch (Exception ignored) {
                switchToAppView();
            }
        }).start();
    }

    public void onNextClick(View view){
        Intent intent = new Intent(this, CreateUserActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void switchToAppView(){
        Intent intent = new Intent(this, AppActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    public void switchToSetupInProcess(){
        Intent intent = new Intent(this, SetupInProcess.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}