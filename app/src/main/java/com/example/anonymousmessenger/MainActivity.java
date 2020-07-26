package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setTheme(R.style.AppTheme);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);
//        InitializeSQLCipher();
        //check if account in storage, then change the next button text to login
        //assume new user for now

        File file = getDatabasePath("demo.db");
        if(file.exists()) //here's how to check
         {
             switchToAppView();
         }
         else{
            Button next = (Button)findViewById(R.id.next);
            next.setText("Create Account");
         }
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