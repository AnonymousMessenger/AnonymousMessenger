package com.dx.anonymousmessenger.ui.view.log;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("BusyWait")
public class LogActivity extends DxActivity {
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
            setTitle(R.string.action_log);
            setBackEnabled(true);
        }catch (Exception ignored){}
        ((MaterialToolbar)findViewById(R.id.toolbar)).inflateMenu(R.menu.log_main_menu);
        ((MaterialToolbar)findViewById(R.id.toolbar)).setOnMenuItemClickListener((item)->{
            onOptionsItemSelected(item);
            return false;
        });

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
//            int oldSize = this.list.size();
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

    private void deleteLogs(MenuItem item){
        new AlertDialog.Builder(this,R.style.AppAlertDialog)
            .setTitle(R.string.delete_log_question)
            .setMessage(getString(R.string.delete_log_details))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(() ->{
                DbHelper.deleteAllLogs((DxApplication)getApplication());
                runOnUiThread(()->{
                    try{
                        Snackbar.make(item.getActionView(),R.string.deleted_logs,Snackbar.LENGTH_LONG).show();
                        stopCheckingLogs();
                        list = new ArrayList<>();
                        adapter.list = this.list;
                        adapter.notifyDataSetChanged();
                        updateUi();
                        checkLogs();
                    }catch (Exception ignored){}
                });
            }).start())
            .setNegativeButton(android.R.string.no, null).show();
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
            deleteLogs(item);
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
        }else if(item.getItemId()==R.id.action_scroll_up){
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