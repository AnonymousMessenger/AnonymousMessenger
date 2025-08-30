package com.dx.anonymousmessenger.ui.view.setup;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;
import android.content.res.ColorStateList; // For Java

public class SetupUsernameFragment extends Fragment {
    private String error;

    public SetupUsernameFragment() {
        // Required empty public constructor
    }

    public static SetupUsernameFragment newInstance() {
        SetupUsernameFragment fragment = new SetupUsernameFragment();
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
        final View rootView = inflater.inflate(R.layout.fragment_setup_username, container, false);
        final TextInputEditText txtNickname = rootView.findViewById(R.id.txt_caption);
        final FloatingActionButton help = rootView.findViewById(R.id.fab_nickname_help);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            txtNickname.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        }
        txtNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>1&&s.length()<35){
                    Button next = requireView().findViewById(R.id.next);
                    next.setEnabled(true);
                    error = getString(R.string.nickname_okay);
                    TextView errorView = requireView().findViewById(R.id.txt_error);
                    errorView.setText(error);
                }else {
                    Button next = requireView().findViewById(R.id.next);
                    next.setEnabled(false);
                    error = getString(R.string.nickname_error);
                    TextView errorView = requireView().findViewById(R.id.txt_error);
                    errorView.setText(error);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        help.setOnClickListener(v -> Utils.showHelpAlert(requireContext(),getString(R.string.nickname_explain), getString(R.string.nickname_explain_title)));

        final Button next = rootView.findViewById(R.id.next);
        next.setOnClickListener(v -> {
            if(Objects.requireNonNull(txtNickname.getText()).length()>1){
                next.setEnabled(false);
                txtNickname.setEnabled(false);
                new Thread(()->{
                    ((CreateUserActivity) requireActivity()).setNickname(txtNickname.getText().toString());
                    ((CreateUserActivity) requireActivity()).changeToSettingsFragment();
                }).start();
            }
        });

        // Add text watcher to update button state
        TextView txtCaption = rootView.findViewById(R.id.txt_caption);
        txtCaption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Button nextButton = getView().findViewById(R.id.next);
                boolean hasText = s.toString().trim().length() > 0;
                nextButton.setEnabled(hasText);
                nextButton.setBackgroundTintList(ColorStateList.valueOf(
                        hasText ? getResources().getColor(R.color.green_tor) :
                                getResources().getColor(R.color.dx_night_700)
                ));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Inflate the layout for this fragment
        return rootView;
    }

    // Add this method to handle example clicks
    public void onExampleClick(View view) {
        TextView example = (TextView) view;
        TextInputEditText editText = getView().findViewById(R.id.txt_caption);
        editText.setText(example.getText().toString().replace("• ", ""));
        editText.requestFocus();
    }

}