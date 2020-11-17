package com.dx.anonymousmessenger;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AppFragment extends Fragment {
    private LinearLayout noContacts;
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter mAdapter;
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

    public AppFragment() {
        // Required empty public constructor
    }

    public static AppFragment newInstance() {
        AppFragment fragment = new AppFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStop() {
        super.onStop();
        stopCheckingMessages();
        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
        super.onPause();
    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        stopCheckingMessages();
//        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
//    }

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
            if(intent.getStringExtra("tor_status1")!=null){
                if(onlineTxt.getText().toString().equals(getString(R.string.offline))){
                    checkConnectivity();
                }
//                if(Objects.requireNonNull(intent.getStringExtra("tor_status1")).equals("ALL GOOD")){
//                    checkConnectivity();
//                }
                updateTorOutput(Objects.requireNonNull(intent.getStringExtra("tor_status1")));
            }else{
                updateUi();
            }
            }
        };
        checkMessages();
        updateUi();
        if(onlineTxt.getText().toString().equals(getString(R.string.offline))){
            checkConnectivity();
        }
        try {
            LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("your_action"));
            LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status1"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

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

        onlineImg = rootView.findViewById(R.id.synced_image);
        offlineImg = rootView.findViewById(R.id.unsynced_image);
        onlineTxt = rootView.findViewById(R.id.sync_text);
        onlineToolbar = rootView.findViewById(R.id.online_toolbar);
        onlineToolbar.setOnClickListener(v -> checkConnectivity());
        torOutput = rootView.findViewById(R.id.status_text);
        noContacts = rootView.findViewById(R.id.no_contacts);
        recyclerView = rootView.findViewById(R.id.recycler);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        lst = new ArrayList<>();
        mAdapter = new MyRecyclerViewAdapter((DxApplication) Objects.requireNonNull(getActivity()).getApplication(),lst,this);
        recyclerView.setAdapter(mAdapter);
        ((SwipeRefreshLayout)rootView.findViewById(R.id.refresh)).setOnRefreshListener(
                () -> {
                    checkConnectivity();
                    updateUi();
                    ((SwipeRefreshLayout)rootView.findViewById(R.id.refresh)).setRefreshing(false);
                }
        );

        checkConnectivity();
//        updateUi();
//        checkMessages();
//        new Thread(()->{
//
//        })

        if(!((DxApplication)getActivity().getApplication()).isWeAsked()){
            new Thread(()->{
                try{
                    Thread.sleep(1000);
                }catch (Exception ignored){}
                try{
                    if(!((DxApplication)getActivity().getApplication()).isIgnoringBatteryOptimizations()){
                        getActivity().runOnUiThread(()-> new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
                            .setTitle(R.string.turn_off_battery)
                            .setMessage(R.string.allow_in_background)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                ((DxApplication)getActivity().getApplication()).requestBatteryOptimizationOff();
                                ((DxApplication)getActivity().getApplication()).setWeAsked(true);
                            })
                            .setNegativeButton(android.R.string.no, (dialog, whichButton)-> ((DxApplication)getActivity().getApplication()).setWeAsked(true)).show());
                    }
                }catch (Exception ignored){}
            }).start();
        }
        return rootView;
    }

    public void checkMessages(){
        if(messageChecker!=null){
            return;
        }
        messageChecker = new Thread(()->{
            while (true){
                try{
                    Thread.sleep(5000);
                    if(getActivity()==null) break;
                    List<String[]> tmp = DbHelper.getContactsList((DxApplication) (getActivity()).getApplication());
                    if(tmp==null || lst==null)break;
                    if(!Utils.arrayListEquals(lst,tmp)){
                        updateUi(tmp);
                    }
                }catch (Exception ignored){break;}
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
        new Thread(()->{
            if(getActivity()==null || mainThread==null){
                return;
            }
            try{
                lst = DbHelper.getContactsList((DxApplication) (getActivity()).getApplication());
                mainThread.post(()->{
                    try{
                        if(lst.isEmpty()){
                            noContacts.setVisibility(View.VISIBLE);
                        }else{
                            noContacts.setVisibility(View.GONE);
                        }
                        mAdapter = new MyRecyclerViewAdapter((DxApplication) getActivity().getApplication(),lst,this);
                        recyclerView.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                        recyclerView.scheduleLayoutAnimation();
                        ((DxApplication)getActivity().getApplication()).clearMessageNotification();
                    } catch (Exception ignored) {}
                });
            }catch (Exception ignored){}
        }).start();
    }

    public void updateUi(List<String[]> tmp){
        new Thread(()->{
            if(getActivity()==null || mainThread==null){
                return;
            }
            try{
                lst = tmp;
                mainThread.post(()->{
                    if(lst.isEmpty()){
                        noContacts.setVisibility(View.VISIBLE);
                    }else{
                        noContacts.setVisibility(View.GONE);
                    }
                    mAdapter = new MyRecyclerViewAdapter((DxApplication) getActivity().getApplication(),lst,this);
                    recyclerView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                    //this is because our test in the message checker always fails
                    recyclerView.scheduleLayoutAnimation();
                });
            }catch (Exception ignored){}
        }).start();
    }

    public boolean isOnline() {
        if(getActivity() !=null) {
            return TorClientSocks4.test((DxApplication) Objects.requireNonNull(getActivity()).getApplication());
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
            ColorDrawable[] color = {new ColorDrawable(Objects.requireNonNull(getContext()).getColor(R.color.startGradientColor)), new ColorDrawable(Objects.requireNonNull(getContext()).getColor(R.color.endGradientColor))};
            TransitionDrawable trans = new TransitionDrawable(color);
            onlineToolbar.setBackground(trans);
            trans.startTransition(3000); // duration 3 seconds

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
                            ColorDrawable[] color = {new ColorDrawable(Objects.requireNonNull(getContext()).getColor(R.color.endGradientColor)), new ColorDrawable(Objects.requireNonNull(getContext()).getColor(R.color.green_tor))};
                            TransitionDrawable trans = new TransitionDrawable(color);
                            onlineToolbar.setBackground(trans);
                            trans.startTransition(1500);
                        }catch (Exception ignored) {}
                    });
                    new Thread(()-> ((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).queueAllUnsentMessages()).start();
                }else{
                    mainThread.post(()->{
                        try{
                            onlineTxt.setText(R.string.offline);
//                            onlineImg.setVisibility(View.GONE);
//                            offlineImg.setVisibility(View.VISIBLE);
                            onlineToolbar.setVisibility(View.VISIBLE);
                            ColorDrawable[] color = {new ColorDrawable(Objects.requireNonNull(getContext()).getColor(R.color.endGradientColor)), new ColorDrawable(Objects.requireNonNull(getContext()).getColor(R.color.red_500))};
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
        torOutput.setText(tor_status);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.app_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_restart_tor:
                new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
                    .setTitle(R.string.restart_tor)
                    .setMessage(R.string.restart_tor_explain)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        ((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).restartTor();
                        mainThread.post(()->{
                            try{
                                onlineTxt.setText(R.string.offline);
                                onlineImg.setVisibility(View.GONE);
                                offlineImg.setVisibility(View.VISIBLE);
                                onlineToolbar.setVisibility(View.VISIBLE);
                                Intent intent = new Intent(getActivity().getApplication(), SetupInProcess.class);
                                intent.putExtra("first_time",false);
                                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                                getActivity().finish();
                            } catch (Exception ignored) {}
                        });
                    })
                    .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
                break;
            case R.id.action_shutdown:
                new AlertDialog.Builder(getContext(),R.style.AppAlertDialog)
                    .setTitle(R.string.shut_app)
                    .setMessage(R.string.shut_app_explain)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        ((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).shutdown();
                    })
                    .setNegativeButton(android.R.string.no, (dialog, whichButton)-> {} ).show();
                break;
            case R.id.action_my_identity:
                stopCheckingMessages();
                try{
                    Intent intent = new Intent(getContext(), MyIdentityActivity.class);
                    if(getContext()!=null){
                        getContext().startActivity(intent);
                    }
                }catch (Exception ignored) {}
                break;
            case R.id.action_my_profile:
                stopCheckingMessages();
                try{
                    Intent intent = new Intent(getContext(), MyProfileActivity.class);
                    if(getContext()!=null){
                        getContext().startActivity(intent);
                    }
                }catch (Exception ignored) {}
                break;
            case R.id.action_about:
                stopCheckingMessages();
                try{
                    Intent intent = new Intent(getContext(), AboutActivity.class);
                    if(getContext()!=null){
                        getContext().startActivity(intent);
                    }
                }catch (Exception ignored) {}
                break;
            case R.id.action_tips:
                stopCheckingMessages();
                try{
                    Intent intent = new Intent(getContext(), TipsActivity.class);
                    if(getContext()!=null){
                        getContext().startActivity(intent);
                    }
                }catch (Exception ignored) {}
                break;
            case R.id.action_license:
                stopCheckingMessages();
                try{
                    Intent intent = new Intent(getContext(), LicenseActivity.class);
                    if(getContext()!=null){
                        getContext().startActivity(intent);
                    }
                }catch (Exception ignored) {}
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}