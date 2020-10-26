package com.dx.anonymousmessenger;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Explode;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.Objects;

public class AddContactActivity extends AppCompatActivity {

    TextView tv;
    TextInputEditText contact;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_add_contact);

        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.add_contact);
            }
        }catch (Exception ignored){}

        tv = findViewById(R.id.txt_myaddress);
        tv.setText(((DxApplication)getApplication()).getHostname()==null?"waiting for tor":((DxApplication)getApplication()).getHostname());
        tv.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Snackbar.make(tv,R.string.copied_address,Snackbar.LENGTH_LONG).show();
        });
        contact = findViewById(R.id.txt_contact_address);
        Context context = this;
        contact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals(((DxApplication)getApplication()).getHostname())){
                    Snackbar.make(contact, R.string.cant_add_self,Snackbar.LENGTH_SHORT).show();
                    contact.setText("");
                    return;
                }
                if(s.toString().endsWith(".onion") && s.toString().length()>40){
                    new AlertDialog.Builder(context,R.style.AppAlertDialog)
                        .setTitle(R.string.add_contact)
                        .setMessage(getString(R.string.confirm_add_contact)+s.toString()+" ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(() ->
                        {
                            try{
                                if(DbHelper.contactExists(s.toString(),(DxApplication)getApplication())){
                                    runOnUiThread(()->{
                                        Snackbar.make(contact, R.string.contact_exists,Snackbar.LENGTH_SHORT).show();
                                        contact.setText("");
                                    });
                                    return;
                                }
                                boolean b = DbHelper.saveContact(s.toString().trim(), ((DxApplication) getApplication()));
                                if(!b){
                                    Log.e("FAILED TO SAVE CONTACT", "SAME " );
                                    runOnUiThread(()->{
                                        Snackbar.make(contact, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                                        contact.setText("");
                                    });
                                    return;
                                }
                                new Thread(()-> {
                                    if(TorClientSocks4.testAddress((DxApplication)getApplication(),s.toString().trim())){
                                        if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(s.toString().trim(),1))){
                                            MessageSender.sendKeyExchangeMessage((DxApplication)getApplication(),s.toString().trim());
                                        }
                                    }
                                }).start();
                                finish();
                            }catch (Exception e){
                                e.printStackTrace();
                                Log.e("FAILED TO SAVE CONTACT", "SAME " );
                                runOnUiThread(()->{
                                    Snackbar.make(contact, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                                    contact.setText("");
                                });
                            }
                        }
                        ).start())
                        .setNegativeButton(android.R.string.no, null).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(),R.string.crash_message, Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }.start();
            try
            {
                Thread.sleep(4000); // Let the Toast display before app will get shutdown
            }
            catch (InterruptedException ignored) {    }
            System.exit(2);
        });
    }
}