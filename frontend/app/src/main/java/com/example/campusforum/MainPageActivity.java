package com.example.campusforum;
// 应用的主页面
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.example.campusforum.databinding.ActivityMainPageBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

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
            }
        });

        User.setCurrentUser();
    }
}