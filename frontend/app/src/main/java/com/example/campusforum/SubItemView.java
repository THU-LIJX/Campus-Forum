package com.example.campusforum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SubItemView extends LinearLayoutCompat {
    // 在java代码里new时会用到
    public SubItemView(Context context) {
        this(context, null);
    }

    // 在xml布局文件中使用时自动调用
    public SubItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // 不会自动调用，如果有默认style时，在第二个构造函数中调用
    public SubItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private TextView username;                         // 用户名
    private TextView userId;                           // 用户id
    private TextView description;                      // 用户描述
    private RoundedImageView avatar;                   // 头像

    private Activity activity;                         // view所在的activity

    int USER_ID;

    private void initView(Context context) {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setLayoutDirection(VERTICAL);
        this.setLayoutParams(layoutParams);
        View view = LayoutInflater.from(context).inflate(R.layout.sub_item, this);

        username = (TextView) view.findViewById(R.id.sub_item_user_name);
        userId = (TextView) view.findViewById(R.id.sub_item_user_id);
        description = (TextView) view.findViewById(R.id.sub_item_description);
        avatar = (RoundedImageView) view.findViewById(R.id.sub_item_avatar);
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setSub(int id) {
        USER_ID = id;
        avatar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, UserActivity.class);
                intent.putExtra("id", USER_ID);
                activity.startActivity(intent);
            }
        });


        HashMap<String, String> query = new HashMap<>();
        query.put("id", Integer.toString(id));
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
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            username.setText(user.username);
                            userId.setText(Integer.toString(user.userId));
                            description.setText(user.description);
                            if (user.avatar.equals("")) {
                                avatar.setImageResource(R.drawable.ranga);
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
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                avatar.setImageBitmap(bitmap);
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
