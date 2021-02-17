package com.dx.anonymousmessenger.ui.view.single_activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Explode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.whispersystems.libsignal.SignalProtocolAddress;

import java.util.ArrayList;
import java.util.Objects;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class AddContactActivity extends AppCompatActivity {

    TextView tv;
    TextInputEditText contact;
    final int QR_RESULT_CODE = 0;
    final int CAMERA_REQUEST_CODE = 1;

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
                if(s.toString().endsWith(".onion") && s.toString().length()>55){
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
                                if(TorClientSocks4.testAddress((DxApplication)getApplication(),s.toString().trim())){
                                    if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(s.toString().trim(),1))){
                                        MessageSender.sendKeyExchangeMessage((DxApplication)getApplication(),s.toString().trim());
                                    }
                                }
                                runOnUiThread(()->{
                                    finish();
                                });
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

        Button scan = findViewById(R.id.btn_scan_contact);
        scan.setOnClickListener((v)->{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this,SimpleScannerActivity.class);
                intent.putExtra("SCAN_MODE", "ADD_CONTACT");
                startActivityForResult(intent, QR_RESULT_CODE);
            }else{
                requestPermissions(
                    new String[] { Manifest.permission.CAMERA },
                    CAMERA_REQUEST_CODE);
            }
        });

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(((DxApplication)getApplication()).getHostname(), BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ImageView qr = ((ImageView) findViewById(R.id.qr_my_address));
            qr.setImageBitmap(bmp);
            qr.setOnClickListener((v) -> {
                try{
                    ImageView newQr = new ImageView(context);
                    newQr.setImageBitmap(bmp);
//                    newQr.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    ViewGroup parentViewGroup = (ViewGroup) newQr.getParent();
                    if (parentViewGroup != null) {
                        parentViewGroup.removeView(newQr);
                        parentViewGroup.removeAllViews();
                    }
                    View view = new View(context);
                    ArrayList<View>  viewArrayList = new ArrayList<>();
                    viewArrayList.add(newQr);
                    view.addChildrenForAccessibility(viewArrayList);
                    AlertDialog.Builder builder =
                        new AlertDialog.Builder(context).
                            setMessage("Message above the image").
                            setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).
                            setView(newQr);
                    builder.create().show();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
         // If it does contain data, decide if you can handle the data.
        if (clipboard.hasPrimaryClip()) {
            if ((Objects.requireNonNull(clipboard.getPrimaryClipDescription()).hasMimeType(MIMETYPE_TEXT_PLAIN))) {
                //since the clipboard contains plain text.
                ClipData.Item item = Objects.requireNonNull(clipboard.getPrimaryClip()).getItemAt(0);
                // Gets the clipboard as text.
                String s = item.getText().toString();

                if(s.equals(((DxApplication)getApplication()).getHostname())){
                    System.out.println("same as hostname");
                    return;
                }
                if(s.trim().length()<56 || !s.trim().endsWith(".onion")){
                    System.out.println("not valid");
                    return;
                }
                try{
                    if(DbHelper.contactExists(s,(DxApplication)getApplication())){
                        System.out.println("exists");
                        return;
                    }

                    new AlertDialog.Builder(context,R.style.AppAlertDialog)
                            .setTitle(R.string.add_contact)
                            .setMessage(getString(R.string.confirm_add_contact)+s+" ?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(() ->
                            {
                                boolean b = DbHelper.saveContact(s.trim(), ((DxApplication) getApplication()));
                                if(!b){
                                    Log.e("FAILED TO SAVE CONTACT", "SAME " );
                                    runOnUiThread(()->{
                                        Snackbar.make(contact, R.string.cant_add_contact,Snackbar.LENGTH_SHORT).show();
                                        contact.setText("");
                                    });
                                    return;
                                }
                                finish();
                                if(TorClientSocks4.testAddress((DxApplication)getApplication(),s.trim())){
                                    if(!((DxApplication)getApplication()).getEntity().getStore().containsSession(new SignalProtocolAddress(s.trim(),1))){
                                        MessageSender.sendKeyExchangeMessage((DxApplication)getApplication(),s.trim());
                                    }
                                }
                            }).start())
                            .setNegativeButton(android.R.string.no, null).show();
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e("FAILED TO SAVE CONTACT", "SAME " );
                }
            }
        }
    }

    public void getCameraPerms(){
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(getApplicationContext(),R.style.AppAlertDialog)
                .setTitle(R.string.cam_perm_ask_title)
                .setMessage(R.string.why_need_cam)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ask_for_cam_btn, (dialog, which) -> requestPermissions(
                        new String[] { Manifest.permission.CAMERA },
                        CAMERA_REQUEST_CODE))
                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                });
        } else {
            requestPermissions(
                new String[] { Manifest.permission.CAMERA },
                CAMERA_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            new AlertDialog.Builder(this,R.style.AppAlertDialog)
                .setTitle(R.string.denied_microphone)
                .setMessage(R.string.denied_microphone_help)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.ask_me_again, (dialog, which) -> getCameraPerms())
                .setNegativeButton(R.string.no_thanks, (dialog, which) -> {
                });
        }
    }
}