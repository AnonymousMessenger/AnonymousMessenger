package com.dx.anonymousmessenger.ui.view.setup;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.service.DxService;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.ui.view.MainActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.AboutActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.DonateActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.LicenseActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.SimpleScannerActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetupSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupSettingsFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    private boolean isInSetup;
    final ActivityResultLauncher<Intent> mScanQrCode = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        String bridges = result.getData().getStringExtra("RESULT");
                        if (bridges != null) {
                            String[] lines = bridges.split(",");
                            for (String line : lines) {
                                DbHelper.saveBridge(line.trim().replace("'", "").replace("[","").replace("]",""), (DxApplication) requireActivity().getApplication());
                            }
                        }
                    }
                    updateBridgeList();
                }
            });
    final ActivityResultLauncher<String> mPermissionResult = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if(result) {
                    Intent intent = new Intent(requireContext(), SimpleScannerActivity.class);
                    intent.putExtra("SCAN_MODE", "ADD_BRIDGE");
                    mScanQrCode.launch(intent);
                }
            });

    public SetupSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param isInSetup boolean indicates whether the view is used within first time use setup or not.
     * @return A new instance of fragment SetupSettingsFragment.
     */
    public static SetupSettingsFragment newInstance(boolean isInSetup) {
        SetupSettingsFragment fragment = new SetupSettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, isInSetup);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isInSetup = getArguments().getBoolean(ARG_PARAM1);
        }
    }


    @Override
    public void onDestroyView() {
        ((DxApplication)requireActivity().getApplication()).reloadSettings();
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_setup_settings, container, false);

        final ConstraintLayout bridgesLayout = rootView.findViewById(R.id.bridge_settings_layout);
        final Button done = rootView.findViewById(R.id.done);
        final SwitchMaterial bridgesSwitch = rootView.findViewById(R.id.switch_bridges);
        final SwitchMaterial unknownContactsSwitch = rootView.findViewById(R.id.switch_allow_unknown);
        final SwitchMaterial allowCalls = rootView.findViewById(R.id.switch_allow_calls);
        final SwitchMaterial allowFilesSwitch = rootView.findViewById(R.id.switch_allow_files);
        final TextInputEditText txtFileLimit = rootView.findViewById(R.id.txt_file_limit);
        final TextInputLayout layoutFileLimit = rootView.findViewById(R.id.txt_layout_file_limit);
        final TextInputEditText txtCheckAddress = rootView.findViewById(R.id.txt_check_address);
        final RecyclerView rvBridges = rootView.findViewById(R.id.rv_bridges);
        final Button addBridge = rootView.findViewById(R.id.btn_add_bridge);
        final Button scanBridge = rootView.findViewById(R.id.btn_scan_bridges);
        final SwitchMaterial enableSocks5Proxy = rootView.findViewById(R.id.switch_use_proxy);
        final TextInputLayout layoutProxy = rootView.findViewById(R.id.txt_layout_proxy);
        final TextInputEditText txtProxyAddress = rootView.findViewById(R.id.txt_proxy_address);
        final TextInputEditText txtProxyUsername = rootView.findViewById(R.id.txt_proxy_username);
        final TextInputEditText txtProxyPassword = rootView.findViewById(R.id.txt_proxy_password);
        final TextInputLayout layoutExclude = rootView.findViewById(R.id.txt_layout_exclude);
        final TextInputEditText txtExclude = rootView.findViewById(R.id.txt_exclude);
        final SwitchMaterial excludeUnknown = rootView.findViewById(R.id.switch_exclude_unknown);
        final SwitchMaterial strictExclude = rootView.findViewById(R.id.switch_strict_exclude);
        final TextView reset = rootView.findViewById(R.id.btn_reset);

        rootView.findViewById(R.id.fab_check_address_help).setOnClickListener(v -> Utils.showHelpAlert(requireContext(),getString(R.string.online_check_address_explain), getString(R.string.online_check_address)));

        rootView.findViewById(R.id.fab_reset_check_address).setOnClickListener(v -> txtCheckAddress.setText(((DxApplication)requireActivity().getApplication()).DEFAULT_SETTINGS[4].toString()));

        rootView.findViewById(R.id.btn_request_bridge).setOnClickListener(v -> Utils.showHelpAlert(requireContext(),getString(R.string.request_bridge_help), getString(R.string.request_bridge)));

        reset.setOnClickListener(v -> {
            try{
                DbHelper.deleteSettings((DxApplication)requireActivity().getApplication());
            }catch (Exception ignored){}
            Object[] settings = ((DxApplication)requireActivity().getApplication()).DEFAULT_SETTINGS;
            bridgesSwitch.setChecked(((int)settings[0]>0));
            bridgesLayout.setVisibility((int)settings[0]>0?View.VISIBLE:View.GONE);
            unknownContactsSwitch.setChecked(((int)settings[1]>0));
            allowCalls.setChecked((int)settings[2]>0);
            allowFilesSwitch.setChecked((int)settings[3]>0);
            layoutFileLimit.setVisibility((int)settings[3]>0?View.VISIBLE:View.GONE);
            txtCheckAddress.setText((String) settings[4]);
            txtFileLimit.setText((String) settings[5]);
            enableSocks5Proxy.setChecked((int)settings[6]>0);
            layoutProxy.setVisibility((int)settings[6]>0?View.VISIBLE:View.GONE);
            txtProxyAddress.setText((String) settings[7]);
            txtProxyUsername.setText((String) settings[8]);
            txtProxyPassword.setText((String) settings[9]);
            txtExclude.setText((String) settings[10]);
            excludeUnknown.setChecked((int)settings[11]>0);
            strictExclude.setChecked((int)settings[12]>0);
        });

        //get default values
        if(isInSetup){
            bridgesSwitch.setChecked(((CreateUserActivity) requireActivity()).areBridgesEnabled());
            bridgesLayout.setVisibility(((CreateUserActivity) requireActivity()).areBridgesEnabled()?View.VISIBLE:View.GONE);
            allowCalls.setChecked(((CreateUserActivity) requireActivity()).isAcceptingCallsAllowed());
            unknownContactsSwitch.setChecked(((CreateUserActivity) requireActivity()).isAcceptingUnknownContactsEnabled());
            allowFilesSwitch.setChecked(((CreateUserActivity) requireActivity()).isReceivingFilesAllowed());
            layoutFileLimit.setVisibility(((CreateUserActivity) requireActivity()).isReceivingFilesAllowed()?View.VISIBLE:View.GONE);
            txtCheckAddress.setText(((CreateUserActivity) requireActivity()).getCheckAddress());
            txtFileLimit.setText(((CreateUserActivity) requireActivity()).getFileSizeLimit());
            enableSocks5Proxy.setChecked(((CreateUserActivity) requireActivity()).isEnableSocks5Proxy());
            layoutProxy.setVisibility(((CreateUserActivity) requireActivity()).isEnableSocks5Proxy()?View.VISIBLE:View.GONE);
            txtProxyAddress.setText(((CreateUserActivity) requireActivity()).getSocks5AddressAndPort());
            txtProxyUsername.setText(((CreateUserActivity) requireActivity()).getSocks5Username());
            txtProxyPassword.setText(((CreateUserActivity) requireActivity()).getSocks5Password());
            txtExclude.setText(((CreateUserActivity) requireActivity()).getExcludeText());
            excludeUnknown.setChecked(((CreateUserActivity) requireActivity()).isExcludeUnknown());
            strictExclude.setChecked(((CreateUserActivity) requireActivity()).isStrictExclude());
        }else{
            try{
                ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).getMenu().clear();
                requireActivity().setTitle(R.string.action_settings);
                ((DxActivity)requireActivity()).setBackEnabled(true);
            }catch (Exception ignored){}

            //read from db in this case or fallback to defaults
            try{
                Object[] settings = DbHelper.getSettingsList((DxApplication)requireActivity().getApplication(),true);
                if (settings == null || settings.length <= 5) {
                    Log.d("ANONYMOUSMESSENGER","not right");
                    settings = ((DxApplication) requireActivity().getApplication()).DEFAULT_SETTINGS;
                }
                bridgesSwitch.setChecked(((int)settings[0]>0));
                bridgesLayout.setVisibility((int)settings[0]>0?View.VISIBLE:View.GONE);
                unknownContactsSwitch.setChecked(((int)settings[1]>0));
                allowCalls.setChecked((int)settings[2]>0);
                allowFilesSwitch.setChecked((int)settings[3]>0);
                layoutFileLimit.setVisibility((int)settings[3]>0?View.VISIBLE:View.GONE);
                txtCheckAddress.setText((String) settings[4]);
                txtFileLimit.setText((String) settings[5]);
                enableSocks5Proxy.setChecked((int)settings[6]>0);
                layoutProxy.setVisibility((int)settings[6]>0?View.VISIBLE:View.GONE);
                txtProxyAddress.setText((String) settings[7]);
                txtProxyUsername.setText((String) settings[8]);
                txtProxyPassword.setText((String) settings[9]);
                txtExclude.setText((String) settings[10]);
                excludeUnknown.setChecked((int)settings[11]>0);
                strictExclude.setChecked((int)settings[12]>0);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //set about & license text view buttons to be visible
        rootView.findViewById(R.id.txt_other).setVisibility(View.VISIBLE);
        TextView changeAppName = rootView.findViewById(R.id.btn_change_app_name);
        changeAppName.setVisibility(View.VISIBLE);
        changeAppName.setOnClickListener(v -> {
            CharSequence[] names = new CharSequence[]{"Anonymous Messenger", "Securoo", "AM"};

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext(),R.style.AppAlertDialog);
            builder.setTitle(R.string.select_app_name);
            builder.setItems(names, (dialog, which) -> {
                switch (which) {
                    case 0:
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        prefs.edit().putString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity").apply();
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), MainActivity.class),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), "com.dx.anonymousmessenger.ui.view.AM"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(),  "com.dx.anonymousmessenger.ui.view.Securoo"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        Intent intent = new Intent();
                        String packageName = requireContext().getPackageName();
                        String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
                        ComponentName componentName = new ComponentName(packageName,
                                Objects.requireNonNull(alias));
                        intent.setComponent(componentName);
                        requireActivity().finishAndRemoveTask();
                        requireActivity().overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        startActivity(intent);
                        break;
                    case 1:
                        SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        prefs2.edit().putString("app-name","com.dx.anonymousmessenger.ui.view.Securoo").apply();
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), "com.dx.anonymousmessenger.ui.view.Securoo"),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), MainActivity.class),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(),  "com.dx.anonymousmessenger.ui.view.AM"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        intent = new Intent();
                        packageName = requireContext().getPackageName();
                        alias = prefs2.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
                        componentName = new ComponentName(packageName,
                                Objects.requireNonNull(alias));
                        intent.setComponent(componentName);
                        requireActivity().finishAndRemoveTask();
                        requireActivity().overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        startActivity(intent);
                        break;
                    case 2:
                        SharedPreferences prefs3 = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        prefs3.edit().putString("app-name","com.dx.anonymousmessenger.ui.view.AM").apply();
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), "com.dx.anonymousmessenger.ui.view.AM"),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), MainActivity.class),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        requireActivity().getPackageManager().setComponentEnabledSetting(
                                new ComponentName(requireActivity(), "com.dx.anonymousmessenger.ui.view.Securoo"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        intent = new Intent();
                        packageName = requireContext().getPackageName();
                        alias = prefs3.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
                        componentName = new ComponentName(packageName,
                                Objects.requireNonNull(alias));
                        intent.setComponent(componentName);
                        requireActivity().finishAndRemoveTask();
                        requireActivity().overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        startActivity(intent);
                        break;
                }
                // refresh service notification
                if(DxApplication.isServiceRunningInForeground(requireContext(), DxService.class)){
                    ((DxApplication)requireActivity().getApplication()).updateServiceNotification();
                }
            });
            builder.show();
        });
        TextView changeTheme = rootView.findViewById(R.id.btn_change_theme);
        changeTheme.setVisibility(View.VISIBLE);
        changeTheme.setOnClickListener(v -> {
            CharSequence[] names = new CharSequence[]{getString(R.string.dark_theme), getString(R.string.light_theme)};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext(),R.style.AppAlertDialog);
            builder.setTitle(R.string.action_change_theme);
            builder.setItems(names, (dialog, which) -> {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                Intent intent = new Intent();
                String packageName = requireContext().getPackageName();
                String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
                ComponentName componentName = new ComponentName(packageName,
                        Objects.requireNonNull(alias));
                intent.setComponent(componentName);
                switch (which) {
                    case 0:
                        prefs.edit().putString("app-theme","dark").apply();
                        requireActivity().setTheme(R.style.AppTheme);
                        requireActivity().finishAndRemoveTask();
                        requireActivity().overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                        startActivity(intent);
                        break;
                    case 1:
                        SharedPreferences prefs2 = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        prefs2.edit().putString("app-theme","light").apply();
                        requireActivity().setTheme(R.style.LightTheme);
                        requireActivity().finishAndRemoveTask();
                        requireActivity().overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                        startActivity(intent);
                        break;
                }
            });
            builder.show();
        });
        TextView donate = rootView.findViewById(R.id.btn_donate);
        donate.setVisibility(View.VISIBLE);
        donate.setOnClickListener((v)->{
            try{
                Intent intent = new Intent(getContext(), DonateActivity.class);
                if(getContext()!=null){
                    getContext().startActivity(intent);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        });
        TextView license = rootView.findViewById(R.id.btn_license);
        license.setVisibility(View.VISIBLE);
        license.setOnClickListener((v)->{
            try{
                Intent intent = new Intent(getContext(), LicenseActivity.class);
                if(getContext()!=null){
                    getContext().startActivity(intent);
                }
            }catch (Exception ignored) {}
        });
        TextView about = rootView.findViewById(R.id.btn_about);
        about.setVisibility(View.VISIBLE);
        about.setOnClickListener((v)->{
            try{
                Intent intent = new Intent(getContext(), AboutActivity.class);
                if(getContext()!=null){
                    getContext().startActivity(intent);
                }
            }catch (Exception ignored) {}
        });

        //exclude Tor nodes related
        txtExclude.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //validate and output errors
                //accepted format is : xx,xx...

                String[] array = s.toString().split(",");
                for (String value : array) {
                    if (value.length() != 2 && value.length() != 0) {
                        txtExclude.setError("Bad format, setting unchanged");
                        return;
                    }
                }

                //if good: save it
                if(isInSetup){
                    ((CreateUserActivity) requireActivity()).setExcludeText(s.toString());
                }else{
                    DbHelper.saveExcludeText(s.toString(),(DxApplication)requireActivity().getApplication());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        excludeUnknown.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setExcludeUnknown(isChecked);
            }else{
                DbHelper.saveExcludeUnknown(isChecked,(DxApplication)requireActivity().getApplication());
            }
        }));

        strictExclude.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setStrictExclude(isChecked);
            }else{
                DbHelper.saveStrictExclude(isChecked,(DxApplication)requireActivity().getApplication());
            }
        }));

        //proxy related
        enableSocks5Proxy.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setEnableSocks5Proxy(isChecked);
            }else{
                DbHelper.saveEnableSocks5Proxy(isChecked,(DxApplication)requireActivity().getApplication());
            }
            layoutProxy.setVisibility(isChecked?View.VISIBLE:View.GONE);
            if (!isChecked) {
                ((InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(requireView().getWindowToken(), 0);
            }
        }));

        txtProxyAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //validate and output errors
                //accepted format is : ip:port
                String ipPattern = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
                if(s.toString().contains(":") && s.toString().split(":").length==2){
                    try{
                        String substring = s.toString().split(":")[1];
                        if(substring.length()<1 || Integer.parseInt(substring)<1 || Integer.parseInt(substring)>65535){
                            txtProxyAddress.setError("Bad format, setting unchanged");
                            return;
                        }

                        if(!s.toString().split(":")[0].matches(ipPattern)){
                            txtProxyAddress.setError("Bad format, setting unchanged");
                            return;
                        }
                    }catch (NumberFormatException ignored){
                        txtProxyAddress.setError("Bad format, setting unchanged");
                        return;
                    }
                }else{
                    if(!s.toString().matches(ipPattern)){
                        txtProxyAddress.setError("Bad format, setting unchanged");
                        return;
                    }
                }
                //if good: save it
                if(isInSetup){
                    ((CreateUserActivity) requireActivity()).setSocks5AddressAndPort(s.toString());
                }else{
                    DbHelper.saveSocks5AddressAndPort(s.toString(),(DxApplication)requireActivity().getApplication());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        txtProxyUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //validate and output errors
                //accepted format is : ip:port

                if(s.toString().length()>255){
                    txtProxyUsername.setError("Bad format, setting unchanged");
                    return;
                }

                //if good: save it
                if(isInSetup){
                    ((CreateUserActivity) requireActivity()).setSocks5Username(s.toString());
                }else{
                    DbHelper.saveSocks5Username(s.toString(),(DxApplication)requireActivity().getApplication());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        txtProxyPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //validate and output errors
                //accepted format is : ip:port

                if(s.toString().length()>255){
                    txtProxyPassword.setError("Bad format, setting unchanged");
                    return;
                }

                //if good: save it
                if(isInSetup){
                    ((CreateUserActivity) requireActivity()).setSocks5Password(s.toString());
                }else{
                    DbHelper.saveSocks5Password(s.toString(),(DxApplication)requireActivity().getApplication());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // bridge related
        bridgesSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setBridgesEnabled(isChecked);
            }else{
                DbHelper.saveEnableBridges(isChecked,(DxApplication)requireActivity().getApplication());
            }
            bridgesLayout.setVisibility(isChecked?View.VISIBLE:View.GONE);
        }));

        scanBridge.setOnClickListener(v -> {
//            final int QR_RESULT_CODE = 0;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(requireContext(), SimpleScannerActivity.class);
                intent.putExtra("SCAN_MODE", "ADD_BRIDGE");
                mScanQrCode.launch(intent);
            }else{

                mPermissionResult.launch(Manifest.permission.CAMERA);
//                Utils.getCameraPerms(requireActivity(),2);
            }
        });

        addBridge.setOnClickListener(v -> {
            //todo: add explain. u can paste multiline and no need for the word bridge and it's meek_lite not meek
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.insert_bridge_line);

            // Set up the input
            final EditText input = new EditText(v.getContext());
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                ((InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(requireView().getWindowToken(), 0);
                //split input into bridge lines separated by \n
                String[] lines = input.getText().toString().split(System.lineSeparator());
                if(isInSetup){
                    List<String> list = ((CreateUserActivity) requireActivity()).getBridgeList();
                    list.addAll(Arrays.asList(lines));
                    ((CreateUserActivity) requireActivity()).setBridgeList(list);
                }else{
                    for (String line : lines) {
                        DbHelper.saveBridge(line, (DxApplication) requireActivity().getApplication());
                    }
                }
                updateBridgeList();
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                ((InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(requireView().getWindowToken(), 0);
                dialog.cancel();
            });
            builder.show();
        });

        //other switches

        allowCalls.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setAcceptingCallsAllowed(isChecked);
            }else{
                DbHelper.saveIsAcceptingCallsAllowed(isChecked,(DxApplication)requireActivity().getApplication());
            }
        }));

        unknownContactsSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setAcceptingUnknownContactsEnabled(isChecked);
            }else{
                DbHelper.saveIsAcceptingUnknownContactsEnabled(isChecked,(DxApplication)requireActivity().getApplication());
            }
        }));

        allowFilesSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isInSetup){
                ((CreateUserActivity) requireActivity()).setReceivingFilesAllowed(isChecked);
            }else{
                DbHelper.saveIsReceivingFilesAllowed(isChecked,(DxApplication)requireActivity().getApplication());
            }
            layoutFileLimit.setVisibility(isChecked?View.VISIBLE:View.GONE);
            if (!isChecked) {
                ((InputMethodManager) requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(requireView().getWindowToken(), 0);
            }
        }));

        txtFileLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //validate and output errors
                //accepted format is : 0-999[gb/mb/kb]
                if(!s.toString().trim().endsWith("gb") && !s.toString().trim().endsWith("mb") && !s.toString().trim().endsWith("kb") && !s.toString().trim().endsWith("GB") && !s.toString().trim().endsWith("MB") && !s.toString().trim().endsWith("KB")){
                    txtFileLimit.setError("Bad format, setting unchanged");
                    return;
                }
                try{
                    String substring = s.toString().trim().substring(0, s.toString().length() - 2).trim();
                    if(substring.length()>3 || substring.length()<1 || Integer.parseInt(substring)<1){
                        txtFileLimit.setError("Bad format, setting unchanged");
                        return;
                    }
                }catch (NumberFormatException ignored){
                    txtFileLimit.setError("Bad format, setting unchanged");
                    return;
                }
                //if good: save it
                if(isInSetup){
                    ((CreateUserActivity) requireActivity()).setFileSizeLimit(s.toString());
                }else{
                    DbHelper.saveFileSizeLimit(s.toString(),(DxApplication)requireActivity().getApplication());
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        txtCheckAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //validate and output errors
                if(s.toString().contains(":") || s.toString().contains("//")){
                    txtCheckAddress.setError("Bad address, setting unchanged");
                    return;
                }
                if(isInSetup){
                    ((CreateUserActivity) requireActivity()).setCheckAddress(s.toString());
                }else{
                    DbHelper.saveCheckAddress(s.toString(),(DxApplication)requireActivity().getApplication());
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        List<String> list;
        if(isInSetup){
            list = ((CreateUserActivity) requireActivity()).getBridgeList();

            done.setText(R.string.next_button);
            done.setOnClickListener(v -> ((CreateUserActivity) requireActivity()).changeToPasswordFragment());
        }else{
            list = DbHelper.getBridgeList((DxApplication) requireActivity().getApplication());

            done.setVisibility(View.GONE);
        }

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getActivity());
        rvBridges.setLayoutManager(layoutManager);
        BridgeRecyclerViewAdapter adapter = new BridgeRecyclerViewAdapter(this.getActivity(), list);
        rvBridges.setAdapter(adapter);

        // Inflate the layout for this fragment
        return rootView;
    }

    public void updateBridgeList(){
        new Thread(()->{
            List<String> list;
            if(isInSetup){
                list = ((CreateUserActivity) requireActivity()).getBridgeList();
            }else{
                list = DbHelper.getBridgeList((DxApplication) requireActivity().getApplication());
            }
            requireActivity().runOnUiThread(()->{
                RecyclerView recyclerView = requireActivity().findViewById(R.id.rv_bridges);
                LinearLayoutManager layoutManager
                        = new LinearLayoutManager(getActivity());
                recyclerView.setLayoutManager(layoutManager);
                BridgeRecyclerViewAdapter adapter = new BridgeRecyclerViewAdapter(getActivity(), list);
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(list.size()-1);
            });
        }).start();
    }
}