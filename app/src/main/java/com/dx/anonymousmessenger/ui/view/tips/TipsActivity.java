package com.dx.anonymousmessenger.ui.view.tips;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.R;

public class TipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        setContentView(R.layout.activity_tips);
        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.action_tips);
            }
        }catch (Exception ignored){}

        String[] strings = getResources().getStringArray(R.array.security_tips);
        RecyclerView recyclerView = findViewById(R.id.reyclerview_tips);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        TipsRecycleViewAdapter adapter = new TipsRecycleViewAdapter(this, strings);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}