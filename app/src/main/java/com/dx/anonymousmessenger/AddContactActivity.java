package com.dx.anonymousmessenger;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputEditText;

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
                getSupportActionBar().setTitle("Add Contact");
            }
        }catch (Exception ignored){}

        tv = findViewById(R.id.txt_myaddress);
        tv.setText(((DxApplication)getApplication()).getHostname());
        tv.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(),"Copied address",Toast.LENGTH_LONG).show();
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
                    Toast.makeText(context, "You can't add yourself!",Toast.LENGTH_SHORT).show();
                    contact.setText("");
                    return;
                }
                if(s.toString().endsWith(".onion") && s.toString().length()>15){
                    new AlertDialog.Builder(context)
                        .setTitle("Add Contact")
                        .setMessage("Do you really want to add "+s.toString()+" ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            new Thread(() ->
                            {
                                try{
                                    if(DbHelper.contactExists(s.toString(),(DxApplication)getApplication())){
                                        runOnUiThread(()->{
                                            Toast.makeText(context, "Contact already exists!",Toast.LENGTH_SHORT).show();
                                            contact.setText("");
                                        });
                                        return;
                                    }
                                    boolean b = DbHelper.saveContact(s.toString().trim(), ((DxApplication) getApplication()));
                                    if(!b){
                                        Log.e("FAILED TO SAVE CONTACT", "SAME " );
                                        runOnUiThread(()->{
                                            Toast.makeText(context, "can't add contact!",Toast.LENGTH_SHORT).show();
                                            contact.setText("");
                                        });
                                        return;
                                    }
                                    finish();
                                }catch (Exception e){
                                    e.printStackTrace();
                                    Log.e("FAILED TO SAVE CONTACT", "SAME " );
                                    runOnUiThread(()->{
                                        Toast.makeText(context, "can't add contact!",Toast.LENGTH_SHORT).show();
                                        contact.setText("");
                                    });
                                }
                            }
                            ).start();
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}