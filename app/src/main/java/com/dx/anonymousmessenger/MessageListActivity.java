package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageListAdapter;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MessageListActivity extends AppCompatActivity {

    public TextView quoteTextTyping;
    public TextView quoteSenderTyping;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    List<QuotedUserMessage> messageList = new ArrayList<>();
    public Handler mainThread = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mMyBroadcastReceiver;
    private Thread messageChecker = null;
    private Button send = null;
    private EditText txt = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_message_list);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getIntent().getStringExtra("nickname"));
        getSupportActionBar().setSubtitle(getIntent().getStringExtra("address"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        quoteTextTyping = findViewById(R.id.quote_text_typing);
        quoteSenderTyping = findViewById(R.id.quote_sender_typing);
        quoteTextTyping.setVisibility(View.GONE);
        quoteTextTyping.setText("");
        quoteSenderTyping.setVisibility(View.GONE);
        quoteSenderTyping.setText("");
        send = findViewById(R.id.button_chatbox_send);
        txt = findViewById(R.id.edittext_chatbox);
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
        mMessageRecycler.setAdapter(mMessageAdapter);
        mMessageRecycler.scrollToPosition(messageList.size() - 1);
        send.setOnClickListener(v -> {
            TextView quoteSenderTyping = (TextView)findViewById(R.id.quote_sender_typing);
            TextView quoteTextTyping = ((TextView)findViewById(R.id.quote_text_typing));
            QuotedUserMessage msg = new QuotedUserMessage(quoteSenderTyping.getText().toString().equals("You")?((DxApplication)getApplication()).getAccount().getNickname():getIntent().getStringExtra("nickname"),quoteTextTyping.getText().toString(),((DxApplication)getApplication()).getHostname(),txt.getText().toString(),((DxApplication)getApplication()).getAccount().getNickname(),new Date().getTime(),false,getIntent().getStringExtra("address"),false);
            messageList.add(msg);
            new Thread(()-> MessageSender.sendMessage(msg,((DxApplication)getApplication()),getIntent().getStringExtra("address"))).start();
            txt.setText("");
            quoteSenderTyping.setText("");
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteTextTyping.setVisibility(View.GONE);
            mMessageAdapter.notifyItemInserted(messageList.size()-1);
            mMessageRecycler.scrollToPosition(messageList.size() - 1);
        });
        quoteTextTyping.setOnClickListener(v -> {
            quoteTextTyping.setVisibility(View.GONE);
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteSenderTyping.setText("");
        });
        quoteSenderTyping.setOnClickListener(v -> {
            quoteTextTyping.setVisibility(View.GONE);
            quoteTextTyping.setText("");
            quoteSenderTyping.setVisibility(View.GONE);
            quoteSenderTyping.setText("");
        });
        updateUi();

        //run db message checker to delete any old messages then tell us to update ui
        checkMessages();
    }

    public void checkMessages(){
        if(messageChecker!=null){
            return;
        }
        messageChecker = new Thread(()->{
            while (true){
                try{
                    Thread.sleep(5000);
                    List<QuotedUserMessage> tmp = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                            getIntent().getStringExtra("address"));
                    //TODO FIX THIS NOTEQUAL SHIT PLEASE BH
                    if(messageList != tmp){
                        Log.e("NOT EQUAL LIST N DB", "REALLY?" );
                        updateUi(tmp);
                    }
                }catch (Exception ignored){break;}
            }
        });
        messageChecker.start();
    }

    public void stopCheckingMessages(){
        if(messageChecker==null){
            return;
        }
        if(messageChecker.isAlive()){
            messageChecker.interrupt();
            messageChecker = null;
        }
    }

    public void updateUi(){
        messageList = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                getIntent().getStringExtra("address"));
        runOnUiThread(()->{
            mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
            mMessageRecycler.setAdapter(mMessageAdapter);
            mMessageAdapter.notifyDataSetChanged();
            mMessageRecycler.scrollToPosition(messageList.size() - 1);
        });
    }

    public void updateUi(List<QuotedUserMessage> tmp){
        new Thread(()->{
            if(quoteTextTyping==null && mMyBroadcastReceiver==null){

            }
            messageList = tmp;
            runOnUiThread(()->{
                try{
                    mMessageAdapter = new MessageListAdapter(this, messageList, (DxApplication) getApplication());
                    mMessageRecycler.setAdapter(mMessageAdapter);
                    mMessageAdapter.notifyDataSetChanged();
                    mMessageRecycler.scrollToPosition(messageList.size() - 1);
                }catch (Exception ignored){}
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMessages();
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
        updateUi();
    }

    @Override
    public boolean onSupportNavigateUp(){
        stopCheckingMessages();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
        quoteTextTyping = null;
        quoteSenderTyping = null;
        mMessageRecycler = null;
        mMessageAdapter = null;
        messageList = null;
        mainThread = null;
        mMyBroadcastReceiver = null;
        messageChecker = null;
        send = null;
        txt = null;
        finish();
        return true;
    }

    @Override
    protected void onPause() {
        stopCheckingMessages();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMyBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        onSupportNavigateUp();
        super.onDestroy();
    }
}