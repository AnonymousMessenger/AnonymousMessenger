package com.dx.anonymousmessenger;

import android.os.Bundle;
import android.transition.Explode;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.Objects;

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
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_app);

        if(((DxApplication) this.getApplication()).getAccount()!=null){
            if(((DxApplication) this.getApplication()).getAccount().getPassword()!=null){
                if(((DxApplication) getApplication()).isTorStartLocked()){
                    showNextFragment(new StartTorFragment());
                }else{
                    loadAppFragment();
                }
            }else{
                loadPasswordEntryFragment();
            }
        }else{
            loadPasswordEntryFragment();
        }
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