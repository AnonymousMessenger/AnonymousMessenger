package com.dx.anonymousmessenger.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ScrollAwareFABBehavior extends FloatingActionButton.Behavior {
    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type);
    }

//    @Override
//    public boolean onStartNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child,
//                                       final View directTargetChild, final View target, final int nestedScrollAxes) {
//        // Ensure we react to vertical scrolling
//        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
//                || super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
//    }


    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            // User scrolled down and the FAB is currently visible -> hide the FAB
            child.show();
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            // User scrolled up and the FAB is currently not visible -> show the FAB
            child.hide();
        }
    }

//    @Override
//    public void onNestedScroll(final CoordinatorLayout coordinatorLayout, final FloatingActionButton child,
//                               final View target, final int dxConsumed, final int dyConsumed,
//                               final int dxUnconsumed, final int dyUnconsumed) {
//        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
//        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
//            // User scrolled down and the FAB is currently visible -> hide the FAB
//            child.show();
//        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
//            // User scrolled up and the FAB is currently not visible -> show the FAB
//            child.hide();
//        }
//    }
}

