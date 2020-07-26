package com.example.anonymousmessenger;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetupUsernameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupUsernameFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String error;

    public SetupUsernameFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupUsernameFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupUsernameFragment newInstance(String param1, String param2) {
        SetupUsernameFragment fragment = new SetupUsernameFragment();
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
        final View rootView = inflater.inflate(R.layout.fragment_setup_username, container, false);
        final TextInputEditText txtNickname = (TextInputEditText) rootView.findViewById(R.id.txt_nickname);
        txtNickname.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        txtNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>1){
                    Button next = (Button) getView().findViewById(R.id.next);
                    next.setEnabled(true);
                    error = getString(R.string.nickname_okay);
                    TextView errorview = (TextView) getView().findViewById(R.id.txt_error);
                    errorview.setText(error);
                }else {
                    Button next = (Button) getView().findViewById(R.id.next);
                    next.setEnabled(false);
                    error = getString(R.string.nickname_error);
                    TextView errorview = (TextView) getView().findViewById(R.id.txt_error);
                    errorview.setText(error);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        final Button next = (Button) rootView.findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txtNickname.getText().length()>1){
                    next.setEnabled(false);
                    txtNickname.setEnabled(false);
                    ((CreateUserActivity)getActivity()).setNickname(txtNickname.getText().toString());
                    ((CreateUserActivity)getActivity()).changeToPasswordActivity();
//                    SetupPasswordFragment secondFragment = new SetupPasswordFragment();
//                    ((CreateUserActivity)getActivity()).showNextFragment(secondFragment);
                }
            }
        });
        // Inflate the layout for this fragment
        return rootView;
    }
}