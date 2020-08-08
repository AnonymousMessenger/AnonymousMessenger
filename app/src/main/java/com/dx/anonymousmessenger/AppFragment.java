package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.tor.TorClientSocks4;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AppFragment extends Fragment {
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    public Handler mainThread = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mMyBroadcastReceiver;
    private View rootView;
    private ImageView onlineImg;
    private ImageView offlineImg;
    private TextView onlineTxt;
    private Toolbar onlineToolbar;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_app, container, false);

        FloatingActionButton btnAddContact = rootView.findViewById(R.id.btn_add_contact);
        btnAddContact.setOnClickListener(v -> ((AppActivity) Objects.requireNonNull(getActivity())).showNextFragment(new AddContactFragment()));

        onlineImg = rootView.findViewById(R.id.online_image);
        offlineImg = rootView.findViewById(R.id.offline_image);
        onlineTxt = rootView.findViewById(R.id.online_text);
        onlineToolbar = rootView.findViewById(R.id.online_toolbar);
        onlineToolbar.setOnClickListener(v -> checkConnectivity());
        recyclerView = rootView.findViewById(R.id.recycler);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MyRecyclerViewAdapter(rootView.getContext(),new ArrayList<>());
        recyclerView.setAdapter(mAdapter);
        updateUi();
        return rootView;
    }

    public void updateUi(){
        new Thread(()->{
            List<String[]> lst = DbHelper.getContactsList((DxApplication) Objects.requireNonNull(getActivity()).getApplication());
            mainThread.post(()->{
                mAdapter = new MyRecyclerViewAdapter(rootView.getContext(),lst);
                recyclerView.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            });
            checkConnectivity();
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
        new Thread(()->{
            if(!((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).getServerReady()){
                if (((DxApplication) getActivity().getApplication()).getTorThread() != null) {
                    ((DxApplication) getActivity().getApplication()).setTorThread(null);
                }
                ((DxApplication)getActivity().getApplication()).startTor();
            }
        }).start();
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                updateUi();
            }
        };
        try {
            LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("your_action"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
    }

    public void checkConnectivity(){
        mainThread.post(()->{
            onlineImg.setVisibility(View.GONE);
            offlineImg.setVisibility(View.GONE);
            onlineTxt.setText("Checking");
            onlineToolbar.setVisibility(View.VISIBLE);
        });
        new Thread(()->{
            boolean online = new TorClientSocks4().test((DxApplication) Objects.requireNonNull(getActivity()).getApplication());
            if(online){
                mainThread.post(()->{
                    onlineTxt.setText("Online");
                    onlineImg.setVisibility(View.VISIBLE);
                    offlineImg.setVisibility(View.GONE);
                    onlineToolbar.setVisibility(View.VISIBLE);

                });
            }else{
                mainThread.post(()->{
                    onlineTxt.setText("Offline");
                    onlineImg.setVisibility(View.GONE);
                    offlineImg.setVisibility(View.VISIBLE);
                    onlineToolbar.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }
}