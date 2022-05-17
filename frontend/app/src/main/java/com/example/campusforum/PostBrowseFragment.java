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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;
import android.view.LayoutInflater;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstance) {

        List<PostAdapter.Post> postList = new ArrayList<>();
        PostAdapter postAdapter = new PostAdapter(postList, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        binding.fragPostBrowseRecyclerView.setAdapter(postAdapter);
        binding.fragPostBrowseRecyclerView.setLayoutManager(linearLayoutManager);

        postList.add(new PostAdapter.Post(1, 1, 1, "FUCK", new ArrayList<String>(), new ArrayList<Integer>(), 1));
        postList.add(new PostAdapter.Post(1, 1, 1, "FUCK", new ArrayList<String>(), new ArrayList<Integer>(), 1));
        postList.add(new PostAdapter.Post(1, 1, 1, "FUCK", new ArrayList<String>(), new ArrayList<Integer>(), 1));
        List<String> datas = new ArrayList<>();
        datas.add("/static/src/28/0.jpg");
        datas.add("/static/src/28/0.jpg");
        datas.add("/static/src/28/0.jpg");
        datas.add("/static/src/28/0.jpg");
        postList.add(new PostAdapter.Post(1, 1, 2, "FUCK", datas, new ArrayList<Integer>(), 1));
//        postList.add(new PostAdapter.Post(1, 1, 8, "FUCK", new ArrayList<String>(), new ArrayList<Integer>(), 1));
        List<String> audioData = new ArrayList<>();
        audioData.add("http://downsc.chinaz.net/Files/DownLoad/sound1/201906/11582.mp3");
        postList.add(new PostAdapter.Post(1, 1, 4, "FUCK", audioData, new ArrayList<Integer>(), 1));
        List<String> audioData2 = new ArrayList<>();
        audioData2.add("http://downsc.chinaz.net/files/download/sound1/201206/1638.mp3");
        postList.add(new PostAdapter.Post(1, 1, 4, "FUCK", audioData2, new ArrayList<Integer>(), 1));

    }

    // 获取应用所需要的权限
    private void getPermission() {
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }
}
