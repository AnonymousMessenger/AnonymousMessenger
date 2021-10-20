package com.dx.anonymousmessenger.ui.view.notepad;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotepadActivity extends DxActivity {

    List<Object[]> list = new ArrayList<>();
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        setContentView(R.layout.activity_notepad);
        try{
            setTitle(R.string.action_notepad);
            setBackEnabled(true);
        }catch (Exception ignored){}
        ((MaterialToolbar)findViewById(R.id.toolbar)).inflateMenu(R.menu.notepad_main_menu);
        ((MaterialToolbar)findViewById(R.id.toolbar)).setOnMenuItemClickListener((item)->{
            onOptionsItemSelected(item);
            return false;
        });

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

        list = DbHelper.getNotepadList((DxApplication)getApplication());
        recyclerView = findViewById(R.id.reyclerview_notes);
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        NotepadRecycleViewAdapter adapter = new NotepadRecycleViewAdapter(this, list);
        recyclerView.setAdapter(adapter);
    }

    public void updateUi(){
        new Thread(()->{
            list = DbHelper.getNotepadList((DxApplication)getApplication());
            runOnUiThread(()->{
                recyclerView = findViewById(R.id.reyclerview_notes);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.action_scroll_up){
            if(list!=null && list.size()>0){
                recyclerView.scrollToPosition(0);
            }
        }else if(item.getItemId()==R.id.action_scroll_down){
            if(list!=null && list.size()>0){
                recyclerView.scrollToPosition(list.size()-1);
            }
        }
        return super.onOptionsItemSelected(item);
    }
}