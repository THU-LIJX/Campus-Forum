package com.example.campusforum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.makeramen.roundedimageview.RoundedImageView;

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

public class CommentItemView extends LinearLayoutCompat {
    // 在java代码里new时会用到
    public CommentItemView(Context context) {
        this(context, null);
    }

    // 在xml布局文件中使用时自动调用
    public CommentItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // 不会自动调用，如果有默认style时，在第二个构造函数中调用
    public CommentItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private Comment comment;                           // 评论

    private TextView username;                         // 用户名
    private TextView userId;                           // 用户id
    private TextView content;                          // 评论内容
    private RoundedImageView avatar;                   // 头像
    private ShapeableImageView deleteIcon;             // 删除图标

    private Activity activity;                         // view所在的activity
    private CommentAdapter commentAdapter;             // view所在的adapter

    private void initView(Context context) {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setLayoutDirection(VERTICAL);
        this.setLayoutParams(layoutParams);
        View view = LayoutInflater.from(context).inflate(R.layout.comment_item, this);

        username = (TextView) view.findViewById(R.id.comment_item_user_name);
        userId = (TextView) view.findViewById(R.id.comment_item_user_id);
        content = (TextView) view.findViewById(R.id.comment_item_content);
        avatar = (RoundedImageView) view.findViewById(R.id.comment_item_avatar);
        deleteIcon = (ShapeableImageView) view.findViewById(R.id.comment_item_delete);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setCommentAdapter(CommentAdapter adapter) {
        commentAdapter = adapter;
    }

    public void setComment(Comment comment) {
        this.comment = comment;

        avatar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, UserActivity.class);
                intent.putExtra("id", comment.userId);
                activity.startActivity(intent);
            }
        });

        username.setText(comment.username);
        userId.setText("#" + Integer.toString(comment.userId));
        content.setText(comment.content);
        if (comment.avatar.equals("")) {
            avatar.setImageResource(R.drawable.ranga);
        } else {
            // 获取头像并设置
            HttpUtil.sendGetRequest(comment.avatar, null, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    InputStream inputStream = response.body().byteStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            avatar.setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }

        if (comment.userId == User.currentUser.userId) {
            deleteIcon.setVisibility(VISIBLE);
            deleteIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    HashMap<String, String> data = new HashMap<>();
                    data.put("id", Integer.toString(comment.commentId));
                    HttpUtil.sendPostRequest("/api/user/delcomment", data, new Callback() {
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
                                    Log.d("delete comment", "ok");
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
            });
        } else {
            deleteIcon.setVisibility(GONE);
        }
    }

}
