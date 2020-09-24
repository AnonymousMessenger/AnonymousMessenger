package com.dx.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class AppActivity extends AppCompatActivity {
    private String fragmentName = "";

    @Override
    protected void onDestroy() {
        fragmentName = null;
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_app);

        new Thread(()->{
            //has logged in?
            if(((DxApplication) this.getApplication()).getAccount()!=null){
                //has logged in?
                if(((DxApplication) this.getApplication()).getAccount().getPassword()!=null){
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

    }

    public void goToTorActivity() {
        Intent tor = new Intent(this, SetupInProcess.class);
        tor.putExtra("first_time",false);
        startActivity(tor);
        finish();
    }

    private void loadAppFragment(){
        showNextFragment(new AppFragment());
    }

    private void loadPasswordEntryFragment(){
        showNextFragment(new PasswordEntryFragment());
    }

    public void showNextFragment(Fragment f) {
        fragmentName = f.getClass().toString();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (!fragmentName.contains("StartTor")) {
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}