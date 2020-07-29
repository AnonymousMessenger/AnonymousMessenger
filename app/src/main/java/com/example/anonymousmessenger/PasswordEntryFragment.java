package com.example.anonymousmessenger;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.util.Objects;

public class PasswordEntryFragment extends Fragment {

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_password_entry, container, false);
        TextInputEditText txtPassword = rootView.findViewById(R.id.password_entry);
        Button btn_next = rootView.findViewById(R.id.next);
        btn_next.setOnClickListener(v -> new Thread(() -> {
            try {
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> btn_next.setEnabled(false));
                Log.d("Account Checker","Checking Password");
                SQLiteDatabase.loadLibs(Objects.requireNonNull(getContext()));
                File databaseFile = new File(getContext().getFilesDir(), "demo.db");
                SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile,
                        Objects.requireNonNull(txtPassword.getText()).toString(),
                        null);
                Cursor cr = database.rawQuery("select count(*) from account;",null);
                int data;
                data = cr.getCount();
                cr.close();
                Log.d("Account Checker","Checking made it data:"+data);
            } catch (SQLiteException e) {
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    txtPassword.setText("");
                    txtPassword.setError("Wrong password");
                    btn_next.setEnabled(true);
                });
            }catch (Exception e){
                e.printStackTrace();
            }

            try{
                SQLiteDatabase.loadLibs(Objects.requireNonNull(getContext()));
                File databaseFile = new File(getContext().getFilesDir(), "demo.db");
                SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile,
                        Objects.requireNonNull(txtPassword.getText()).toString(),
                        null);
                Cursor cr = database.rawQuery("select * from account;",null);
                cr.moveToFirst();
                DxAccount account = new DxAccount(cr.getString(0),cr.getBlob(1),
                        cr.getString(2),cr.getType(3));
                account.setPassword(txtPassword.getText().toString());
                ((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).setAccount(account);
                ((AppActivity)getActivity()).showNextFragment(new AppFragment());
            }catch(Exception e){
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                    btn_next.setEnabled(true);
                    txtPassword.setError("An unexpected error happened");
                }
                );
                e.printStackTrace();
            }
        }).start());

        txtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>6)
                    btn_next.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return rootView;
    }
}