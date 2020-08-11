package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Objects;

public class StartTorFragment extends Fragment {

    private BroadcastReceiver mMyBroadcastReceiver;
    private View rootView;
    private TextView statusText;

    public StartTorFragment() {
        // Required empty public constructor
    }

    public static StartTorFragment newInstance() {
        StartTorFragment fragment = new StartTorFragment();
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
        rootView = inflater.inflate(R.layout.fragment_start_tor, container, false);
        statusText = rootView.findViewById(R.id.status_text);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if(intent.getStringExtra("tor_status")!=null){
                    updateUi(Objects.requireNonNull(intent.getStringExtra("tor_status")));
                }
            }
        };
        try {
            LocalBroadcastManager.getInstance(rootView.getContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
        mMyBroadcastReceiver = null;
        rootView = null;
        statusText = null;
        super.onDestroyView();
    }

    public void updateUi(String torStatus){
        if(torStatus.contains("ALL GOOD")){
            LocalBroadcastManager.getInstance(rootView.getContext()).unregisterReceiver(mMyBroadcastReceiver);
            ((AppActivity) Objects.requireNonNull(getActivity())).showNextFragment(new AppFragment());
        }else{
            try {
                statusText.setText(torStatus);
            }catch (Exception ignored){}
        }
    }
}