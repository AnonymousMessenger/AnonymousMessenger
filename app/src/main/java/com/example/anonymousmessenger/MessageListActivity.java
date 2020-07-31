package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.anonymousmessenger.db.DbHelper;
import com.example.anonymousmessenger.messages.MessageListAdapter;
import com.example.anonymousmessenger.messages.MessageSender;
import com.example.anonymousmessenger.messages.UserMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MessageListActivity extends AppCompatActivity {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    List<UserMessage> messageList = new ArrayList<>();
    public Handler mainThread = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mMyBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_message_list);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getIntent().getStringExtra("nickname"));
        getSupportActionBar().setSubtitle(getIntent().getStringExtra("address"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ImageButton send = findViewById(R.id.button_chatbox_send);
        EditText txt = findViewById(R.id.edittext_chatbox);
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        mMessageRecycler.setAdapter(mMessageAdapter);
        mMessageRecycler.scrollToPosition(messageList.size() - 1);
        send.setOnClickListener(v -> {
            UserMessage msg = new UserMessage(((DxApplication)getApplication()).getHostname(),
            txt.getText().toString(),((DxApplication)getApplication()).getAccount().getNickname(),
                    new Date().getTime(),false,getIntent().getStringExtra("address"));
            messageList.add(msg);
            new Thread(()-> MessageSender.sendMessage(msg,((DxApplication)getApplication()),getIntent().getStringExtra("address"))).start();
            txt.setText("");
            mMessageAdapter.notifyItemInserted(messageList.size()-1);
            mMessageRecycler.scrollToPosition(messageList.size() - 1);
        });
        updateUi();
    }

    public void updateUi(){
        new Thread(()->{
            messageList = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                    getIntent().getStringExtra("address"));
            runOnUiThread(()->{
                mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
                mMessageRecycler.setAdapter(mMessageAdapter);
                mMessageAdapter.notifyDataSetChanged();
                mMessageRecycler.scrollToPosition(messageList.size() - 1);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                updateUi();
            }
        };
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(mMyBroadcastReceiver,new IntentFilter("your_action"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}