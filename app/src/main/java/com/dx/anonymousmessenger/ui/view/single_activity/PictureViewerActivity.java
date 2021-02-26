package com.dx.anonymousmessenger.ui.view.single_activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.transition.Explode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class PictureViewerActivity extends DxActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_picture_viewer);

        try{
            setBackEnabled(true);
            if(getIntent().getStringExtra("address")==null || Objects.equals(getIntent().getStringExtra("address"), "")){
                setTitle(R.string.picure_view);
            }else{
                setTitle(Objects.equals(getIntent().getStringExtra("nickname"), "") ? (Objects.equals(getIntent().getStringExtra("address"),
                        ((DxApplication) getApplication()).getHostname()) ?
                        getString(R.string.you)
                        :getIntent().getStringExtra("address"))
                    : (Objects.equals(getIntent().getStringExtra("nickname"), ((DxApplication) getApplication()).getAccount().getNickname()) ?
                        getString(R.string.you)
                        :getIntent().getStringExtra("nickname")));
                if(getIntent().getLongExtra("time",0)!=0){
                    setSubtitle(Utils.formatDateTime(getIntent().getLongExtra("time",0)));
                }
            }
        }catch (Exception ignored){}

        if(getIntent().getBooleanExtra("appData",false)){
            ((MaterialToolbar)findViewById(R.id.toolbar)).inflateMenu(R.menu.picture_menu);
            ((MaterialToolbar)findViewById(R.id.toolbar)).setOnMenuItemClickListener((item)->{
                onOptionsItemSelected(item);
                return false;
            });
            new Thread(()->{
                Bitmap image;
                try{
                    byte[] file = FileHelper.getFile(getIntent().getStringExtra("path"),(DxApplication) getApplication());
                    if(file==null){
                        return;
                    }
                    Bitmap image2 = BitmapFactory.decodeByteArray(file, 0, file.length);
                    if(image2.getHeight()>1280 && image2.getWidth()>960){
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        image2 = BitmapFactory.decodeByteArray(file, 0, file.length,options);
                    }
                    image = image2;
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                if(image==null){
                    return;
                }
                new Handler(Looper.getMainLooper()).post(()->{
                    ImageView img = findViewById(R.id.img_to_send);
                    try{
                        img.setImageBitmap(image);
                    }catch (Exception ignored) {}
                    TextView textCaption = findViewById(R.id.txt_caption_view);
                    img.setOnClickListener(v -> {
                        if(textCaption.getText().toString().isEmpty()){
                            return;
                        }
                        if(textCaption.getVisibility()==View.VISIBLE){
                            textCaption.setVisibility(View.GONE);
                        }else{
                            textCaption.setVisibility(View.VISIBLE);
                        }
                    });
                    if(Objects.equals(getIntent().getStringExtra("message"), "")){
                        return;
                    }
                    textCaption.setVisibility(View.VISIBLE);
                    textCaption.setText(getIntent().getStringExtra("message"));
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
        InputMethodManager imm = requireNonNull(
                ContextCompat.getSystemService(this, InputMethodManager.class));
        imm.hideSoftInputFromWindow(msg.getWindowToken(), 0);
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
                final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                        "address"),
                        (DxApplication) getApplication());
                if(fullAddress == null){
                    return;
                }
                //save metadata in encrypted database with reference to encrypted file
                QuotedUserMessage qum = new QuotedUserMessage("","",app.getHostname(),txtMsg,
                        app.getAccount().getNickname(),time,false,fullAddress,false,filename,path,"image");
                //send message and get received status
                MessageSender.sendMediaMessage(qum,app,fullAddress);
            }).start();
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(getIntent().getBooleanExtra("appData",false)){
            getMenuInflater().inflate(R.menu.picture_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save_picture) {
            new AlertDialog.Builder(this,R.style.AppAlertDialog)
                .setTitle(R.string.save_to_storage)
                .setMessage(R.string.save_to_storage_explain)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(this::saveToStorage
                ).start())
                .setNegativeButton(android.R.string.no, null).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void saveToStorage(){
        try{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveToStorage();
                }
                return;
            }
            if(getIntent().getBooleanExtra("appData",false)){
                Bitmap image;
                byte[] file = FileHelper.getFile(getIntent().getStringExtra("path"), (DxApplication) getApplication());
                if(file==null){
                    return;
                }
                image = BitmapFactory.decodeByteArray(file, 0, file.length);
                if(image == null){
                    return;
                }
                MediaStore.Images.Media.insertImage(getContentResolver(), image, getString(R.string.anonymous_messenger)+Utils.formatDateTime(getIntent().getLongExtra("time",new Date().getTime())) , "");
                runOnUiThread(()-> Snackbar.make(findViewById(R.id.img_to_send),R.string.saved_to_storage,Snackbar.LENGTH_LONG).show());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}