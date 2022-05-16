package com.dx.anonymousmessenger.ui.custom;

import static android.view.MotionEvent.FLAG_WINDOW_IS_OBSCURED;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TapSafeToolbar  extends androidx.appcompat.widget.Toolbar{

    @Nullable
    private TapSafeFrameLayout.OnTapFilteredListener listener;

    public TapSafeToolbar(@NonNull Context context) {
        super(context);
        setFilterTouchesWhenObscured(this, true);
    }

    public TapSafeToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFilterTouchesWhenObscured(this, true);
    }

    public TapSafeToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFilterTouchesWhenObscured(this, true);
    }

    public void setOnTapFilteredListener(TapSafeFrameLayout.OnTapFilteredListener listener) {
        this.listener = listener;
    }

    public interface OnTapFilteredListener {
        boolean shouldAllowTap();
    }

    public static void setFilterTouchesWhenObscured(View v, boolean filter) {
        v.setFilterTouchesWhenObscured(filter);
        // Workaround for Android bug #13530806, see
        // https://android.googlesource.com/platform/frameworks/base/+/aba566589e0011c4b973c0d4f77be4e9ee176089%5E%21/core/java/android/view/View.java
        if (v.getFilterTouchesWhenObscured() != filter)
            v.setFilterTouchesWhenObscured(!filter);
    }

    @Override
    public boolean onFilterTouchEventForSecurity(MotionEvent e) {
        boolean obscured = (e.getFlags() & FLAG_WINDOW_IS_OBSCURED) != 0;
        if (obscured && listener != null) return listener.shouldAllowTap();
        else return !obscured;
    }
}
