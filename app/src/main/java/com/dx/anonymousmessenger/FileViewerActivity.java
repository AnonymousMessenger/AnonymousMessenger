package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Date;
import java.util.Objects;

public class FileViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
//            Objects.requireNonNull(getSupportActionBar()).hide();
        }catch (Exception ignored){}
        setContentView(R.layout.activity_file_viewer);

        TextView filenameTxt = findViewById(R.id.txt_filename);
        TextView fileSizeTxt = findViewById(R.id.txt_file_size);
        FloatingActionButton send = findViewById(R.id.btn_send_file);

        if(getIntent().getStringExtra("filename") == null || Objects.equals(getIntent().getStringExtra("filename"), "")){
            filenameTxt.setText(R.string.unknown_file_name);
        }else{
            filenameTxt.setText(getIntent().getStringExtra("filename"));
        }

        if(getIntent().getStringExtra("size") == null || Objects.equals(getIntent().getStringExtra("size"), "")){
            fileSizeTxt.setText(R.string.unknown_file_size);
            send.setVisibility(View.GONE);
        }else{
            try{
                String humanSize = Utils.humanReadableByteCount(Long.parseLong(Objects.requireNonNull(getIntent().getStringExtra("size"))));
                fileSizeTxt.setText(humanSize);
            }catch (Exception ignored) {}
        }

        if(getIntent().getStringExtra("path") == null || Objects.equals(getIntent().getStringExtra("path"), "")
            || getIntent().getStringExtra("address") == null){
            send.setVisibility(View.GONE);
            return;
        }

        send.setOnClickListener(v -> new Thread(()->{
            DxApplication app = (DxApplication) getApplication();
            final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                    "address"),
                    (DxApplication) getApplication());
            if(fullAddress == null){
                return;
            }
            String path;
            try {
                runOnUiThread(()->{
                    send.setVisibility(View.GONE);
                    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                });
                //todo change to stream
                byte[] byteArray = FileHelper.getUnencryptedFile(getIntent().getStringExtra("path"),app);
                path = FileHelper.saveFile(byteArray,app, Objects.requireNonNull(getIntent().getStringExtra("filename")));
                if(path==null){
                    return;
                }
                QuotedUserMessage qum = new QuotedUserMessage(app.getHostname(),
                        app.getAccount().getNickname(),new Date().getTime(),false,fullAddress,
                        getIntent().getStringExtra("filename"),path,"file");

                //send message and get received status
                new Thread(()->{
                    MessageSender.sendMediaMessage(qum,app,fullAddress);
                }).start();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(()->{
                    send.setVisibility(View.VISIBLE);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                });
            }
        }).start());

    }
}