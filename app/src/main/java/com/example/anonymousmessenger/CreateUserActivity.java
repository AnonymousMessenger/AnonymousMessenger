package com.example.anonymousmessenger;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.example.anonymousmessenger.tor.ServerSocketViaTor;
import com.example.anonymousmessenger.util.BytesUtil;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.*;


public class CreateUserActivity extends AppCompatActivity {
    private String nickname;
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeToPasswordActivity(){
        if (findViewById(R.id.fragment_container) != null) {
            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                    .remove(getSupportFragmentManager().findFragmentById(R.id.fragment_container)).commit();
            SetupPasswordFragment secondFragment = new SetupPasswordFragment();
            secondFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                    .replace(R.id.fragment_container, secondFragment).commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_create_user);

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            SetupUsernameFragment firstFragment = new SetupUsernameFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                    .add(R.id.fragment_container, firstFragment).commit();
        }
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void showNextFragment(Fragment f) {
//        if (!getLifecycle().getCurrentState().isAtLeast(STARTED)) return;
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    public static void setError(TextInputLayout til, String error,
                                boolean set) {
		if (set) {
			if (til.getError() == null) til.setError(error);
		} else {
			til.setError(null);
		}
	}

    protected void createAccount(){
        if(nickname==null || password==null){
            throw new IllegalStateException();
        }

        ((DxApplication) this.getApplication()).sendNotification("Almost Ready!","Starting tor and warming up to get all we need to connect!");

        DxAccount account;
        if(((DxApplication) this.getApplication()).getAccount()==null){
            account = new DxAccount();
        }else{
            account = ((DxApplication) this.getApplication()).getAccount();
        }
        account.setNickname(nickname);
        account.setPassword(password);
        account.setIdentity_key("sdgfsdfsda".getBytes());
        ((DxApplication) this.getApplication()).setAccount(account);
        ((DxApplication) this.getApplication()).enableStrictMode();
        Thread torThread = new Thread(() -> {
            try {
                startTorFirstTime();
            } catch (InterruptedException | ClassNotFoundException | CloneNotSupportedException | IOException e) {
                e.printStackTrace();
            }
        });
        torThread.start();
        ((DxApplication) this.getApplication()).setTorThread(torThread);
        StrictMode.ThreadPolicy tp = StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        new Thread(() -> {
            try{
                Thread.sleep(6000);
                while(account.getNickname()==null | account.getAddress()==null | account.getPort()==0 | account.getIdentity_key()==null){
                    Log.e("STORE ACCOUNT","not yet ready to do so");
                    Thread.sleep(2000);
                }
                saveAccount(account);
                ((DxApplication) this.getApplication()).sendNotification("Ready to chat securely!",
                        "You got all you need to chat securely with your friends!");
                switchToAppView();
            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    private void saveAccount(DxAccount account) throws IOException {
        Log.d("Account Saver","Saving Accout");
        SQLiteDatabase.loadLibs(this);
        File databaseFile = new File(this.getFilesDir(), "demo.db");
        databaseFile.mkdirs();
        databaseFile.delete();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, password,
                null);
        database.execSQL(account.getSqlCreateTableString());
        database.execSQL(account.getSqlInsertString(),account.getSqlInsertValues());
    }

    private void startTorFirstTime() throws InterruptedException, ClassNotFoundException, CloneNotSupportedException, IOException {
        ((DxApplication) this.getApplication()).setTorSocket(new ServerSocketViaTor(getApplicationContext()));
        ((DxApplication) this.getApplication()).enableStrictMode();
        ServerSocketViaTor ssvt = ((DxApplication) this.getApplication()).getTorSocket();
        ssvt.init(((DxApplication) this.getApplication()));
    }

    public void switchToAppView(){
        Intent intent = new Intent(this, AppActivity.class);
        startActivity(intent);
        finish();
    }
}