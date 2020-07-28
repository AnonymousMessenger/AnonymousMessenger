package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.anonymousmessenger.db.DbHelper;
import com.example.anonymousmessenger.messages.Message;
import com.example.anonymousmessenger.messages.MessageListAdapter;
import com.example.anonymousmessenger.messages.MessageSender;
import com.example.anonymousmessenger.messages.UserMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageListActivity extends AppCompatActivity {

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    List<UserMessage> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);
        getSupportActionBar().setTitle(getIntent().getStringExtra("nickname"));
        getSupportActionBar().setSubtitle(getIntent().getStringExtra("address"));
        getSupportActionBar().setBackgroundDrawable(getDrawable(R.drawable.military_back));
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        messageList = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                getIntent().getStringExtra("address"));
//        messageList.add(new UserMessage("rh87234ryhff9h34989345.onion","whata da fak iza ap ma " +
//                "nigga","yoor negga",new Date().getTime()));
        mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        mMessageRecycler.setAdapter(mMessageAdapter);
        Button send = (Button) findViewById(R.id.button_chatbox_send);
        EditText txt = (EditText) findViewById(R.id.edittext_chatbox);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserMessage msg = new UserMessage(((DxApplication)getApplication()).getHostname(),
                txt.getText().toString(),((DxApplication)getApplication()).getAccount().getNickname(),
                        new Date().getTime(),false,getIntent().getStringExtra("address"));
                messageList.add(msg);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MessageSender.sendMessage(msg,((DxApplication)getApplication()),getIntent().getStringExtra("address"));
                    }
                }).start();
                txt.setText("");
                mMessageAdapter.notifyDataSetChanged();
            }
        });
    }
}