package com.dx.anonymousmessenger.ui.view;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static java.util.Objects.requireNonNull;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.custom.ScreenFilterDialogFragment;
import com.dx.anonymousmessenger.ui.custom.TapSafeFrameLayout;
import com.dx.anonymousmessenger.ui.custom.TapSafeToolbar;
import com.dx.anonymousmessenger.ui.view.single_activity.CrashActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.PictureViewerActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class DxActivity extends AppCompatActivity implements TapSafeFrameLayout.OnTapFilteredListener{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        if(this.getClass() != PictureViewerActivity.class){
            SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(this);
            String theme = prefs2.getString("app-theme","dark");
            if (theme != null) {
                if(theme.equals("dark")){
                    setTheme(R.style.AppTheme);
                }else if(theme.equals("light")){
                    setTheme(R.style.LightTheme);
                }
            }
        }


        Thread.setDefaultUncaughtExceptionHandler((paramThread, paramThrowable) -> {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
//                    Toast.makeText(getApplicationContext(),R.string.crash_message, Toast.LENGTH_LONG).show();
                    final Intent intent = new Intent(getApplication(), CrashActivity.class);
                    intent.putExtra("stack",paramThrowable.getStackTrace());
                    intent.putExtra("message",paramThrowable.getMessage());
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    intent.putExtra("cause",paramThrowable.getCause());
                    getApplication().startActivity(intent);
//                    StringWriter sw = new StringWriter();
//                    PrintWriter pw = new PrintWriter(sw);
//                    throwable.printStackTrace(pw);
//                    String stackTraceString = sw.toString();
                    Looper.loop();
                }
            }.start();
            try
            {
                Thread.sleep(7500); // Let the Toast display before app will get shutdown
            }
            catch (InterruptedException ignored) {    }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
//            System.exit(2);
        });
    }

    /*
     * Wraps the given view in a wrapper that notifies this activity when an
     * obscured touch has been filtered, and returns the wrapper.
     */
    private TapSafeFrameLayout makeTapSafeWrapper(View v) {
        TapSafeFrameLayout wrapper = new TapSafeFrameLayout(this);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        wrapper.setOnTapFilteredListener(this);
        wrapper.addView(v);
        return wrapper;
    }

    private TapSafeToolbar makeTapSafeWrapper(Toolbar v) {
        TapSafeToolbar wrapper = new TapSafeToolbar(this);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        wrapper.setOnTapFilteredListener(this);
        wrapper.addView(v);
        return wrapper;
    }

    @Override
    public void setContentView(@LayoutRes int layoutRes) {
        setContentView(getLayoutInflater().inflate(layoutRes, null));
    }

    @Override
    public void setContentView(View v) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        TapSafeFrameLayout view = makeTapSafeWrapper(v);

        super.setContentView(view);
        useCustomActionBarToolbar();
//        Rect visibleRect = new Rect();
//        v.getGlobalVisibleRect(visibleRect);
//        boolean b = visibleRect.height() == v.getHeight() && visibleRect.width() == v.getWidth();
//        if(b){
//            Log.d("ANONYMOUSMESSENGER","!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//            Log.d("ANONYMOUSMESSENGER","SCREEEEEEEEEEEEEN OBSTRUCTEDDDDDDDD");
//        }
    }

    protected void useCustomActionBarToolbar() {
        if(getSupportActionBar()==null){
            return;
        }
        requireNonNull(getSupportActionBar()).hide();
        if (findViewById(R.id.toolbar) != null) {
            requireNonNull(getSupportActionBar()).hide();
//            findViewById(R.id.img_toolbar_back).setOnClickListener((v)->{
//                this.onSupportNavigateUp();
//            });
        }
    }

    protected void setTitle(String title){
        if (findViewById(R.id.toolbar) != null) {
            ((MaterialToolbar)findViewById(R.id.toolbar)).setTitle(title);
        }
//        if (findViewById(R.id.txt_toolbar_title) != null) {
//            ((TextView)findViewById(R.id.txt_toolbar_title)).setText(title);
//        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (findViewById(R.id.toolbar) != null) {
            ((MaterialToolbar)findViewById(R.id.toolbar)).setTitle(title);
        }
    }

    @Override
    public void setTitle(int titleId) {
        if (findViewById(R.id.toolbar) != null) {
            ((MaterialToolbar)findViewById(R.id.toolbar)).setTitle(titleId);
        }
    }

    protected void setSubtitle(String subtitle){
        if (findViewById(R.id.toolbar) != null) {
            ((MaterialToolbar)findViewById(R.id.toolbar)).setSubtitle(subtitle);
        }
    }

    protected void setSubtitle(int subtitle){
        if (findViewById(R.id.toolbar) != null) {
            ((MaterialToolbar)findViewById(R.id.toolbar)).setSubtitle(subtitle);
        }
    }

    public void setBackEnabled(boolean visible){
        if (findViewById(R.id.top_bar) != null && visible) {
            ((MaterialToolbar)findViewById(R.id.toolbar)).setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
            ((MaterialToolbar)findViewById(R.id.toolbar)).setNavigationOnClickListener((v)-> onSupportNavigateUp());
        }
    }

    @Override
    public boolean shouldAllowTap() {
        return showScreenFilterWarning();
    }

    private boolean showScreenFilterWarning() {
        // If the dialog is already visible, filter the tap
        ScreenFilterDialogFragment f = findDialogFragment();
        if (f != null && f.isVisible()) return false;
        // Show dialog unless onSaveInstanceState() has been called, see #1112
        FragmentManager fm = getSupportFragmentManager();
        if (!fm.isStateSaved()) {
            // Create dialog
            f = ScreenFilterDialogFragment.newInstance();
            // Hide soft keyboard when (re)showing dialog
            View focus = getCurrentFocus();
            if (focus != null) hideSoftKeyboard(focus);
            f.show(fm, ScreenFilterDialogFragment.TAG);
        }
        // Filter the tap
        return false;
    }

    @Nullable
    private ScreenFilterDialogFragment findDialogFragment() {
        Fragment f = getSupportFragmentManager().findFragmentByTag(
                ScreenFilterDialogFragment.TAG);
        return (ScreenFilterDialogFragment) f;
    }

    public static void hideSoftKeyboard(View view) {
        InputMethodManager imm = requireNonNull(
                ContextCompat.getSystemService(view.getContext(), InputMethodManager.class));
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if(isInMultiWindowMode){
            showScreenFilterWarning();
        }
        super.onMultiWindowModeChanged(isInMultiWindowMode);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if(isInPictureInPictureMode){
            showScreenFilterWarning();
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    public void superStartActivity(Intent intent){
        super.startActivity(intent);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }

    public void superFinish(){
        super.finish();
    }
}
