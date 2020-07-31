package com.example.anonymousmessenger;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.anonymousmessenger.crypto.PasswordStrengthEstimator;
import com.example.anonymousmessenger.login.StrengthMeter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashSet;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static com.example.anonymousmessenger.crypto.PasswordStrengthEstimator.QUITE_WEAK;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetupPasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupPasswordFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextInputLayout passwordEntryWrapper;
    private TextInputLayout passwordConfirmationWrapper;
    private TextInputEditText passwordEntry;
    private TextInputEditText passwordConfirmation;
    private StrengthMeter strengthMeter;
    private Button nextButton;
    private ProgressBar progressBar;

    public SetupPasswordFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SetupPasswordFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupPasswordFragment newInstance(String param1, String param2) {
        SetupPasswordFragment fragment = new SetupPasswordFragment();
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
        View v = inflater.inflate(R.layout.fragment_setup_password2, container,
                false);

        strengthMeter = v.findViewById(R.id.strength_meter);
        passwordEntryWrapper = v.findViewById(R.id.password_entry_wrapper);
        passwordEntry = v.findViewById(R.id.password_entry);
        passwordConfirmationWrapper =
                v.findViewById(R.id.password_confirm_wrapper);
        passwordConfirmation = v.findViewById(R.id.password_confirm);
        nextButton = v.findViewById(R.id.next);
        progressBar = v.findViewById(R.id.progress);

        passwordEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence authorName, int i, int i1, int i2) {
                String password1 = passwordEntry.getText().toString();
                String password2 = passwordConfirmation.getText().toString();
                boolean passwordsMatch = password1.equals(password2);

                strengthMeter
                        .setVisibility(password1.length() > 0 ? VISIBLE : INVISIBLE);
                float strength = estimatePasswordStrength(password1);
                strengthMeter.setStrength(strength);
                boolean strongEnough = strength > QUITE_WEAK;

                setError(passwordEntryWrapper, getString(R.string.password_too_weak),
                        password1.length() > 0 && !strongEnough);
                setError(passwordConfirmationWrapper,
                        getString(R.string.passwords_do_not_match),
                        password2.length() > 0 && !passwordsMatch);

                boolean enabled = passwordsMatch && strongEnough;
                nextButton.setEnabled(enabled);
//                passwordConfirmation.setOnEditorActionListener(enabled ? (TextView.OnEditorActionListener) this : null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        passwordConfirmation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password1 = passwordEntry.getText().toString();
                String password2 = passwordConfirmation.getText().toString();
                boolean passwordsMatch = password1.equals(password2);

                strengthMeter
                        .setVisibility(password1.length() > 0 ? VISIBLE : INVISIBLE);
                float strength = estimatePasswordStrength(password1);
                strengthMeter.setStrength(strength);
                boolean strongEnough = strength > QUITE_WEAK;

                setError(passwordEntryWrapper, getString(R.string.password_too_weak),
                        password1.length() > 0 && !strongEnough);
                setError(passwordConfirmationWrapper,
                        getString(R.string.passwords_do_not_match),
                        password2.length() > 0 && !passwordsMatch);

                boolean enabled = passwordsMatch && strongEnough;
                nextButton.setEnabled(enabled);
//                passwordConfirmation.setOnEditorActionListener(enabled ? (TextView.OnEditorActionListener) this : null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        nextButton.setOnClickListener(v1 -> {
            IBinder token = passwordEntry.getWindowToken();
            Object o = getContext().getSystemService(INPUT_METHOD_SERVICE);
            ((InputMethodManager) o).hideSoftInputFromWindow(token, 0);
            ((CreateUserActivity)getActivity()).setPassword(passwordEntry.getText().toString());
                nextButton.setVisibility(INVISIBLE);
                progressBar.setVisibility(VISIBLE);
                ((CreateUserActivity)getActivity()).createAccount();
        });

        return v;
    }

    protected String getHelpText() {
        return getString(R.string.setup_password_explanation);
    }
    public static void setError(TextInputLayout til, @Nullable String error,
                                boolean set) {
        if (set) {
            if (til.getError() == null) til.setError(error);
        } else {
            til.setError(null);
        }
    }
    public static float estimatePasswordStrength(String password){
        // The minimum number of unique characters in a strong password
	    final int STRONG_UNIQUE_CHARS = 12;
        HashSet<Character> unique = new HashSet<>();
        int length = password.length();
        for (int i = 0; i < length; i++) unique.add(password.charAt(i));
        return Math.min(1, (float) unique.size() / STRONG_UNIQUE_CHARS);
    }
}