package com.dx.anonymousmessenger;

import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class AppActivity extends AppCompatActivity implements ComponentCallbacks2 {

    private boolean isAppFragmentShown;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_app);

        new Thread(()->{
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
                        //on users orders it goes to contacts
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
        }).start();

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

    }

    public void goToTorActivity() {
        Intent tor = new Intent(this, SetupInProcess.class);
        tor.putExtra("first_time",false);
        tor.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(tor);
        finish();
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
            .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
            .replace(R.id.fragment_container, f)
            .commit();
    }

    @Override
    public void onBackPressed() {
            finish();
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        onBackPressed();
//        return true;
//    }

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