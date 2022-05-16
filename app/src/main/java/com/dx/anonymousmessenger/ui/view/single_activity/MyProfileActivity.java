package com.dx.anonymousmessenger.ui.view.single_activity;

import static java.util.Objects.requireNonNull;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MyProfileActivity extends DxActivity {

    private static final int STORAGE_CODE = 0;
    private static final int REQUEST_PICK_FILE = 1;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(new
                    ActivityResultContracts.GetContent(),
            uri -> new Thread(() -> {
                try{
                    Intent intent = new Intent(MyProfileActivity.this, PictureViewerActivity.class);
                    intent.putExtra("address","addkih;,hklkjkjljklklkl;l'l;ress".substring(0,10));
                    intent.putExtra("nickname","nickname");
                    intent.putExtra("uri",uri.toString());
                    intent.putExtra("type", MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
                    startActivity(intent);
                }catch (Exception e){e.printStackTrace();}
            }).start());

    @Override
    public boolean onSupportNavigateUp() {
        finishAfterTransition();
        return true;
    }

    @Override
    public void onBackPressed() {
        finishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageView profileImage = findViewById(R.id.img_profile);
        profileImage.setTransitionName("profile_picture");
        new Thread(() -> {
            try{
                String path = ((DxApplication)getApplication()).getAccount().getProfileImagePath();
                if(path==null){
                    throw new Resources.NotFoundException("");
                }
                if(path.equals("")){
                    throw new Resources.NotFoundException("");
                }
                byte[] image = FileHelper.getFile(path,((DxApplication)getApplication()));
                if(image==null){
                    return;
                }
                new Handler(getMainLooper()).post(() -> {
                    profileImage.setImageBitmap(BitmapFactory.decodeByteArray(image,0,image.length));
                    profileImage.setOnClickListener((v) -> {
                        Intent intent = new Intent(this, PictureViewerActivity.class);
                        intent.putExtra("address",getIntent().getStringExtra("address"));
                        intent.putExtra("nickname","me");
                        intent.putExtra("time",0L);
                        intent.putExtra("appData",true);
                        intent.putExtra("path",path);
                        intent.putExtra("message","");
                        v.getContext().startActivity(intent);
                    });
                });
            }catch (Exception ignored){}
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
//        getWindow().setExitTransition(new Explode());
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
            ClipboardManager clipboard = ContextCompat.getSystemService(this, ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", address.getText().toString());
            requireNonNull(clipboard).setPrimaryClip(clip);
            Snackbar.make(address, R.string.copied_address, Snackbar.LENGTH_LONG).show();
        });
        Button changeNickname = findViewById(R.id.btn_change_nickname);
        changeNickname.setOnClickListener(v -> {
            TextInputLayout newNicknameContainer = findViewById(R.id.txt_container_new_nickname);
            TextInputEditText newNickname = findViewById(R.id.txt_new_nickname);
            if(newNicknameContainer.getVisibility()==View.VISIBLE){
                if("".equals(requireNonNull(newNickname.getText()).toString().trim())){
                    Snackbar.make(newNickname, R.string.empty_nickname_help,Snackbar.LENGTH_LONG).show();
                    return;
                }
                changeNickname(requireNonNull(newNickname.getText()).toString());
                changeNickname.setText(R.string.change_nickname);
                newNicknameContainer.setVisibility(View.GONE);
                newNickname.setText("");
            }else{
                newNicknameContainer.setVisibility(View.VISIBLE);
                changeNickname.setText(R.string.confirm);
            }
        });
        ImageView profileImage = findViewById(R.id.img_profile);
        profileImage.setTransitionName("profile_picture");
        new Thread(() -> {
            try{
                byte[] image = FileHelper.getFile(((DxApplication)getApplication()).getAccount().getProfileImagePath(),((DxApplication)getApplication()));
                if(image==null){
                    return;
                }
                new Handler(getMainLooper()).post(() -> profileImage.setImageBitmap(BitmapFactory.decodeByteArray(image,0,image.length)));
            }catch (Exception ignored){}
        }).start();
        FloatingActionButton editProfPic = findViewById(R.id.fab_edit_prof_pic);
        editProfPic.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.prof_pic_menu);
            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.remove_pic){
                    new AlertDialog.Builder(v.getContext(), R.style.AppAlertDialog)
                            .setTitle(R.string.remove_profile_image)
                            .setMessage(R.string.remove_profile_image_details)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                //delete
                                new Thread(()->{
                                    try{
                                        // delete pic from db/file
                                        String path = ((DxApplication)getApplication()).getAccount().getProfileImagePath();
                                        if(path!=null && !path.equals("")){
                                            FileHelper.deleteFile(path,((DxApplication)getApplication()));
                                        }
                                        ((DxApplication)getApplication()).getAccount().changeProfileImage("",((DxApplication)getApplication()));
                                        new Handler(Looper.getMainLooper()).post(()->{
                                            //remove pic from imageview
                                            Snackbar.make(v, R.string.profile_image_removed, Snackbar.LENGTH_SHORT).show();
                                        });
                                    }catch (Exception ignored){}
                                }).start();
                            })
                            .setNegativeButton(android.R.string.no, (dialog, whichButton) -> {
                            }).show();
                } else if(item.getItemId() == R.id.change_pic){
                    try{
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, STORAGE_CODE);
                            }
                            return false;
                        }
                        mGetContent.launch("image/*");
//                            Snackbar.make(v, R.string.profile_image_changed, Snackbar.LENGTH_SHORT).show();
                    }catch (Exception ignored) {}
                }
                return false;
            });
            popup.show();
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

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        if(requestCode == REQUEST_PICK_FILE){
//            try{
//                if(data == null || data.getData() == null){
//                    return;
//                }
//                Intent intent = new Intent(this, FileViewerActivity.class);
//                intent.putExtra("uri",data.getData());
//                intent.putExtra("filename", FileHelper.getFileName(data.getData(),this));
//                intent.putExtra("size",FileHelper.getFileSize(data.getData(),this));
//                intent.putExtra("address", requireNonNull(getIntent().getStringExtra("address")).substring(0,10));
//                startActivity(intent);
//            }catch (Exception e){e.printStackTrace();}
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //permission was just granted by the user
                mGetContent.launch("image/*");
            }
        }
    }
}