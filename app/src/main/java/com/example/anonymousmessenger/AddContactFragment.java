package com.example.anonymousmessenger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import static androidx.core.content.ContextCompat.getSystemService;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddContactFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
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
        tv.setText(((DxApplication)getActivity().getApplication()).getAccount().getAddress());
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = getSystemService(getContext(), ClipboardManager.class);
                ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(),"Copied address",Toast.LENGTH_LONG).show();
            }
        });
        return rootView;
    }
}