package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.campusforum.databinding.ActivityNotiBinding;

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

public class NotiActivity extends AppCompatActivity {
    ActivityNotiBinding binding;

    private List<Notification> notificationList;
    private NotiAdapter notiAdapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        notificationList = new ArrayList<>();
        notiAdapter = new NotiAdapter(notificationList, this);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        binding.activityNotiRecyclerview.setAdapter(notiAdapter);
        binding.activityNotiRecyclerview.setLayoutManager(linearLayoutManager);

        // 导航按钮
        binding.activityNotiActionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

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