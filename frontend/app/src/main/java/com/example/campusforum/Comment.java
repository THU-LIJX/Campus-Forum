package com.example.campusforum;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Comment implements Serializable {
    public int commentId;
    public int userId;
    public String username;
    public String avatar;
    public String content;

    public Comment(JSONObject jsonObject) {
        try {
            commentId = jsonObject.getInt("id");
            userId = jsonObject.getInt("user");
            username = jsonObject.getString("user_name");
            avatar = jsonObject.getString("avatar");
            content = jsonObject.getString("text");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
