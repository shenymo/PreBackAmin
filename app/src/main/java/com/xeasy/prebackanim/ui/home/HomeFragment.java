package com.xeasy.prebackanim.ui.home;

import static com.xeasy.prebackanim.dao.BlackListDao.configVer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xeasy.prebackanim.R;
import com.xeasy.prebackanim.dao.BlackListDao;
import com.xeasy.prebackanim.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private ExpandableAdapter adapter;
    private FragmentHomeBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

//        final TextView textView = binding.textHome;
//        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Long thisVer = configVer;

    @Override
    public void onStart() {
        super.onStart();

        if ( adapter == null || !Objects.equals(configVer, thisVer)) {
            thisVer = configVer;
            inflationList();
        }
    }

    public void inflationList() {
        Map<String, List<String>> data = new HashMap<>();
//            data.put("Group A", Arrays.asList("Item 1", "Item 2"));
        Map<String, BlackListDao.BlackList4App> config = BlackListDao.getConfig(getContext());
        PackageManager packageManager = getContext().getPackageManager();
        config.forEach((k,v) -> {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(k, 0);
                String appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
                data.put(appName + ": " + k, new ArrayList<>(v.activityNameMap.keySet()));
            } catch (Exception e) {
                Log.d(this.getClass().getName(), "初始化config错误!");
            }
        });

        adapter = new ExpandableAdapter(data);

        RecyclerView recyclerView = getActivity().findViewById(R.id.main_recyclerView);
        // 在 RecyclerView 初始化时添加
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setAddDuration(150L);
        defaultItemAnimator.setRemoveDuration(150L);
        recyclerView.setItemAnimator(defaultItemAnimator);
//            recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    public ExpandableAdapter getAdapter() {
        return adapter;
    }
}