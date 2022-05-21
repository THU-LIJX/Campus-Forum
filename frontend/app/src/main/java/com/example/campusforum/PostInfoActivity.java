package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.campusforum.databinding.ActivityPostInfoBinding;

import org.json.JSONArray;
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

public class PostInfoActivity extends AppCompatActivity {
    ActivityPostInfoBinding binding;

    private CommentAdapter commentAdapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle bundle = getIntent().getExtras();
        PostAdapter.Post post = (PostAdapter.Post) bundle.getSerializable("post");

        binding.activityPostInfoPost.setActivity(this);
        binding.activityPostInfoPost.setPost(post);

        commentAdapter = new CommentAdapter(post.comments, this, binding.activityPostInfoPost, post);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        binding.activityPostInfoComments.setAdapter(commentAdapter);
        binding.activityPostInfoComments.setLayoutManager(linearLayoutManager);

        // 跳转到点赞列表
        binding.activityPostInfoLikes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PostInfoActivity.this, SubListActivity.class);
                intent.putExtra("type", SubListActivity.LIKE_LIST);
                intent.putExtra("post", post);
                startActivity(intent);
            }
        });

        binding.activityPostInfoBtnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = binding.activityPostInfoEdtComment.getText().toString();
                binding.activityPostInfoEdtComment.setText("");
                if (content.equals("")) {
                    Toast.makeText(getApplicationContext(), "评论不能为空", Toast.LENGTH_SHORT);
                } else {
                    HashMap<String, String> data = new HashMap<>();
                    data.put("blog", Integer.toString(post.postId));
                    data.put("text", content);
                    HttpUtil.sendPostRequest("/api/user/comment", data, new Callback() {
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
                                    Log.d("comment", "ok");
                                    JSONArray jsonArray = jsonObject.getJSONArray("comments");
                                    List<Comment> commentList = new ArrayList<>();
                                    for (int i = 0; i < jsonArray.length(); i++) commentList.add(new Comment(jsonArray.getJSONObject(i)));
                                    commentAdapter.setCommentList(commentList);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }
}