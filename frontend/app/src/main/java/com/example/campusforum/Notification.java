package com.example.campusforum;

import org.json.JSONException;
import org.json.JSONObject;

public class Notification {
    public static final String COMMENT = "comment";
    public static final String LIKE = "like";
    public static final String POST = "post";

    public int notiId;
    public int userId;
    public int postId;
    public String username;
    public String avatar;
    public String action;

    public Notification(JSONObject jsonObject) {
        try {
            notiId = jsonObject.getInt("id");
            userId = jsonObject.getInt("user");
            postId = jsonObject.getInt("blog");
            username = jsonObject.getString("user_name");
            avatar = jsonObject.getString("avatar");
            action = jsonObject.getString("action");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
