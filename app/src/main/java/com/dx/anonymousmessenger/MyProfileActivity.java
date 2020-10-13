package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class MyProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_my_profile);

        TextView nickname = findViewById(R.id.txt_nickname);
        nickname.setText(((DxApplication)getApplication()).getAccount().getNickname());
        TextView address = findViewById(R.id.txt_myaddress);
        address.setText(((DxApplication)getApplication()).getHostname());
        Button changeNickname = findViewById(R.id.btn_change_nickname);
        changeNickname.setOnClickListener(v -> {
            TextInputLayout newNicknameContainer = findViewById(R.id.txt_container_new_nickname);
            TextInputEditText newNickname = findViewById(R.id.txt_new_nickname);
            if(newNicknameContainer.getVisibility()==View.VISIBLE){
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