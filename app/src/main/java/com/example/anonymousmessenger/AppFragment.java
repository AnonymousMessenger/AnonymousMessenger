package com.example.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.anonymousmessenger.db.DbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class AppFragment extends Fragment {
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    public Handler mainThread = new Handler(Looper.getMainLooper());

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
        final View rootView = inflater.inflate(R.layout.fragment_app, container, false);
        if(!((DxApplication) Objects.requireNonNull(getActivity()).getApplication()).getServerReady()){
            if (((DxApplication) getActivity().getApplication()).getTorThread() != null) {
                ((DxApplication) getActivity().getApplication()).setTorThread(null);
            }
            ((DxApplication)getActivity().getApplication()).startTor();
        }
        FloatingActionButton btnAddContact = rootView.findViewById(R.id.btn_add_contact);
        btnAddContact.setOnClickListener(v -> ((AppActivity)getActivity()).showNextFragment(new AddContactFragment()));

        recyclerView = rootView.findViewById(R.id.recycler);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MyRecyclerViewAdapter(rootView.getContext(),new ArrayList<>());
        recyclerView.setAdapter(mAdapter);

        new Thread(()->{
            List<String[]> lst = DbHelper.getContactsList((DxApplication) getActivity().getApplication());
            mainThread.post(()->{
                mAdapter = new MyRecyclerViewAdapter(rootView.getContext(),lst);
                mAdapter.setClickListener((view, position) -> {
                    Intent intent = new Intent(getActivity(), MessageListActivity.class);
                    intent.putExtra("nickname",mAdapter.getItem(position)[0]);
                    intent.putExtra("address",mAdapter.getItem(position)[1]);
                    startActivity(intent);
                });
                recyclerView.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            });
        }).start();
        return rootView;
    }
}