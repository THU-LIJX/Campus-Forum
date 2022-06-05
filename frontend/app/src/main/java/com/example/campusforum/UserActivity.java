package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.campusforum.databinding.ActivityUserBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

// 要跳转到个人主页，必须传用户id
public class UserActivity extends AppCompatActivity {
    ActivityUserBinding binding;

    User user;
    int userId;
    private int currentPage;
    private int pageSize = 5;
    private String sortLiked = "DES";
    private String sortTime = "DES";

    private List<PostAdapter.Post> postList;
    private PostAdapter postAdapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 导航按钮
        binding.activityUserActionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // recyclerview滑到底部时加载更多动态
        binding.activityUserPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        // 点击关注的人显示关注列表
        binding.activityUserSubscriptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserActivity.this, SubListActivity.class);
                intent.putExtra("type", SubListActivity.SUB_LIST);
                intent.putExtra("id", userId);
                startActivity(intent);
            }
        });

        binding.activityUserActionbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.block && userId != User.currentUser.userId) {
                    HashMap<String, String> data = new HashMap<>();
                    data.put("id", Integer.toString(userId));
                    HttpUtil.sendPostRequest("/api/user/block", data, new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            try {
                                String result = Objects.requireNonNull(response.body()).string();
                                JSONObject jsonObject = new JSONObject(result);
                                Log.d("block", "ok");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                return false;
            }
        });

        // 获取用户信息并填充
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        currentPage = -1;
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        binding.activityUserPosts.setAdapter(postAdapter);
        binding.activityUserPosts.setLayoutManager(linearLayoutManager);

        Bundle data = getIntent().getExtras();
        userId = data.getInt("id");
        HashMap<String, String> query = new HashMap<>();
        query.put("id", Integer.toString(userId));
        HttpUtil.sendGetRequest("/api/info", query, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    user = new User(jsonObject);
                    // 初始化界面
                    UserActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.activityUserUsername.setText(user.username);
                            binding.activityUserUserid.setText("@" + Integer.toString(user.userId));
                            binding.activityUserUserDescription.setText(user.description);
                            binding.activityUserSubscriptions.setText("已关注"+ user.subscriptions.size() + "人");
                            // 如果用户没设置，使用默认头像
                            if (user.avatar.equals("")) {
                                binding.activityUserAvatar.setImageResource(R.drawable.ranga);
                            } else {
                                // 获取头像并设置
                                HttpUtil.sendGetRequest(user.avatar, null, new Callback() {
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                        InputStream inputStream = response.body().byteStream();
                                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                        UserActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                binding.activityUserAvatar.setImageBitmap(bitmap);
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });

                    // 获取用户的动态
                    getPost();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // 用户查看自己的个人主页
        if (userId == User.currentUser.userId) {
            // 显示edit profile按钮
            binding.activityUserFuncBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(UserActivity.this, UserEditActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            // 显示follow、followed按钮
            System.out.println(User.currentUser.subscriptions);
            if (User.currentUser.subscriptions.contains(userId)) {
                onFollowClick();
            } else {
                onFollowedClick();
            }
        }
    }

    // 关注
    private void onFollowClick() {
        binding.activityUserFuncBtn.setText("取关");
        binding.activityUserFuncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFollowedClick();
            }
        });
        // 不需要重复关注
        if (!User.currentUser.subscriptions.contains(userId)) {
            HashMap<String, String> data = new HashMap<>();
            data.put("id", Integer.toString(userId));
            HttpUtil.sendPostRequest("/api/user/subscribe", data, new Callback() {
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
                            User.currentUser.subscriptions.add(userId);
                            System.out.println(User.currentUser.subscriptions);
                            Log.d("fuck", "ok");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    init();
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

    // 取消关注
    private void onFollowedClick() {
        binding.activityUserFuncBtn.setText("关注");
        binding.activityUserFuncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFollowClick();
            }
        });
        // 不需要重复取消关注
        if (User.currentUser.subscriptions.contains(userId)) {
            HashMap<String, String> data = new HashMap<>();
            data.put("id", Integer.toString(userId));
            HttpUtil.sendPostRequest("/api/user/unsubscribe", data, new Callback() {
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
                            User.currentUser.subscriptions.remove(Integer.valueOf(userId));
                            System.out.println(User.currentUser.subscriptions);
                            Log.d("fuck", "ok");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    init();
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

    // 获取动态
    private void getPost() {
        if ((currentPage + 1) * pageSize < user.postNum) {
            currentPage += 1;
            HashMap<String, String> query = new HashMap<>();
            query.put("pagesize", Integer.toString(pageSize));
            query.put("page", Integer.toString(currentPage));
            query.put("sort_time", sortTime);
            query.put("sort_liked", sortLiked);
            query.put("user", Integer.toString(userId));
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
                            JSONArray jsonArray = jsonObject.getJSONArray("blogs");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            postList.add(new PostAdapter.Post(jsonArray.getJSONObject(i)));
                                            postAdapter.notifyItemInserted(postList.size()-1);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
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
}