package com.dx.anonymousmessenger.ui.custom;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.dx.anonymousmessenger.R;

public class ScreenFilterDialogFragment extends DialogFragment {

    public static final String TAG = ScreenFilterDialogFragment.class.getName();

//    ScreenFilterMonitor screenFilterMonitor;

    private DismissListener dismissListener = null;

    public static ScreenFilterDialogFragment newInstance() {
        return new ScreenFilterDialogFragment();
    }

    public void setDismissListener(DismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) throw new IllegalStateException();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                R.style.OverlayAlertDialog);
        builder.setTitle(R.string.screen_filter_title);

        LayoutInflater inflater = activity.getLayoutInflater();
        // See https://stackoverflow.com/a/24720976/6314875
        View dialogView = inflater.inflate(R.layout.dialog_screen_filter, null);
        builder.setView(dialogView);
        TextView message = dialogView.findViewById(R.id.screen_filter_message);
        if (SDK_INT <= 29) {
            message.setText(getString(R.string.screen_filter_body));
        } else {
            message.setText(R.string.screen_filter_body);
        }
        builder.setPositiveButton(R.string.try_now, (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) dismissListener.onDialogDismissed();
    }

    public interface DismissListener {
        void onDialogDismissed();
    }
}

