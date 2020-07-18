package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setTheme(R.style.AppTheme);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        //check if account in storage, then change the next button text to login
        //assume new user for now

//        Button next = (Button)findViewById(R.id.next);
//        next.setText("Next");
//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onNextClick(view);
//            }
//        });
    }

    public void onNextClick(View view){
        Intent intent = new Intent(this, CreateUserActivity.class);
        startActivity(intent);
        finish();
    }
}