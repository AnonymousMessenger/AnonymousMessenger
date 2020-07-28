package com.example.anonymousmessenger;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.anonymousmessenger.db.DbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


public class AppFragment extends Fragment {
    private RecyclerView recyclerView;
    private MyRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

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
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_app, container, false);
        if(!((DxApplication)getActivity().getApplication()).getServerReady()){
            if (((DxApplication) getActivity().getApplication()).getTorThread() != null) {
                ((DxApplication) getActivity().getApplication()).setTorThread(null);
            }
            ((DxApplication)getActivity().getApplication()).startTor();
        }
        FloatingActionButton btnAddContact = (FloatingActionButton) rootView.findViewById(R.id.btn_add_contact);
        btnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AppActivity)getActivity()).showNextFragment(new AddContactFragment());
            }
        });
        //get contacts from db
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler);
//        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        List<String[]> lst = DbHelper.getContactsList((DxApplication) getActivity().getApplication());
        mAdapter = new MyRecyclerViewAdapter(rootView.getContext(),lst);
        mAdapter.setClickListener(new MyRecyclerViewAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Log.d("Shit's been clicked",mAdapter.getItem(position)[0].equals("")?
                        mAdapter.getItem(position)[1]:mAdapter.getItem(position)[0]);
                Intent intent = new Intent(getActivity(), MessageListActivity.class);
                intent.putExtra("nickname",mAdapter.getItem(position)[0]);
                intent.putExtra("address",mAdapter.getItem(position)[1]);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(mAdapter);
        return rootView;
    }
}