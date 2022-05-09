package com.dx.anonymousmessenger.ui.view.setup;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.dx.anonymousmessenger.crypto.PasswordStrengthEstimator.QUITE_WEAK;

import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.ui.custom.StrengthMeter;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetupPasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupPasswordFragment extends Fragment {

    private TextInputLayout passwordEntryWrapper;
    private TextInputLayout passwordConfirmationWrapper;
    private TextInputEditText passwordEntry;
    private TextInputEditText passwordConfirmation;
    private StrengthMeter strengthMeter;
    private Button nextButton;
    private Button easyButton;
    private ProgressBar progressBar;

    public SetupPasswordFragment() {
        // Required empty public constructor
    }

    public static SetupPasswordFragment newInstance() {
        SetupPasswordFragment fragment = new SetupPasswordFragment();
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
        View v = inflater.inflate(R.layout.fragment_setup_password2, container,
                false);

        strengthMeter = v.findViewById(R.id.strength_meter);
        passwordEntryWrapper = v.findViewById(R.id.password_entry_wrapper);
        passwordEntry = v.findViewById(R.id.password_entry);
        passwordConfirmationWrapper =
                v.findViewById(R.id.password_confirm_wrapper);
        passwordConfirmation = v.findViewById(R.id.password_confirm);
        nextButton = v.findViewById(R.id.next);
        easyButton = v.findViewById(R.id.btn_easy_password);
        progressBar = v.findViewById(R.id.progress);
        FloatingActionButton help = v.findViewById(R.id.fab_password_help);

        help.setOnClickListener(view -> Utils.showHelpAlert(requireContext(),getString(R.string.setup_password_explanation), getString(R.string.password_explain_title)));

        passwordEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence authorName, int i, int i1, int i2) {
                String password1 = Objects.requireNonNull(passwordEntry.getText()).toString();
                String password2 = Objects.requireNonNull(passwordConfirmation.getText()).toString();
                boolean passwordsMatch = password1.equals(password2);

                strengthMeter
                        .setVisibility(password1.length() > 0 ? VISIBLE : INVISIBLE);
                float strength = estimatePasswordStrength(password1);
                strengthMeter.setStrength(strength);
                boolean strongEnough = strength > QUITE_WEAK;

//                setError(passwordEntryWrapper, getString(R.string.password_too_weak),
//                        password1.length() > 0 && !strongEnough);
                setError(passwordConfirmationWrapper,
                        getString(R.string.passwords_do_not_match),
                        password2.length() > 0 && !passwordsMatch);

                nextButton.setEnabled(passwordsMatch);
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
                String password1 = Objects.requireNonNull(passwordEntry.getText()).toString();
                String password2 = Objects.requireNonNull(passwordConfirmation.getText()).toString();
                boolean passwordsMatch = password1.equals(password2);

                strengthMeter
                        .setVisibility(password1.length() > 0 ? VISIBLE : INVISIBLE);
                float strength = estimatePasswordStrength(password1);
                strengthMeter.setStrength(strength);
                boolean strongEnough = strength > QUITE_WEAK;

//                setError(passwordEntryWrapper, getString(R.string.password_too_weak),
//                        password1.length() > 0 && !strongEnough);
                setError(passwordConfirmationWrapper,
                        getString(R.string.passwords_do_not_match),
                        password2.length() > 0 && !passwordsMatch);

                nextButton.setEnabled(passwordsMatch);
//                passwordConfirmation.setOnEditorActionListener(enabled ? (TextView.OnEditorActionListener) this : null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        nextButton.setOnClickListener(v1 -> {
//            ((CreateUserActivity)getActivity()).setOkayToBack(false);
            IBinder token = passwordEntry.getWindowToken();
            Object o = requireContext().getSystemService(INPUT_METHOD_SERVICE);
            ((InputMethodManager) o).hideSoftInputFromWindow(token, 0);
            passwordEntry.setEnabled(false);
            passwordConfirmation.setEnabled(false);
            nextButton.setVisibility(INVISIBLE);
            progressBar.setVisibility(VISIBLE);
            ((CreateUserActivity) requireActivity()).createAccount(Objects.requireNonNull(passwordEntry.getText()).toString().getBytes(StandardCharsets.UTF_8));

//            Intent intent = new Intent(getActivity(), SetupInProcess.class);
//            startActivity(intent);
//            if(getActivity()!=null){
//                getActivity().finish();
//            }
        });

        easyButton.setOnClickListener(v1 ->{
            IBinder token = passwordEntry.getWindowToken();
            Object o = requireContext().getSystemService(INPUT_METHOD_SERVICE);
            ((InputMethodManager) o).hideSoftInputFromWindow(token, 0);
            passwordEntry.setEnabled(false);
            passwordConfirmation.setEnabled(false);
            nextButton.setVisibility(INVISIBLE);
            progressBar.setVisibility(VISIBLE);
            //set easy password here
            ((CreateUserActivity) requireActivity()).createAccount("easy_password".getBytes(StandardCharsets.UTF_8));
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