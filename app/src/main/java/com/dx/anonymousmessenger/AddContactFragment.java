package com.dx.anonymousmessenger;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.db.DbHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import static androidx.core.content.ContextCompat.getSystemService;

public class AddContactFragment extends Fragment {

    View rootView;
    TextView tv;
    TextInputEditText contact;

    public AddContactFragment() {
        // Required empty public constructor
    }

    public static AddContactFragment newInstance(String param1, String param2) {
        AddContactFragment fragment = new AddContactFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        rootView = null;
        tv = null;
        contact = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_contact, container, false);

        try{
            if(getActivity()!=null && getActivity() instanceof AppActivity && ((AppActivity) getActivity()).getSupportActionBar()!=null){
                ((AppActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                ((AppActivity) getActivity()).getSupportActionBar().setTitle("Add Contact");
            }
        }catch (Exception ignored){}

        tv = rootView.findViewById(R.id.txt_myaddress);
        tv.setText(((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).getAccount().getAddress());
        tv.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(Objects.requireNonNull(getContext()), ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Toast.makeText(getContext(),"Copied address",Toast.LENGTH_LONG).show();
        });
        contact = rootView.findViewById(R.id.txt_contact_address);
        contact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().endsWith(".onion") && s.toString().length()>15){
                    new AlertDialog.Builder(getContext())
                        .setTitle("Add Contact")
                        .setMessage("Do you really want to add "+s.toString()+" ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new Thread(() -> DbHelper.saveContact(s.toString().trim(), ((DxApplication) Objects.requireNonNull(getActivity()).getApplication()))).start();
                                ((AppActivity) Objects.requireNonNull(getActivity())).showNextFragment(new AppFragment());
                            }
                        })
                         .setNegativeButton(android.R.string.no, null).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return rootView;
    }
}