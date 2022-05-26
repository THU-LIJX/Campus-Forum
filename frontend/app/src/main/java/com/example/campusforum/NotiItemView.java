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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

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

public class NotiItemView extends LinearLayoutCompat {
    // 在java代码里new时会用到
    public NotiItemView(Context context) {
        this(context, null);
    }

    // 在xml布局文件中使用时自动调用
    public NotiItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // 不会自动调用，如果有默认style时，在第二个构造函数中调用
    public NotiItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private TextView username;                         // 用户名
    private TextView userId;                           // 用户id
    private TextView state;                            // 通知状态（是否已读）
    private TextView action;                           // 行为
    private RoundedImageView avatar;                   // 头像

    private LinearLayoutCompat mainContent;            // 卡片主体

    private Activity activity;                         // view所在的activity

    int USER_ID;

    private void initView(Context context) {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setLayoutDirection(VERTICAL);
        this.setLayoutParams(layoutParams);
        View view = LayoutInflater.from(context).inflate(R.layout.noti_item, this);

        username = (TextView) view.findViewById(R.id.noti_item_user_name);
        userId = (TextView) view.findViewById(R.id.noti_item_user_id);
        state = (TextView) view.findViewById(R.id.noti_item_state);
        action = (TextView) view.findViewById(R.id.noti_item_action);
        avatar = (RoundedImageView) view.findViewById(R.id.noti_item_avatar);
        mainContent = (LinearLayoutCompat) view.findViewById(R.id.noti_item_main_content);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setNoti(Notification notification) {
        avatar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, UserActivity.class);
                intent.putExtra("id", notification.userId);
                activity.startActivity(intent);
            }
        });

        username.setText(notification.username);
        userId.setText(Integer.toString(notification.userId));
        state.setText("READ");
        switch (notification.action) {
            case Notification.COMMENT:
                action.setText(notification.username + "评论了动态" + notification.postId);
                break;
            case Notification.LIKE:
                action.setText(notification.username + "点赞了动态" + notification.postId);
                break;
            case Notification.POST:
                action.setText(notification.username + "发布了动态" + notification.postId);
                break;
        }

        mainContent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                HttpUtil.sendGetRequest("/api/blog/" + notification.postId, null, new Callback() {
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
                                PostAdapter.Post post = new PostAdapter.Post(jsonObject.getJSONObject("blog"));
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent(activity, PostInfoActivity.class);
                                        intent.putExtra("post", post);
                                        activity.startActivity(intent);
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void setState(Boolean read) {
        if (read) {
            state.setText("READ");
        } else {
            state.setText("UNREAD");
        }
    }
}
