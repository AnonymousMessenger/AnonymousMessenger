package com.dx.anonymousmessenger;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import static androidx.core.content.ContextCompat.getSystemService;

public class AddContactFragment extends Fragment {

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_contact, container, false);
        TextView tv = rootView.findViewById(R.id.txt_myaddress);
        tv.setText(((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).getAccount().getAddress());
        tv.setOnClickListener(v -> {
            ClipboardManager clipboard = getSystemService(Objects.requireNonNull(getContext()), ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            Toast.makeText(getContext(),"Copied address",Toast.LENGTH_LONG).show();
        });
        TextInputEditText contact = rootView.findViewById(R.id.txt_contact_address);
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
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            Toast.makeText(getContext(), "Contact Added", Toast.LENGTH_SHORT).show();
                            new Thread(()-> DbHelper.saveContact(s.toString().trim(),((DxApplication) Objects.requireNonNull(getActivity()).getApplication()))).start();
                            ((AppActivity) Objects.requireNonNull(getActivity())).showNextFragment(new AppFragment());
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