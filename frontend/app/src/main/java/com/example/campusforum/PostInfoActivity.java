package com.example.campusforum;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.campusforum.databinding.ActivityPostInfoBinding;

public class PostInfoActivity extends AppCompatActivity {
    ActivityPostInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle bundle = getIntent().getExtras();
        PostAdapter.Post post = (PostAdapter.Post) bundle.getSerializable("post");

        binding.activityPostInfoPost.setActivity(this);
        binding.activityPostInfoPost.setPost(post);

    }
}