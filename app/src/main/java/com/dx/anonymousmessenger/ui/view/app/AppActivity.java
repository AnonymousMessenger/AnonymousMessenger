package com.dx.anonymousmessenger.ui.view.app;

import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.setup.SetupInProcess;
import com.dx.anonymousmessenger.ui.view.setup.SetupSettingsFragment;

public class AppActivity extends DxActivity implements ComponentCallbacks2 {

    private boolean isAppFragmentShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
        if(alias!=null && alias.equals("com.dx.anonymousmessenger.ui.view.MainActivity")){
            setTitle(getString(R.string.app_name));
        }else if(alias!=null){
            setTitle(
                    alias.split("\\.")[alias.split("\\.").length-1]
            );
        }

//        new Thread(()->{
            if(((DxApplication) this.getApplication()).isExitingHoldup()){
                loadAppFragment();
                ((DxApplication) this.getApplication()).setExitingHoldup(false);
                return;
            }
            ((DxApplication) this.getApplication()).setExitingHoldup(false);
            //has logged in?
            if(((DxApplication) this.getApplication()).getAccount()!=null){
                //has logged in?
                if(((DxApplication) this.getApplication()).getAccount().getPassword()!=null){
                    if(getIntent().getBooleanExtra("force_app",false)){
                        //on user's orders it goes to contacts
                        loadAppFragment();
                        return;
                    }
                    //still starting up tor?
                    if(!((DxApplication) getApplication()).isServerReady()){
                        goToTorActivity();
                    }else{
                        //all set
                        loadAppFragment();
                    }
                }else{
                    loadPasswordEntryFragment();
                }
            }else{
                loadPasswordEntryFragment();
            }
//        }).start();
    }

    public void goToTorActivity() {
        Intent tor = new Intent(this, SetupInProcess.class);
        tor.putExtra("first_time",false);
        tor.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(tor);
        finish();
    }

    public void changeToSettingsFragment(){
        showNextFragment(SetupSettingsFragment.newInstance(false));
    }

    private void loadAppFragment(){
        if(isAppFragmentShown){
            return;
        }
        isAppFragmentShown = true;
        showNextFragment(new AppFragment());
    }

    private void loadPasswordEntryFragment(){
        showNextFragment(new PasswordEntryFragment());
    }

    public void showNextFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.slide_from_right,R.anim.slide_to_left)
            .replace(R.id.fragment_container, f)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
            finishAndRemoveTask();
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     * @param level the memory-related event that was raised.
     */
    public void onTrimMemory(int level) {

        // Determine which lifecycle or system event was raised.
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
                break;
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */

                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */

                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */

            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                finish();
                break;
        }
    }
}