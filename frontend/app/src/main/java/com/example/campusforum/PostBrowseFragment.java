package com.example.campusforum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.campusforum.databinding.FragmentPostBrowseBinding;
import com.google.android.material.imageview.ShapeableImageView;
import com.previewlibrary.GPreviewBuilder;
import com.previewlibrary.ZoomMediaLoader;
import com.previewlibrary.enitity.IThumbViewInfo;
import com.previewlibrary.loader.IZoomMediaLoader;
import com.previewlibrary.loader.MySimpleTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.xml.transform.Result;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PostBrowseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PostBrowseFragment extends Fragment {

    private FragmentPostBrowseBinding binding;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PostBrowseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PostBrowseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PostBrowseFragment newInstance(String param1, String param2) {
        PostBrowseFragment fragment = new PostBrowseFragment();
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
        binding = FragmentPostBrowseBinding.inflate(inflater, container, false);
        ZoomMediaLoader.getInstance().init(new TestImageLoader());
        return binding.getRoot();
    }

    private List<PostAdapter.Post> postList;
    private PostAdapter postAdapter;
    private LinearLayoutManager linearLayoutManager;

    private int currentPage = -1;
    private int pageSize = 10;
    private String sortLiked = "DES";
    private String sortTime = "DES";
    private boolean subscribed = true;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {
        getPermission();
        // recyclerview滑到底部时加载更多动态
        binding.fragPostBrowseRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                    if (lastPosition == postList.size() - 1) {
                        getPost();
                    }
                }
            }
        });

        // 菜单设置
        binding.fragmentPostBrowseTopBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {
                    case R.id.global_search:
                        intent = new Intent(getActivity(), SearchActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.time_asc:
                        sortTime = "ASC";
                        break;
                    case R.id.time_des:
                        sortTime = "DES";
                        break;
                    case R.id.like_asc:
                        sortLiked = "ASC";
                        break;
                    case R.id.like_des:
                        sortLiked = "DES";
                        break;
                    case R.id.all:
                        subscribed = false;
                        break;
                    case R.id.subscribe:
                        subscribed = true;
                        break;
                }
                init();
                return false;
            }
        });

        // 初始化页面
        init();
    }

    // 是否第一次调用onResume
    private boolean firstCall = true;
    @Override
    public void onResume() {
        super.onResume();
        if (firstCall) {
            firstCall = false;
        } else {
            init();
        }
    }


    private void init() {
        currentPage = -1;
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this.getActivity());
        linearLayoutManager = new LinearLayoutManager(getContext());
        binding.fragPostBrowseRecyclerView.setAdapter(postAdapter);
        binding.fragPostBrowseRecyclerView.setLayoutManager(linearLayoutManager);
        getPost();
    }

    private void getPost() {
        HashMap<String, String> query = new HashMap<>();
        query.put("pagesize", Integer.toString(pageSize));
        query.put("page", Integer.toString(currentPage+1));
        query.put("sort_time", sortTime);
        query.put("sort_liked", sortLiked);
        // 和后端约定的接口
        if (subscribed) {
            query.put("subscribed", Boolean.toString(subscribed)); // 显示自己和关注的人的动态
        }
        HttpUtil.sendGetRequest("/api/user/blogs", query, new Callback() {
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
                        // 如果没有内容，blogs字段为null
                        JSONArray jsonArray = jsonObject.getJSONArray("blogs");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    System.out.println("Post Length" + jsonArray.length());
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        postList.add(new PostAdapter.Post(jsonArray.getJSONObject(i)));
                                        postAdapter.notifyItemInserted(postList.size()-1);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        currentPage += 1;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        binding.fragmentPostBrowseTopBar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), UserActivity.class);
                intent.putExtra("id", User.currentUser.userId);
                startActivity(intent);
            }
        });
    }

    // 获取应用所需要的权限
    private void getPermission() {
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }
}
