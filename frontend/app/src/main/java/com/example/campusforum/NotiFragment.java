package com.example.campusforum;

import static com.luck.picture.lib.thread.PictureThreadUtils.runOnUiThread;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.campusforum.databinding.FragmentNotiBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NotiFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NotiFragment extends Fragment {
    private FragmentNotiBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NotiFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NotiFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NotiFragment newInstance(String param1, String param2) {
        NotiFragment fragment = new NotiFragment();
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
        // Inflate the layout for this fragment
        binding = FragmentNotiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    private List<Notification> notificationList;
    private NotiAdapter notiAdapter;
    private LinearLayoutManager linearLayoutManager;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {

        notificationList = new ArrayList<>();
        notiAdapter = new NotiAdapter(notificationList, this.getActivity());
        linearLayoutManager = new LinearLayoutManager(getContext());
        binding.fragmentNotiRecyclerview.setAdapter(notiAdapter);
        binding.fragmentNotiRecyclerview.setLayoutManager(linearLayoutManager);

        // 获取通知
        HttpUtil.sendGetRequest("/api/user/notices", null, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String result = Objects.requireNonNull(response.body()).string();
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.getString("message").equals("ok")) {
                        int commentNum = jsonObject.getInt("comment");
                        int likeNum = jsonObject.getInt("like");
                        int postNum = jsonObject.getInt("post");
                        int unreadNum = commentNum + likeNum + postNum;
                        notiAdapter.setUnreadNum(unreadNum);
                        JSONArray jsonArray = jsonObject.getJSONArray("notices");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            notificationList.add(new Notification(jsonArray.getJSONObject(i)));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notiAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}