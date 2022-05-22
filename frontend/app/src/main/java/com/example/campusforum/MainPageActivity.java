package com.example.campusforum;
// 应用的主页面
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.campusforum.databinding.ActivityMainPageBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainPageActivity extends AppCompatActivity {
    ActivityMainPageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_page_nav_host);
        NavController navController = navHostFragment.getNavController();
        // 将BottomNavigationView与NavController绑定
        NavigationUI.setupWithNavController(binding.mainPageBtmNavi, navController);

        binding.mainPageToPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainPageActivity.this,PostEditActivity.class);
                startActivity(intent);
                //测试subscribe
//                HashMap<String,String>data=new HashMap<>();
//                data.put("id",Integer.toString(54));
//                HttpUtil.sendPostRequest("/api/user/subscribe", data, new Callback() {
//                    @Override
//                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
//
//                    }
//
//                    @Override
//                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                        try {
//                            String res= Objects.requireNonNull(response.body()).string();
//                            Log.d("Test", "onResponse: "+res);
//                            JSONObject jsonObject=new JSONObject(res);
//                            Log.d("Test", "onResponse: message"+jsonObject.getString("message"));
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
            }
        });

        User.setCurrentUser();
    }
}