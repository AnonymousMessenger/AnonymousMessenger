package com.dx.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.tor.ServerSocketViaTor;
import com.google.android.material.textfield.TextInputLayout;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


public class CreateUserActivity extends AppCompatActivity {
    private String nickname;
    private String password;
    private boolean noBack = false;

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
        showNextFragment(new SetupPasswordFragment());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_create_user);
        showNextFragment(new SetupUsernameFragment());
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
            switchToMainView();
            finish();
        }
        else {
            if(!noBack){
                super.onBackPressed();
            }
        }
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
        new Thread(() -> {
            try{
                if(nickname==null || password==null){
                    throw new IllegalStateException();
                }

                ((DxApplication) this.getApplication()).sendNotification("Almost Ready!","Starting tor and warming up to get all we need to connect!",false);

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

                Thread.sleep(9000);
                while(account.getNickname()==null | account.getAddress()==null | account.getPort()==0 | account.getIdentity_key()==null){
                    //Log.e("STORE ACCOUNT","not yet ready to do so");
                    Thread.sleep(2000);
                }
                saveAccount(account);
                ((DxApplication) this.getApplication()).sendNotification("Ready to chat securely!",
                        "You got all you need to chat securely with your friends!",false);
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
        ((DxApplication)getApplication()).setDb(database);
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

    public void switchToMainView(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}