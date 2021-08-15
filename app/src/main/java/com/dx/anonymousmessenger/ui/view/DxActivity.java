package com.dx.anonymousmessenger.ui.view;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

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
import com.google.android.material.appbar.MaterialToolbar;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static java.util.Objects.requireNonNull;

public class DxActivity extends AppCompatActivity implements TapSafeFrameLayout.OnTapFilteredListener{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

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
//            Log.d("GENERAL","!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//            Log.d("GENERAL","SCREEEEEEEEEEEEEN OBSTRUCTEDDDDDDDD");
//        }
    }

    protected void useCustomActionBarToolbar() {
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
            ((MaterialToolbar)findViewById(R.id.toolbar)).setNavigationOnClickListener((v)->{
                onSupportNavigateUp();
            });
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
}
