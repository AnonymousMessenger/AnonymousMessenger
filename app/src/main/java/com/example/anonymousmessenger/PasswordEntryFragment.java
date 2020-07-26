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

import com.example.anonymousmessenger.util.BytesUtil;
import com.google.android.material.textfield.TextInputEditText;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PasswordEntryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PasswordEntryFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PasswordEntryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PasswordEntryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PasswordEntryFragment newInstance(String param1, String param2) {
        PasswordEntryFragment fragment = new PasswordEntryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_password_entry, container, false);
        TextInputEditText txtPassword =
                (TextInputEditText) rootView.findViewById(R.id.password_entry);
        Button btn_next = (Button) rootView.findViewById(R.id.next);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //test decrypt db and tell user to retry or take to the other fragment
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn_next.setEnabled(false);
                                }
                            });
                            Log.d("Account Checker","Checking Password");
                            SQLiteDatabase.loadLibs(rootView.getContext());
                            File databaseFile = new File(rootView.getContext().getFilesDir(), "demo.db");
                            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile,
                                    txtPassword.getText().toString(),
                                    null);
                            Cursor cr = database.rawQuery("select count(*) from account;",null);
                            int data = 0;
                            data = cr.getCount();
                            cr.close();
                            Log.d("Account Checker","Checking made it data:"+data);
                        } catch (SQLiteException e) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    txtPassword.setText("");
                                    txtPassword.setError("Wrong password");
                                    btn_next.setEnabled(true);
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        try{
                            SQLiteDatabase.loadLibs(rootView.getContext());
                            File databaseFile = new File(rootView.getContext().getFilesDir(), "demo.db");
                            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile,
                                    txtPassword.getText().toString(),
                                    null);
                            Cursor cr = database.rawQuery("select * from account;",null);
                            cr.moveToFirst();
                            DxAccount account = new DxAccount(cr.getString(0),cr.getBlob(1),
                                    cr.getString(2),cr.getType(3));
                            account.setPassword(txtPassword.getText().toString());
                            ((DxApplication)getActivity().getApplication()).setAccount(account);
                            ((AppActivity)getActivity()).showNextFragment(new AppFragment());
                        }catch(Exception e){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btn_next.setEnabled(true);
                                    txtPassword.setError("An unexpected error happened");
                                }
                            }
                            );
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });

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