package com.dx.anonymousmessenger.ui.view.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.account.DxAccount;
import com.dx.anonymousmessenger.call.DxCallService;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.tor.TorClient;
import com.dx.anonymousmessenger.ui.view.call.CallActivity;
import com.dx.anonymousmessenger.ui.view.log.LogActivity;
import com.dx.anonymousmessenger.ui.view.notepad.NotepadActivity;
import com.dx.anonymousmessenger.ui.view.setup.SetupInProcess;
import com.dx.anonymousmessenger.ui.view.single_activity.AddContactActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.MyIdentityActivity;
import com.dx.anonymousmessenger.ui.view.single_activity.MyProfileActivity;
import com.dx.anonymousmessenger.ui.view.tips.TipsActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AppFragment extends Fragment {
    private LinearLayout noContacts;
    private RecyclerView recyclerView;
    private ContactListAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    public Handler mainThread = null;
    private BroadcastReceiver mMyBroadcastReceiver;
    private View rootView;
    private ImageView onlineImg;
    private ImageView offlineImg;
    private TextView onlineTxt,torOutput;
    private Toolbar onlineToolbar;
    private List<String[]> lst;
    private Thread messageChecker = null;
    private volatile boolean pinging;
    private boolean started;

    public AppFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStop() {
        super.onStop();
        stopCheckingMessages();
        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopCheckingMessages();
        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
        recyclerView = null;
        mAdapter = null;
        layoutManager = null;
        mainThread = null;
        mMyBroadcastReceiver = null;
        rootView = null;
        onlineImg = null;
        offlineImg = null;
        onlineTxt = null;
        torOutput = null;
        onlineToolbar = null;
        lst = null;
        messageChecker = null;
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mMyBroadcastReceiver!=null){
            return;
        }
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
            if(intent.getStringExtra("tor_status")!=null){
                if(!onlineTxt.getText().toString().equals(getString(R.string.online)) && !onlineTxt.getText().toString().equals(getString(R.string.checking))){
                    checkConnectivity();
                }
//                if(Objects.requireNonNull(intent.getStringExtra("tor_status")).equals("ALL GOOD")){
//                    checkConnectivity();
//                }
                updateTorOutput(Objects.requireNonNull(intent.getStringExtra("tor_status")));
            }else{
                if(intent.getLongExtra("delete",-1)>-1 || Objects.equals(intent.getStringExtra("type"), "online_status")){
                    updateUi(false);
                }else{
                    updateUi();
                }
            }
            }
        };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String alias = prefs.getString("app-name","com.dx.anonymousmessenger.ui.view.MainActivity");
        if(alias!=null && alias.equals("com.dx.anonymousmessenger.ui.view.MainActivity")){
            requireActivity().setTitle(getString(R.string.app_name));
        }else if(alias!=null){
            requireActivity().setTitle(
                    alias.split("\\.")[alias.split("\\.").length-1]
            );
        }

        checkMessages();
        if(!started){
            updateUi();
            checkConnectivity();
            started = true;
        }else{
            updateUi(false);
        }

        if(((DxApplication) requireActivity().getApplication()).isInCall()){
            rootView.findViewById(R.id.frame_return_to_call).setVisibility(View.VISIBLE);
        }else{
            rootView.findViewById(R.id.frame_return_to_call).setVisibility(View.GONE);
        }

        if(!onlineTxt.getText().toString().equals(getString(R.string.online)) && !onlineTxt.getText().toString().equals(getString(R.string.checking))){
            checkConnectivity();
        }
        try {
            LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("your_action"));
            LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        ((AppActivity)requireActivity()).setTitle(getString(R.string.app_name));
//        if(!onlineTxt.getText().toString().equals(getString(R.string.online)) && !onlineTxt.getText().toString().equals(getString(R.string.checking))){
//            checkConnectivity();
//        }
//        checkMessages();
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_app, container, false);
        mainThread = new Handler(Looper.getMainLooper());
        FloatingActionButton btnAddContact = rootView.findViewById(R.id.btn_add_contact);
        btnAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AddContactActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            v.getContext().startActivity(intent);
        });
        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).getMenu().clear();
        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).inflateMenu(R.menu.app_menu);

//        new Thread(() -> {
//            try{
//                byte[] image = FileHelper.getFile(((DxApplication) requireActivity().getApplication()).getAccount().getProfileImagePath(), ((DxApplication) requireActivity().getApplication()));
//                if (image == null) {
//                    return;
//                }
//                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeByteArray(image, 0, image.length));
//                drawable.setCircular(true);
//                new Handler(Looper.getMainLooper()).post(()->{
//                    ((MaterialToolbar) requireActivity().findViewById(R.id.toolbar)).getMenu().getItem(0).setIcon(drawable);
//                });
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }).start();

        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).setNavigationOnClickListener((item)->{
//            if(item.getItemId()==R.id.action_my_profile){
                stopCheckingMessages();
                new Handler().postDelayed(()->{
                    try{
                        Intent intent = new Intent(getContext(), MyProfileActivity.class);
                        if(getContext()!=null){
                            View v =  ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar));
//                            ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), v, "profile_picture");
                            v.getContext().startActivity(intent);
//                        getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                },150);
//                return true;
//            }
//            return false;
        });
        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).setNavigationIcon(R.drawable.ic_baseline_account_circle_24);
        ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).setOnMenuItemClickListener((item)->{
//            PopupMenu popup = new PopupMenu(getContext(), v.);
////            popup.getMenuInflater()
//            popup.inflate(R.menu.app_menu);
//            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId()==R.id.action_settings){
                    ((AppActivity)requireActivity()).changeToSettingsFragment();
                    return true;
                }else if(item.getItemId()==R.id.action_clear_tor_cache){
                    new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
                        .setTitle(R.string.action_clear_tor_cache)
                        .setMessage(R.string.clear_tor_cache_explain)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(()->{
                            try {
                                ((DxApplication) requireActivity().getApplication()).getAndroidTorRelay().clearTorCache();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start())
                        .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
                    return true;
                }else if(item.getItemId()==R.id.action_restart_tor){
                    new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
                        .setTitle(R.string.restart_tor)
                        .setMessage(R.string.restart_tor_explain)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            ((DxApplication) requireActivity().getApplication()).restartTor();
                            mainThread.post(()->{
                                try{
                                    onlineTxt.setText(R.string.offline);
                                    onlineImg.setVisibility(View.GONE);
                                    offlineImg.setVisibility(View.VISIBLE);
                                    onlineToolbar.setVisibility(View.VISIBLE);
                                    Intent intent = new Intent(requireActivity().getApplication(), SetupInProcess.class);
                                    intent.putExtra("first_time",false);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    requireActivity().finish();
                                } catch (Exception ignored) {}
                            });
                        })
                        .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
                    return true;
                }else if(item.getItemId()==R.id.action_shutdown){
                    new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
                        .setTitle(R.string.shut_app)
                        .setMessage(R.string.shut_app_explain)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->{
                            requireActivity().finishAndRemoveTask();
                            requireActivity().overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                            try {
                                Thread.sleep(250);
                            } catch (Exception ignored) {}
                            ((DxApplication) requireActivity().getApplication()).shutdown();
                        })
                        .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
                    return true;
                }else if(item.getItemId()==R.id.action_my_identity){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), MyIdentityActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }/*else if(item.getItemId()==R.id.action_my_profile){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), MyProfileActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }*//*else if(item.getItemId()==R.id.action_about){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), AboutActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }*/else if(item.getItemId()==R.id.action_notepad){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), NotepadActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }else if(item.getItemId()==R.id.action_log){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), LogActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }else if(item.getItemId()==R.id.action_tips){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), TipsActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }/*else if(item.getItemId()==R.id.action_license){
                    stopCheckingMessages();
                    try{
                        Intent intent = new Intent(getContext(), LicenseActivity.class);
                        if(getContext()!=null){
                            getContext().startActivity(intent);
                        }
                    }catch (Exception ignored) {}
                    return true;
                }*/else{
                    return false;
                }
            });
            //displaying the popup
//            @SuppressLint("RestrictedApi") MenuPopupHelper menuHelper = new MenuPopupHelper(v.getContext(), (MenuBuilder) popup.getMenu(), v);
//            menuHelper.setForceShowIcon(true);
//            menuHelper.show();
//
////            popup.show();
//        });
        onlineImg = rootView.findViewById(R.id.synced_image);
        offlineImg = rootView.findViewById(R.id.unsynced_image);
        onlineTxt = rootView.findViewById(R.id.sync_text);
        onlineToolbar = rootView.findViewById(R.id.online_toolbar);
        onlineToolbar.setOnClickListener(v -> checkConnectivity());
        torOutput = rootView.findViewById(R.id.status_text);
        noContacts = rootView.findViewById(R.id.no_contacts);
        recyclerView = rootView.findViewById(R.id.recycler);
        // edit this line to reverse/unreverse layout
        layoutManager = new LinearLayoutManager(getContext(),RecyclerView.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);
        lst = new ArrayList<>();
        mAdapter = new ContactListAdapter((DxApplication) requireActivity().getApplication(),lst,this);
        recyclerView.setAdapter(mAdapter);
        ((SwipyRefreshLayout)rootView.findViewById(R.id.refresh)).setProgressBackgroundColor(R.color.dx_night_940);
        ((SwipyRefreshLayout)rootView.findViewById(R.id.refresh)).setColorSchemeResources(R.color.dx_white);
        ((SwipyRefreshLayout)rootView.findViewById(R.id.refresh)).setDistanceToTriggerSync(100);
        ((SwipyRefreshLayout)rootView.findViewById(R.id.refresh)).setOnRefreshListener(
                (direction) -> {
                    checkConnectivity();
                    updateUi();
                    ((SwipyRefreshLayout)rootView.findViewById(R.id.refresh)).setRefreshing(true);
                    new Handler().postDelayed(() -> ((SwipyRefreshLayout)rootView.findViewById(R.id.refresh)).setRefreshing(false),500);
                }
        );

        rootView.findViewById(R.id.btn_return_to_call).setOnClickListener(v->{
            if (((DxApplication) requireActivity().getApplication()).isInCall()){
                //goto call
                Intent contentIntent = new Intent(getActivity(), CallActivity.class);
                if(((DxApplication) requireActivity().getApplication()).getCc()!=null){
                    contentIntent.putExtra("address",((DxApplication) requireActivity().getApplication()).getCc().getAddress().substring(0,10));
                    if(!((DxApplication) requireActivity().getApplication()).getCc().isAnswered()){
                        contentIntent.setAction(DxCallService.ACTION_START_INCOMING_CALL);
                    }else{
                        contentIntent.setAction("");
                    }
                }
//                contentIntent.setAction(type);
                contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(contentIntent);
            }else{
                rootView.findViewById(R.id.frame_return_to_call).setVisibility(View.GONE);
            }
        });

//        if(!((DxApplication) requireActivity().getApplication()).isWeAsked()){
//            new Thread(()->{
//                try{
//                    Thread.sleep(1000);
//                }catch (Exception ignored){}
//                try{
//                    if(!((DxApplication) requireActivity().getApplication()).isIgnoringBatteryOptimizations()){
//                        requireActivity().runOnUiThread(()-> new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
//                            .setTitle(R.string.turn_off_battery)
//                            .setMessage(R.string.allow_in_background)
//                            .setIcon(android.R.drawable.ic_dialog_alert)
//                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
//                                ((DxApplication) requireActivity().getApplication()).requestBatteryOptimizationOff();
//                                ((DxApplication) requireActivity().getApplication()).setWeAsked(true);
//                            })
//                            .setNegativeButton(android.R.string.no, (dialog, whichButton)-> ((DxApplication) requireActivity().getApplication()).setWeAsked(true)).show());
//                    }
//                }catch (Exception ignored){}
//            }).start();
//        }
        return rootView;
    }

    public void checkMessages(){
        if(messageChecker!=null){
            return;
        }
        messageChecker = new Thread(()->{
            while (true){
                try{
                    if(getActivity()==null) break;
                    List<String[]> tmp = DbHelper.getContactsList((DxApplication) (getActivity()).getApplication());
                    if(tmp==null )break;
                    if(!Utils.arrayListEquals(lst,tmp)){
                        updateUi(false,tmp);
//                        updateUi();
                    }
                    //noinspection BusyWait
                    Thread.sleep(5000);
                }catch (Exception ignored){
                    break;}
            }
        });
        messageChecker.start();
    }

    public void stopCheckingMessages(){
        if(messageChecker==null){
            return;
        }
        if(messageChecker.isAlive()){
            messageChecker.interrupt();
        }
        messageChecker = null;
    }

    public void updateUi(){
//        new Thread(()->{
//            if(getActivity()==null || mainThread==null){
//                return;
//            }
//            try{
//                lst = DbHelper.getContactsList((DxApplication) (getActivity()).getApplication());
//                if(lst==null){
//                    return;
//                }
//                mainThread.post(()->{
//                    try{
//                        if(lst.isEmpty()){
//                            noContacts.setVisibility(View.VISIBLE);
//                        }else{
//                            noContacts.setVisibility(View.GONE);
//                        }
////                        mAdapter = new MyRecyclerViewAdapter((DxApplication) getActivity().getApplication(),lst,this);
//                        mAdapter.mData = lst;
////                        recyclerView.setAdapter(mAdapter);
//                        mAdapter.notifyDataSetChanged();
//                        recyclerView.scheduleLayoutAnimation();
//                        ((DxApplication)getActivity().getApplication()).clearMessageNotification();
//                    } catch (Exception ignored) {}
//                });
//            }catch (Exception ignored){}
//        }).start();
        updateUi(true);
    }

    public void updateUi(boolean animate){
        updateUi(animate,null);
    }

    public void updateUi(boolean animate, List<String[]> tmp){
        new Thread(()->{
            if(getActivity()==null || mainThread==null){
                return;
            }
            try{
                try{
                    DxAccount account = ((DxApplication) requireActivity().getApplication()).getAccount();
                    if(account==null){
                        throw new Resources.NotFoundException("");
                    }
                    if(account.getProfileImagePath()==null){
                        throw new Resources.NotFoundException("");
                    }
                    if(account.getProfileImagePath().equals("")){
                        throw new Resources.NotFoundException("");
                    }

                    //commented this out because I was not able to make the profile image small enough here to fit in an icon correctly...

//                    byte[] image = FileHelper.getFile(((DxApplication) requireActivity().getApplication()).getAccount().getProfileImagePath(), ((DxApplication) requireActivity().getApplication()));
//                    if (image == null) {
//                        throw new Resources.NotFoundException("");
//                    }
//                    RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeByteArray(image, 0, image.length));
//                    drawable.setCircular(true);
//                    new Handler(Looper.getMainLooper()).post(()-> ((MaterialToolbar)requireActivity().findViewById(R.id.toolbar)).setNavigationIcon(drawable));
                }catch (Exception ignored){}
                if(tmp!=null){
                    lst = tmp;
                }else{
                    lst = DbHelper.getContactsList((DxApplication) (getActivity()).getApplication());
                }
                if(lst==null){
                    return;
                }
                mainThread.post(()->{
                    try{
                        ProgressBar pb = requireActivity().findViewById(R.id.progress_contacts);
                        if(pb!=null){
                            pb.setVisibility(View.GONE);
                        }
                        if(lst.isEmpty()){
                            noContacts.setVisibility(View.VISIBLE);
                        }else{
                            noContacts.setVisibility(View.GONE);
                        }
                        mAdapter.mData = lst;
//                        recyclerView.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                        if(animate){
                            recyclerView.scheduleLayoutAnimation();
                        }
                        ((DxApplication)getActivity().getApplication()).clearMessageNotification();
                    } catch (Exception ignored) {}
                });
            }catch (Exception ignored){}
        }).start();
    }

//    public void updateUi(List<String[]> tmp){
//        if(tmp==null){
//            return;
//        }
//        new Thread(()->{
//            if(getActivity()==null || mainThread==null){
//                return;
//            }
//            try{
//                try{
//                    byte[] image = FileHelper.getFile(((DxApplication) requireActivity().getApplication()).getAccount().getProfileImagePath(), ((DxApplication) requireActivity().getApplication()));
//                    if (image == null) {
//                        return;
//                    }
//                    RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), BitmapFactory.decodeByteArray(image, 0, image.length));
//                    drawable.setCircular(true);
//                    new Handler(Looper.getMainLooper()).post(()->{
//                        ((MaterialToolbar) requireActivity().findViewById(R.id.toolbar)).getMenu().getItem(0).setIcon(drawable);
//                    });
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                lst = tmp;
//                mainThread.post(()->{
//                    if(lst.isEmpty()){
//                        noContacts.setVisibility(View.VISIBLE);
//                    }else{
//                        noContacts.setVisibility(View.GONE);
//                    }
//                    mAdapter = new ContactListAdapter((DxApplication) getActivity().getApplication(),lst,this);
//                    recyclerView.setAdapter(mAdapter);
//                    mAdapter.notifyDataSetChanged();
////                    recyclerView.scheduleLayoutAnimation();
//                });
//            }catch (Exception ignored){}
//        }).start();
//    }

    public boolean isOnline() {
        if(getActivity() !=null) {
            return TorClient.test((DxApplication) requireActivity().getApplication());
        }
        return false;
    }

    public void checkConnectivity(){
        if(getActivity()==null || mainThread==null || onlineToolbar==null || onlineImg==null || onlineTxt==null || offlineImg==null){
                return;
        }
        if(pinging){
            return;
        }
        pinging = true;
        try{
            onlineImg.setVisibility(View.GONE);
            offlineImg.setVisibility(View.GONE);
            onlineTxt.setText(R.string.checking);
            onlineTxt.setVisibility(View.GONE);
            onlineToolbar.setVisibility(View.VISIBLE);
            ColorDrawable[] color = {new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.startGradientColor)), new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.endGradientColor))};
            TransitionDrawable trans = new TransitionDrawable(color);
            onlineToolbar.setBackground(trans);
            trans.startTransition(1500);

        }catch (Exception ignored){}

        new Thread(()->{
            if(getActivity()==null || mainThread==null){
                pinging = false;
                return;
            }
            try{
                if(getActivity()==null || getActivity().getApplication()==null){
                    pinging = false;
                    return;
                }
                if(isOnline()){
                    mainThread.post(()->{
                        try{
                            onlineTxt.setText(R.string.online);
//                            onlineImg.setVisibility(View.VISIBLE);
//                            offlineImg.setVisibility(View.GONE);
                            onlineToolbar.setVisibility(View.VISIBLE);
                            ColorDrawable[] color = {new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.endGradientColor)), new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.green_tor))};
                            TransitionDrawable trans = new TransitionDrawable(color);
                            onlineToolbar.setBackground(trans);
                            trans.startTransition(1500);
                        }catch (Exception ignored) {}
                    });
                    new Thread(()-> ((DxApplication) requireActivity().getApplication()).queueAllUnsentMessages()).start();
                }else{
                    mainThread.post(()->{
                        try{
                            onlineTxt.setText(R.string.offline);
//                            onlineImg.setVisibility(View.GONE);
//                            offlineImg.setVisibility(View.VISIBLE);
                            onlineToolbar.setVisibility(View.VISIBLE);
                            ColorDrawable[] color = {new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.endGradientColor)), new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.red_500))};
                            TransitionDrawable trans = new TransitionDrawable(color);
                            onlineToolbar.setBackground(trans);
                            trans.startTransition(1500);
                        }catch (Exception ignored) {}
                    });
                }
                pinging = false;
            }catch (Exception ignored){pinging = false;}
        }).start();
    }

    private void updateTorOutput(String tor_status) {
        if(tor_status!=null && tor_status.equals("tor_error")){
            torOutput.setText(getText(R.string.tor_exited));
        }
        torOutput.setText(tor_status);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.app_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);//activity_fragment_container_toolbar
    }

}