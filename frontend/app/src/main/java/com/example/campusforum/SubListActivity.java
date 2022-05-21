package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;

import com.example.campusforum.databinding.ActivitySubListBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SubListActivity extends AppCompatActivity {
    ActivitySubListBinding binding;

    public static String LIKE_LIST = "LIKE_LIST";   // 点赞列表
    public static String SUB_LIST = "SUB_LIST";     // 关注列表

    private List<Integer> subList;
    private SubAdapter subAdapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 导航按钮
        binding.activitySubListActionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        subList = new ArrayList<>();
        subAdapter = new SubAdapter(subList, this);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        binding.activitySubListRecyclerview.setAdapter(subAdapter);
        binding.activitySubListRecyclerview.setLayoutManager(linearLayoutManager);

        Bundle bundle = getIntent().getExtras();
        if (bundle.getString("type").equals(SUB_LIST)) {
            binding.activitySubListActionbar.setTitle("关注列表");
            int userId = bundle.getInt("id");
            HashMap<String, String> query = new HashMap<>();
            query.put("id", Integer.toString(userId));
            // 获取关注列表
            HttpUtil.sendGetRequest("/api/info", query, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String result = Objects.requireNonNull(response.body()).string();
                        JSONObject jsonObject = new JSONObject(result);
                        User user = new User(jsonObject);
                        SubListActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < user.subscriptions.size(); i++) {
                                    subList.add(user.subscriptions.get(i));
                                    subAdapter.notifyItemInserted(i);
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else if (bundle.getString("type").equals(LIKE_LIST)) {
            binding.activitySubListActionbar.setTitle("点赞列表");
            PostAdapter.Post post = (PostAdapter.Post) bundle.getSerializable("post");
            for (int i = 0; i < post.likedBy.size(); i++) {
                subList.add(post.likedBy.get(i));
                subAdapter.notifyItemInserted(i);
            }
        }
    }
}