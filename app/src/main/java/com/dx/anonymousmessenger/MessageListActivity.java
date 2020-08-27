package com.dx.anonymousmessenger;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.Explode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.call.CallMaker;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageListAdapter;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.example.anonymousmessenger.CallActivity;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MessageListActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_CODE = 1;
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
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
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
            if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(getIntent().getStringExtra("address"),1))){
                Toast.makeText(this,"Session doesn't exist, not sending message",Toast.LENGTH_SHORT).show();
                new Thread(()-> MessageSender.sendKeyExchangeMessage(((DxApplication)getApplication()),getIntent().getStringExtra("address"))).start();
                return;
            }
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
            while (messageList!=null){
                try{
                    Thread.sleep(5000);
                    List<QuotedUserMessage> tmp = DbHelper.getMessageList(((DxApplication) getApplication()) ,
                            getIntent().getStringExtra("address"));

                    if(messageList.size() != tmp.size()){
                        updateUi(tmp);
                    }
                }catch (Exception ignored){messageList=null;break;}
            }
            messageList = null;
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
            String newName = messageList.get(messageList.size()-1).getSender();
            Objects.requireNonNull(getSupportActionBar()).setTitle(newName);
        });
    }

    public void updateUi(List<QuotedUserMessage> tmp){
        new Thread(()->{
            if(quoteTextTyping==null && mMyBroadcastReceiver==null){
                return;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.message_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_call:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                    if(((DxApplication)getApplication()).isInCall()){
                        Toast.makeText(this,"Already in a call",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    ((DxApplication)getApplication()).setCm(new CallMaker(getIntent().getStringExtra("address"),((DxApplication)getApplication())));
                    ((DxApplication)getApplication()).getCm().start();
                    Intent intent = new Intent(this, CallActivity.class);
                    intent.putExtra("address",getIntent().getStringExtra("address"));
                    intent.putExtra("nickname",getIntent().getStringExtra("nickname"));
                    startActivity(intent);

                }else{
                    requestPermissions(
                            new String[] { Manifest.permission.RECORD_AUDIO },
                            REQUEST_CODE);
                }
                break;
            case R.id.action_settings:
                //add the function to perform here
                DxApplication app =  ((DxApplication) getApplication());
                app.getEntity().getStore().deleteSession(new SignalProtocolAddress(getIntent().getStringExtra("address"),1));
                //((DxSignalKeyStore)app.getEntity().getStore()).removeIdentity(new SignalProtocolAddress(getIntent().getStringExtra("address"),1));
                Log.e("RESET SESSION","RESET SESSION with : "+getIntent().getStringExtra("address"));
                return true;
            case R.id.action_verify_identity:
                stopCheckingMessages();
                Intent intent = new Intent(this, VerifyIdentityActivity.class);
                intent.putExtra("address",getIntent().getStringExtra("address"));
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                }  else {
                    new AlertDialog.Builder(getApplicationContext())
                            .setTitle("Denied Microphone Permission")
                            .setMessage("this way you can't make or receive calls")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.ask_me_again, (DialogInterface.OnClickListener) (dialog, which) -> {
                                getMicrophonePerms();
                            })
                            .setNegativeButton(R.string.no_thanks, (DialogInterface.OnClickListener) (dialog, which) -> {

                            });
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    public void getMicrophonePerms(){
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.

        } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            new AlertDialog.Builder(getApplicationContext())
                .setTitle(R.string.mic_perm_ask_title)
                .setMessage(R.string.why_need_mic)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ask_for_mic_btn, (DialogInterface.OnClickListener) (dialog, which) -> {
                    requestPermissions(
                            new String[] { Manifest.permission.RECORD_AUDIO },
                            REQUEST_CODE);
                })
                .setNegativeButton(R.string.no_thanks, (DialogInterface.OnClickListener) (dialog, which) -> {

                });
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissions(
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    REQUEST_CODE);
        }
    }

}