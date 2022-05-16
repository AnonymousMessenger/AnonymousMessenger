package com.dx.anonymousmessenger.ui.view.app;

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

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.account.DxAccount;
import com.google.android.material.appbar.MaterialToolbar;
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

    public static PasswordEntryFragment newInstance() {
        return new PasswordEntryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        txtPassword = null;
//        rootView = null;
//        progressBar = null;
//        btn_next = null;
//        app = null;
//    }

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
        app = ((DxApplication) requireActivity().getApplication());
        //clean app bar
        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).getMenu().clear();
        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).setNavigationIcon(R.drawable.ic_stat_name);
        //try the easy password here and login if it works
        login(true);
        btn_next.setOnClickListener(v -> login(false));

        txtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //see if we can detect screen obstructed here
            }

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

        //todo warn user against using a non free keyboard with internet access.
//        InputMethodManager im = (InputMethodManager)app.getSystemService(Context.INPUT_METHOD_SERVICE);
//        String list = im.getEnabledInputMethodList().toString();
//        Log.d("ANONYMOUSMESSENGER","!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        Log.d("ANONYMOUSMESSENGER",list);

//        for (int i=0;i<list.length();i++){
//            Log.d("ANONYMOUSMESSENGER",list[i]);
//        }

        return rootView;
    }

    public void login(boolean easy){
        if(!easy){
            btn_next.setEnabled(false);
            txtPassword.setEnabled(false);
            btn_next.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
        new Thread(() -> {
            try {
                SQLiteDatabase database = isPasswordCorrect(easy?"easy_password".getBytes(StandardCharsets.UTF_8):Objects.requireNonNull(txtPassword.getText()).toString().getBytes(StandardCharsets.UTF_8));
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> ((AppActivity) getActivity()).goToTorActivity());
                }
                String pass = easy?"easy_password":txtPassword.getText().toString();
                if (!easy) {
                    txtPassword = null;
                }
                btn_next = null;
                progressBar = null;
                rootView = null;
                Cursor cr = database.rawQuery("SELECT * FROM account LIMIT 1;", null);
                if (cr != null && cr.moveToFirst()) {
                    DxAccount account = new DxAccount(cr.getString(0));
                    try {
                        database.execSQL("ALTER TABLE account ADD COLUMN profile_image_path TEXT default ''");
                    }catch (Exception ignored){
                        try{
                            account.setProfileImagePath(cr.getString(2));
                        }catch (Exception ignored2){}
                    }

                    cr.close();
                    account.setPassword(easy?"easy_password".getBytes(StandardCharsets.UTF_8):pass.getBytes(StandardCharsets.UTF_8));
                    app.setAccount(account,false);
                    if (!app.isServerReady()) {
//                            if (app.getTorThread() != null) {
//                                app.getTorThread().interrupt();
//                                app.setTorThread(null);
//                            }
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
                    requireActivity().runOnUiThread(() -> {
                        if(!easy){
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
                        }
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
                        requireActivity().finish();
                    }catch (Exception ignored) {}
                }
                e.printStackTrace();
            }
        }).start();
    }

    public SQLiteDatabase isPasswordCorrect(byte[] password) throws SQLiteException{
        Log.d("Account Checker","Checking Password");
        SQLiteDatabase database = app.getDb(password);
        Cursor cr2 = database.rawQuery("select count(*) from account;",null);
        int data;
        data = cr2.getCount();
        cr2.close();
        Log.d("Account Checker","Checking made it data:"+data);
        return database;
    }
}