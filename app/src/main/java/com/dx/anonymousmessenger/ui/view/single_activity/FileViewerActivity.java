package com.dx.anonymousmessenger.ui.view.single_activity;

import android.os.Bundle;
import android.transition.Explode;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.util.Date;
import java.util.Objects;

public class FileViewerActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
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

        send.setOnClickListener(v -> new Thread(()->{
            DxApplication app = (DxApplication) getApplication();
            String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                    "address"),
                    (DxApplication) getApplication());
            if(fullAddress == null){
                return;
            }
            String path = null;
            try {
                runOnUiThread(()->{
                    send.setVisibility(View.GONE);
                    findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                });
                InputStream is = FileHelper.getInputStreamFromUri(getIntent().getParcelableExtra("uri"),this);
                try{
                    path = FileHelper.saveFile(is,app, Objects.requireNonNull(getIntent().getStringExtra("filename")), Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("size"))));
                } catch (Exception e){
                    e.printStackTrace();
                }
                if(path==null){
                    return;
                }
                QuotedUserMessage qum = new QuotedUserMessage(app.getHostname(),
                        app.getAccount().getNickname(),new Date().getTime(),false,fullAddress,
                        getIntent().getStringExtra("filename"),path,"file");

                //send message and get received status
                new Thread(()->{
//                    MessageSender.sendMediaMessage(qum,app,fullAddress);
                    MessageSender.sendFile(qum,app,fullAddress);
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