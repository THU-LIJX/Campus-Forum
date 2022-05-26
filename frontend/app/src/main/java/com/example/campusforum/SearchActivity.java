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
    private int currentPage = -1;
    private int pageSize = 5;
    private String sortLiked = "DES";
    private String sortTime = "DES";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this);
        linearLayoutManager = new LinearLayoutManager(this);
        binding.activitySearchRecyclerView.setAdapter(postAdapter);
        binding.activitySearchRecyclerView.setLayoutManager(linearLayoutManager);


        ArrayList<String> items = new ArrayList<>();
        //! text、image、sound、video
        items.add("文字"); //text
        items.add("图像"); // image
        items.add("语音"); // sound
        items.add("视频"); // video
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.serach_list_item, items);
        binding.searchSelect.setAdapter(adapter);
        setContentView(binding.getRoot());

        binding.activitySearchRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastVisibleItemPosition();
                    if (lastPosition == postList.size() - 1) {
                        getSearchResult();
                    }
                }
            }
        });


        binding.btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSearchResult();
            }
        });
    }

    private void getSearchResult() {
        String searchContent = Objects.requireNonNull(binding.searchKeyWord.getText()).toString();
                String searchUserNameContent = binding.searchUserName.getText().toString();
                String searchTypeContent = binding.searchSelect.getText().toString();
                Log.d(TAG, "onClick: "+ searchTypeContent);
                HashMap<String, String> data = new HashMap<>();
                data.put("content", searchContent);
                data.put("username", searchUserNameContent);
                switch (searchTypeContent) {
                    case "文字":
                        data.put("type", "text");
                        Log.d(TAG, "传入参数：" + searchContent + "," + searchUserNameContent + ",text");
                        break;
                    case "图像":
                        data.put("type", "image");
                        Log.d(TAG, "传入参数：" + searchContent + "," + searchUserNameContent + ",image");
                        break;
                    case "语音":
                        data.put("type", "sound");
                        Log.d(TAG, "传入参数：" + searchContent + "," + searchUserNameContent + ",？？？sound");
                        break;
                    case "视频":
                        data.put("type", "video");
                        Log.d(TAG, "传入参数：" + searchContent + "," + searchUserNameContent + ",video");
                        break;
                    default:
                        data.put("type", "photo?");
                        Log.d(TAG, "getSearchResult: 进入搜索界面");
                        break;
                }

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


//    private showMenu(View v, ) {

//}
}

