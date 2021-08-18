package com.dx.anonymousmessenger.ui.view.single_activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.Explode;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class MyProfileActivity extends DxActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_my_profile);

        try{
            setTitle(R.string.action_my_profile);
            setBackEnabled(true);
        }catch (Exception ignored){}

        TextView nickname = findViewById(R.id.txt_nickname);
        nickname.setText(((DxApplication)getApplication()).getAccount().getNickname());
        TextView address = findViewById(R.id.txt_myaddress);
        address.setText(((DxApplication)getApplication()).getHostname()==null?((DxApplication)getApplication()).getMyAddressOffline():((DxApplication)getApplication()).getHostname());
        address.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", address.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Snackbar.make(address, R.string.copied_address, Snackbar.LENGTH_LONG).show();
        });
        Button changeNickname = findViewById(R.id.btn_change_nickname);
        changeNickname.setOnClickListener(v -> {
            TextInputLayout newNicknameContainer = findViewById(R.id.txt_container_new_nickname);
            TextInputEditText newNickname = findViewById(R.id.txt_new_nickname);
            if(newNicknameContainer.getVisibility()==View.VISIBLE){
                if("".equals(Objects.requireNonNull(newNickname.getText()).toString().trim())){
                    Snackbar.make(newNickname, R.string.empty_nickname_help,Snackbar.LENGTH_LONG).show();
                    return;
                }
                changeNickname(Objects.requireNonNull(newNickname.getText()).toString());
                changeNickname.setText(R.string.change_nickname);
                newNicknameContainer.setVisibility(View.GONE);
                newNickname.setText("");
            }else{
                newNicknameContainer.setVisibility(View.VISIBLE);
                changeNickname.setText(R.string.confirm);
            }
        });
    }

    private void changeNickname(String newNickname) {
        new Thread(()->{
            try{
                ((DxApplication)getApplication()).getAccount().changeNickname(newNickname,(DxApplication)getApplication());
                new Handler(Looper.getMainLooper()).post(()->{
                    try{
                        TextView nickname = findViewById(R.id.txt_nickname);
                        nickname.setText(((DxApplication)getApplication()).getAccount().getNickname());
                    }catch (Exception ignored) {}
                });
            }catch (Exception ignored) {}
        }).start();
    }
}