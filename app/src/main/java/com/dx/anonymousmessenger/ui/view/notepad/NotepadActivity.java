package com.dx.anonymousmessenger.ui.view.notepad;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;

import java.util.Date;
import java.util.List;

public class NotepadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        setContentView(R.layout.activity_notepad);
        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.action_notepad);
            }
        }catch (Exception ignored){}

        EditText txt = findViewById(R.id.edittext_chatbox);
        Button save = findViewById(R.id.button_save);
        save.setOnClickListener(v -> {
            new Thread(()-> {
                DbHelper.saveNote(txt.getText().toString(),new Date().getTime(),"","","",(DxApplication)getApplication());
                updateUi();
            }).start();
            txt.setText("");
//            audio.setVisibility(View.VISIBLE);
//            file.setVisibility(View.VISIBLE);
        });

        List<Object[]> list = DbHelper.getNotepadList((DxApplication)getApplication());
        RecyclerView recyclerView = findViewById(R.id.reyclerview_notes);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        NotepadRecycleViewAdapter adapter = new NotepadRecycleViewAdapter(this, list);
        recyclerView.setAdapter(adapter);
    }

    public void updateUi(){
        new Thread(()->{
            List<Object[]> list = DbHelper.getNotepadList((DxApplication)getApplication());
            runOnUiThread(()->{
                RecyclerView recyclerView = findViewById(R.id.reyclerview_notes);
                LinearLayoutManager layoutManager
                        = new LinearLayoutManager(this);
                recyclerView.setLayoutManager(layoutManager);
                NotepadRecycleViewAdapter adapter = new NotepadRecycleViewAdapter(this, list);
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(list.size()-1);
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}