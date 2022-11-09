package com.dx.anonymousmessenger.ui.view.single_activity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.transition.Explode;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.view.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class CrashActivity extends AppCompatActivity {

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_crash);

        String error = getIntent().getStringExtra("message");
        TextView details = findViewById(R.id.error_details);
        details.setText(error);
        details.setOnClickListener((v) -> {
            details.setTextIsSelectable(true);
            details.setSelected(true);
        });

        Button restartApp = findViewById(R.id.restart_button);
        restartApp.setOnClickListener((v) -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            if (intent.getComponent() != null) {
            //If the class name has been set, we force it to simulate a Launcher launch.
            //If we don't do this, if you restart from the error activity, then press home,
            //and then launch the activity from the launcher, the main activity appears twice on the backstack.
            //This will most likely not have any detrimental effect because if you set the Intent component,
            //if will always be launched regardless of the actions specified here.
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            }
            finishAndRemoveTask();
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
            startActivity(intent);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });

        FloatingActionButton copy = findViewById(R.id.fab_copy_error);
        copy.setOnClickListener((v) -> {
            ClipboardManager clipboard = ContextCompat.getSystemService(this, ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", error);
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Snackbar.make(v, R.string.copied, Snackbar.LENGTH_SHORT).setAnchorView(restartApp).show();
        });
    }

    @Override
    public void onBackPressed() {
        finishAndRemoveTask();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }

    @Override
    public void finish() {
        finishAndRemoveTask();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }
}