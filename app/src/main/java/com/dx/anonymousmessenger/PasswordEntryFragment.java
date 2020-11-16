package com.dx.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class PasswordEntryFragment extends Fragment {
    TextInputEditText txtPassword;
    View rootView;
    ProgressBar progressBar;
    Button btn_next;
    DxApplication app;
    LinearLayout errorBox;

    public PasswordEntryFragment() {
        // Required empty public constructor
    }

    public static PasswordEntryFragment newInstance(String param1, String param2) {
        return new PasswordEntryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
//        txtPassword = null;
//        rootView = null;
//        progressBar = null;
//        btn_next = null;
//        app = null;
    }

    @Override
    public void onDestroyView() {
        txtPassword = null;
        rootView = null;
        progressBar = null;
        btn_next = null;
        app = null;
        errorBox = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_password_entry, container, false);
        txtPassword = rootView.findViewById(R.id.password_entry);
        progressBar = rootView.findViewById(R.id.progress2);
        btn_next = rootView.findViewById(R.id.next);
        errorBox = rootView.findViewById(R.id.error_box);
        app = ((DxApplication) Objects.requireNonNull(getActivity()).getApplication());
        btn_next.setOnClickListener(v -> {
            btn_next.setEnabled(false);
            txtPassword.setEnabled(false);
            btn_next.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    SQLiteDatabase database = isPasswordCorrect(Objects.requireNonNull(txtPassword.getText()).toString());
                    if (getActivity() != null) {
                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> ((AppActivity) getActivity()).goToTorActivity());
                    }
                    String pass = txtPassword.getText().toString();
                    txtPassword = null;
                    btn_next = null;
                    progressBar = null;
                    rootView = null;
                    Cursor cr = database.rawQuery("SELECT * FROM account LIMIT 1;", null);
                    if (cr != null && cr.moveToFirst()) {
                        DxAccount account = new DxAccount(cr.getString(0));
                        cr.close();
                        account.setPassword(pass);
                        app.setAccount(account,false);
                        if (!app.isServerReady()) {
                            if (app.getTorThread() != null) {
                                app.getTorThread().interrupt();
                                app.setTorThread(null);
                            }
                            app.startTor();
                        }
                    } else {
//                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
//                            txtPassword.setEnabled(true);
//                            btn_next.setVisibility(View.VISIBLE);
//                            progressBar.setVisibility(View.GONE);
//                            txtPassword.setError("An unexpected error happened");
//                        });
                        Intent intent = new Intent(getActivity(), AppActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        getActivity().finish();
                    }
                    if (cr!=null && !cr.isClosed()) {
                        cr.close();
                    }
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    if (getActivity() != null) {
                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                            try{
                                txtPassword.setText("");
                                txtPassword.setEnabled(true);
                                txtPassword.setError(getString(R.string.wrong_password));
                                errorBox.setVisibility(View.VISIBLE);
                                Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.animation1);
                                errorBox.startAnimation(hyperspaceJumpAnimation);
                                btn_next.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }catch (Exception ignored) {}
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
//                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
//                            txtPassword.setEnabled(true);
//                            btn_next.setVisibility(View.VISIBLE);
//                            progressBar.setVisibility(View.GONE);
//                            txtPassword.setError("An unexpected error happened");
//                        });
                        Intent intent = new Intent(getActivity(), AppActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        try{
                            Objects.requireNonNull(getActivity()).finish();
                        }catch (Exception ignored) {}
                    }
                    e.printStackTrace();
                }
            }).start();
        });

        txtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>6){
                    try{
                        btn_next.setEnabled(true);
                    }catch (Exception ignored) {}
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return rootView;
    }

    public SQLiteDatabase isPasswordCorrect(String password) throws SQLiteException{
        Log.d("Account Checker","Checking Password");
        byte[] serialized = password.getBytes(StandardCharsets.UTF_8);
        password = new String(serialized, StandardCharsets.UTF_8);
        SQLiteDatabase database = app.getDb(password);
        Cursor cr2 = database.rawQuery("select count(*) from account;",null);
        int data;
        data = cr2.getCount();
        cr2.close();
        Log.d("Account Checker","Checking made it data:"+data);
        return database;
    }
}