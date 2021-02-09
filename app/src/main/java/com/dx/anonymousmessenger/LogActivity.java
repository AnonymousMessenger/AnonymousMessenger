package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.db.DbHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity {
    private Thread logChecker = null;
    List<Object[]> list = new ArrayList<>();
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    LogRecycleViewAdapter adapter;
    SwitchMaterial switchNotice;
    boolean notice = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        setContentView(R.layout.activity_log);
        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.action_log);
            }
        }catch (Exception ignored){}

//        List<Object[]> list = DbHelper.getLogList((DxApplication)getApplication());
        switchNotice = findViewById(R.id.switch_notice);
        switchNotice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notice = isChecked;
            stopCheckingLogs();
            list = new ArrayList<>();
            adapter.list = this.list;
            adapter.notifyDataSetChanged();
            updateUi();
            checkLogs();
        });
        recyclerView = findViewById(R.id.recycler_log);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new LogRecycleViewAdapter(this, list);
        recyclerView.setAdapter(adapter);
        updateUi();
        checkLogs();
    }

    public void checkLogs(){
        if(logChecker!=null){
            return;
        }
        logChecker = new Thread(()->{
            while (list!=null){
                try{
                    Thread.sleep(2000);
                    updateUi();
                }catch (Exception ignored){break;}
            }
        });
        logChecker.start();
    }

    public void stopCheckingLogs(){
        if(logChecker==null){
            return;
        }
        if(logChecker.isAlive()){
            logChecker.interrupt();
            logChecker = null;
        }
    }

    public void updateUi(){
        new Thread(()->{
            int oldSize = this.list.size();
            List<Object[]> newList;
            if(notice){
                newList = DbHelper.getLogListWithNotice((DxApplication)getApplication());
            }else{
                newList = DbHelper.getLogList((DxApplication)getApplication());
            }
            int newSize = newList.size();
            this.list = newList;
            runOnUiThread(()->{
                if(newSize>0){
                    findViewById(R.id.switch_notice).setVisibility(View.GONE);
                }else{
                    findViewById(R.id.switch_notice).setVisibility(View.VISIBLE);
                }
                adapter.list = this.list;
//                adapter.notifyItemRangeInserted(oldSize,newSize);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void deleteLogs(){
        new Thread(()->{
            DbHelper.deleteAllLogs((DxApplication)getApplication());
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        stopCheckingLogs();
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.action_clear_log){
            deleteLogs();
        }else if(item.getItemId()==R.id.action_log_notice){
            if(item.isChecked()){
                notice = false;
                item.setChecked(false);
                switchNotice.setChecked(false);
            }else{
                notice = true;
                item.setChecked(true);
                switchNotice.setChecked(true);
            }
            stopCheckingLogs();
            list = new ArrayList<>();
            adapter.list = this.list;
            adapter.notifyDataSetChanged();
            updateUi();
            checkLogs();
        }
        return super.onOptionsItemSelected(item);
    }
}