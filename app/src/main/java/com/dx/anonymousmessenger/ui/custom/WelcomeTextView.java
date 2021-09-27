package com.dx.anonymousmessenger.ui.custom;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;

import com.dx.anonymousmessenger.R;


public class WelcomeTextView extends androidx.appcompat.widget.AppCompatTextView {

    public WelcomeTextView(Context context, AttributeSet as) {
        super(context, as);

        setElevation(99);
        setLineSpacing(0, 1.1f);
        setTextSize(getResources().getDimensionPixelSize(R.dimen.textsize24));
//        setTextColor(ContextCompat.getColor(context, R.color.dx_night_940));
//        setBackgroundColor(ContextCompat.getColor(context, R.color.dx_dark_purple));
        setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
    }

//    public FrameLayout.LayoutParams layoutParamsForFrameLayout() {
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//        params.gravity = Gravity.CENTER;
//        return params;
//    }

    public void animateText() {
        String text = getResources().getString(R.string.welcome_to_anonymous_messenger);
        animationLoop(0, text);
    }

    private void animationLoop(int i, String text){
        int ANIM_DURATION_PER_LETTER = 100;
        new Handler().postDelayed(() -> {
            setText(text.substring(0,i));
            if(i<text.length()){
                animationLoop(i+1, text);
            }
        }, ANIM_DURATION_PER_LETTER);
    }
}