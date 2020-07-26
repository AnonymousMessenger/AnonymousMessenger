package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

public class AppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        DxAccount account = ((DxApplication) this.getApplication()).getAccount();
        if(account!=null){
            String password = ((DxApplication) this.getApplication()).getAccount().getPassword();
            if(password!=null){
                loadAppFragment();
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
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }
}