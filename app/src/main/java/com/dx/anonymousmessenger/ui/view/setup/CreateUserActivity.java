package com.dx.anonymousmessenger.ui.view.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.MainActivity;
import com.dx.anonymousmessenger.ui.view.app.AppActivity;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;


public class CreateUserActivity extends DxActivity {
    private String nickname;
    private List<String> bridgeList = new ArrayList<>();
    private boolean bridgesEnabled = false;
    private boolean isAcceptingUnknownContactsEnabled = false;
    private boolean isAcceptingCallsAllowed = true;
    private boolean isReceivingFilesAllowed = true;
    private String checkAddress = "duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion";
    private String fileSizeLimit = "3gb";

    public String getCheckAddress() {
        return checkAddress;
    }

    public void setCheckAddress(String checkAddress) {
        this.checkAddress = checkAddress;
    }

    public boolean areBridgesEnabled() {
        return bridgesEnabled;
    }

    public void setBridgesEnabled(boolean bridgesEnabled) {
        this.bridgesEnabled = bridgesEnabled;
    }

    public boolean isAcceptingUnknownContactsEnabled() {
        return isAcceptingUnknownContactsEnabled;
    }

    public void setAcceptingUnknownContactsEnabled(boolean acceptingUnknownContactsEnabled) {
        isAcceptingUnknownContactsEnabled = acceptingUnknownContactsEnabled;
    }

    public boolean isAcceptingCallsAllowed() {
        return isAcceptingCallsAllowed;
    }

    public void setAcceptingCallsAllowed(boolean acceptingCallsAllowed) {
        isAcceptingCallsAllowed = acceptingCallsAllowed;
    }

    public boolean isReceivingFilesAllowed() {
        return isReceivingFilesAllowed;
    }

    public void setReceivingFilesAllowed(boolean receivingFilesAllowed) {
        isReceivingFilesAllowed = receivingFilesAllowed;
    }

    public String getFileSizeLimit() {
        return fileSizeLimit;
    }

    public void setFileSizeLimit(String fileSizeLimit) {
        this.fileSizeLimit = fileSizeLimit;
    }


    public List<String> getBridgeList() {
        return bridgeList;
    }

    public void setBridgeList(List<String> bridgeList) {
        this.bridgeList = bridgeList;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeToSettingsFragment(){
        showNextFragment(SetupSettingsFragment.newInstance(true));
    }

    public void changeToPasswordFragment(){
        showNextFragment(new SetupPasswordFragment());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
//        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
//        getWindow().setExitTransition(new Explode());
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
//            if(!noBack){
                super.onBackPressed();
//            }
        }
    }

    private void showNextFragment(Fragment f) {
        //if (!getLifecycle().getCurrentState().isAtLeast(STARTED)) return;
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    private static void setError(TextInputLayout til, String error,
                                boolean set) {
		if (set) {
			if (til.getError() == null) til.setError(error);
		} else {
			til.setError(null);
		}
	}

    protected void createAccount(byte[] password){
        if(nickname==null || password==null){
            return;
        }
        try{
            //create account and start tor
            ((DxApplication)getApplication()).createAccount(password,nickname,bridgesEnabled,bridgeList,isAcceptingUnknownContactsEnabled,isAcceptingCallsAllowed,isReceivingFilesAllowed,checkAddress,fileSizeLimit);
            Intent intent = new Intent(this, SetupInProcess.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void switchToAppView(){
        Intent intent = new Intent(this, AppActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void switchToMainView(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}