package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.example.campusforum.databinding.ActivityMainPageBinding;
import com.example.campusforum.databinding.ActivitySearchBinding;

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

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "FUCK";
    ActivitySearchBinding binding;
    private List<PostAdapter.Post> postList;
    private PostAdapter postAdapter;
    private LinearLayoutManager linearLayoutManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this);
        linearLayoutManager = new LinearLayoutManager(this);
        binding.activitySearchRecyclerView.setAdapter(postAdapter);
        binding.activitySearchRecyclerView.setLayoutManager(linearLayoutManager);

        ArrayList<String> items = new ArrayList<>();
        ArrayList<String> items_kind = new ArrayList<>();
        //! text、image、sound、video
        items.add("文字"); //text
        items.add("图像"); // image
        items.add("语音"); // sound
        items.add("视频"); // video
        //! title, content
        items_kind.add("动态标题");
        items_kind.add("动态内容");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.serach_list_item, items);
        ArrayAdapter<String> adapter_kind = new ArrayAdapter<String>(this, R.layout.serach_list_item, items_kind);
        binding.searchSelect.setAdapter(adapter);
        binding.searchSelectTitleOrContent.setAdapter(adapter_kind);


        binding.btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postList.clear();
                postAdapter.notifyDataSetChanged();
                getSearchResult();
            }
        });
    }

    private void getSearchResult() {
        String searchContent = Objects.requireNonNull(binding.searchKeyWord.getText()).toString();
        String searchUserNameContent = binding.searchUserName.getText().toString();
        String searchTypeContent = binding.searchSelect.getText().toString();
        String searchKeyWordType = binding.searchSelectTitleOrContent.getText().toString();
        String searchTypeContent_Convert;
        //! 发送四个包， type, username, content, title
        HashMap<String, String> data = new HashMap<>();
        data.put("user_name", searchUserNameContent);
        switch (searchTypeContent) {
            case "文字":
                searchTypeContent_Convert = "text";
                break;
            case "图像":
                searchTypeContent_Convert = "image";
                break;
            case "语音":
                searchTypeContent_Convert = "voice";
                break;
            case "视频":
                searchTypeContent_Convert = "video";
                break;
            default:
                searchTypeContent_Convert = "";
                break;
        }
        data.put("type", searchTypeContent_Convert);
        switch (searchKeyWordType) {
            case "动态标题":
                data.put("title", searchContent);
                data.put("content", "");
                Log.d(TAG, "user_name:" + searchUserNameContent + ";type" + searchTypeContent_Convert + ";title:" + searchContent + ";content:" + "null");
                break;
            case "动态内容":
                data.put("content", searchContent);
                data.put("title", "");
                Log.d(TAG, "username:" + searchUserNameContent + ";type" + searchTypeContent_Convert + ";title:" + "null" + ";content:" + searchContent);
                break;
            default:
                break;

        }

        System.out.println("anchor");
        System.out.println(data);

       HttpUtil.sendGetRequest("/api/user/search", data, new Callback() {
           @Override
           public void onFailure(@NonNull Call call, @NonNull IOException e) {
               e.printStackTrace();
           }

           @Override
           public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String result = Objects.requireNonNull(response.body()).string();
                    JSONObject jsonObject = new JSONObject(result);
                    Log.d("result", jsonObject.toString());
                    if (jsonObject.getString("message").equals("ok")) {
                        JSONArray jsonArray= jsonObject.getJSONArray("blogs");
//                                Log.d(TAG, jsonArray.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    System.out.println("Post Length" + jsonArray.length());
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

