package com.dx.anonymousmessenger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

public class PictureViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
//            Objects.requireNonNull(getSupportActionBar()).hide();
        }catch (Exception ignored){}
        setContentView(R.layout.activity_picture_viewer);

        try{
            if(getSupportActionBar()!=null){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                if(getIntent().getStringExtra("address")==null || Objects.equals(getIntent().getStringExtra("address"), "")){
                    getSupportActionBar().setTitle(R.string.picure_view);
                }else{
                    getSupportActionBar().setTitle(Objects.equals(getIntent().getStringExtra("nickname"), "") ?getIntent().getStringExtra("address"):getIntent().getStringExtra("nickname"));
                }
            }
        }catch (Exception ignored){}

        if(getIntent().getBooleanExtra("appData",false)){
            new Thread(()->{
                Bitmap image;
                try{
                    byte[] file = FileHelper.getFile(getIntent().getStringExtra("path"),(DxApplication) getApplication());
                    if(file==null){
                        return;
                    }
                    image = BitmapFactory.decodeByteArray(file, 0, file.length);
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                if(image==null){
                    return;
                }
                new Handler(Looper.getMainLooper()).post(()->{
                    ImageView img = findViewById(R.id.img_to_send);
                    img.setImageBitmap(image);
                    TextInputLayout textInputLayout = findViewById(R.id.txt_layout_caption);
                    textInputLayout.setVisibility(View.VISIBLE);
                    TextInputEditText msg = findViewById(R.id.txt_caption);
                    msg.setText(getIntent().getStringExtra("message"));
                    msg.setEnabled(false);
                });
            }).start();
            return;
        }

        new Thread(()->{
            Bitmap image;
            try{
                image = BitmapFactory.decodeFile(getIntent().getStringExtra("path"));
            }catch (Exception e){
                e.printStackTrace();
                return;
            }
            if(image==null){
                return;
            }
            new Handler(Looper.getMainLooper()).post(()->{
                ImageView img = findViewById(R.id.img_to_send);
                img.setImageBitmap(image);
            });
        }).start();

        TextInputLayout textInputLayout = findViewById(R.id.txt_layout_caption);
        textInputLayout.setVisibility(View.VISIBLE);
        TextInputEditText msg = findViewById(R.id.txt_caption);
        FloatingActionButton fabSendMedia = findViewById(R.id.btn_send_media);
        fabSendMedia.setVisibility(View.VISIBLE);
        fabSendMedia.setOnClickListener(v -> {
            msg.setEnabled(false);
            new Thread(()->{
                DxApplication app = (DxApplication) getApplication();
                //get time ready
                long time = new Date().getTime();
                //save bytes encrypted into a file and get path
                String filename = String.valueOf(time);
                String path = null;
                try {
                    Bitmap image = BitmapFactory.decodeFile(getIntent().getStringExtra("path"));
                    if(image==null){
                        return;
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    path = FileHelper.saveFile(byteArray,app,filename);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                if(path==null){
                    return;
                }
                String txtMsg = "";
                if(msg.getText()!=null){
                    txtMsg = msg.getText().toString();
                }
                //save metadata in encrypted database with reference to encrypted file
                QuotedUserMessage qum = new QuotedUserMessage("","",app.getHostname(),txtMsg,app.getAccount().getNickname(),time,false,getIntent().getStringExtra("address"),false,filename,path,"image");
                //send message and get received status
                MessageSender.sendMediaMessage(qum,app,getIntent().getStringExtra("address"));
            }).start();
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}