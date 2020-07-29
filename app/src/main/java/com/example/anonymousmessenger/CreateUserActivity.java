package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.example.anonymousmessenger.tor.ServerSocketViaTor;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.*;
import java.util.Objects;


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
                    .remove(Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment_container))).commit();
            SetupPasswordFragment secondFragment = new SetupPasswordFragment();
            secondFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                    .add(R.id.fragment_container, secondFragment).commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_create_user);

        if (findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            SetupUsernameFragment firstFragment = new SetupUsernameFragment();

            firstFragment.setArguments(getIntent().getExtras());

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
        //if (!getLifecycle().getCurrentState().isAtLeast(STARTED)) return;
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
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        torThread.start();
        ((DxApplication) this.getApplication()).setTorThread(torThread);
        StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        new Thread(() -> {
            try{
                Thread.sleep(9000);
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

    private void saveAccount(DxAccount account) {
        Log.d("Account Saver","Saving Account");
        SQLiteDatabase.loadLibs(this);
        File databaseFile = new File(this.getFilesDir(), "demo.db");
        //noinspection ResultOfMethodCallIgnored
        databaseFile.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        databaseFile.delete();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, password,
                null);
        database.execSQL(account.getSqlCreateTableString());
        database.execSQL(account.getSqlInsertString(),account.getSqlInsertValues());
    }

    private void startTorFirstTime() throws InterruptedException, IOException {
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