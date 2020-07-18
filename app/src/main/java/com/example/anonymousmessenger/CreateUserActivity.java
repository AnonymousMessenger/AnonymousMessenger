package com.example.anonymousmessenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.textfield.TextInputLayout;

import static androidx.lifecycle.Lifecycle.State.STARTED;

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

            // Create a new Fragment to be placed in the activity layout
//            SetupUsernameFragment firstFragment = new SetupUsernameFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
//            firstFragment.setArguments(getIntent().getExtras());

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
}