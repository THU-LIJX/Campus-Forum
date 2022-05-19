package com.example.campusforum;

import android.util.Log;

import androidx.annotation.NonNull;

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

public class User {
    static public User currentUser;     // 当前用户，只使用userId、subscriptions

    public int userId;
    public String username;
    public String email;
    public String avatar;
    public String description;
    public List<Integer> subscriptions;
    public int postNum;

    /**
     * @param jsonObject 后端传回来的信息
     */
    public User(JSONObject jsonObject) {
        try {
            userId = jsonObject.getInt("id");
            username = jsonObject.getString("name");
            email = jsonObject.getString("email");
            avatar = jsonObject.getString("avatar");
            description = jsonObject.getString("description");
            subscriptions = new ArrayList<>();
            JSONArray jsonArray = jsonObject.getJSONArray("subscriptions");
            for (int i = 0; i < jsonArray.length(); i++) {
                subscriptions.add(jsonArray.getInt(i));
            }
            jsonArray = jsonObject.getJSONArray("blogs");
            postNum = jsonArray.length();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 只需要在登录之后调用一下
     */
    static public void setCurrentUser() {
        HashMap<String, String> query = new HashMap<>();
        query.put("myself", "true");
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
                    currentUser = new User(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
