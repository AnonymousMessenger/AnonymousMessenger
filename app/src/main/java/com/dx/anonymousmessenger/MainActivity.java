package com.dx.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
//            Objects.requireNonNull(getSupportActionBar()).hide();
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
                    if(((DxApplication)getApplication()).isServiceRunningInForeground(this, MyService.class)){
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

        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(),R.string.crash_message, Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }.start();
            try
            {
                Thread.sleep(4000); // Let the Toast display before app will get shutdown
            }
            catch (InterruptedException ignored) {    }
            System.exit(2);
        });
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