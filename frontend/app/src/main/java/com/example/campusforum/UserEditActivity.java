package com.example.campusforum;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.campusforum.databinding.ActivityUserBinding;
import com.example.campusforum.databinding.ActivityUserEditBinding;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.IOException;

public class UserEditActivity extends AppCompatActivity {
    private static final String TAG = "测试输出";
    private final String IMAGE_TYPE = "image/*";

    ActivityUserEditBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPermission();
        binding = ActivityUserEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.activityEditUserAvatar.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("Range")
            public void onClick(View view) {
                //TODO  这里完成上传图片的代码

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0x01);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == 0x01) {
                // Get the url of the image from data
                assert data != null;
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    Log.d(TAG, selectedImageUri.toString());
                    binding.activityUserAvatar.setImageURI(selectedImageUri);
                    // update the preview image in the layout
//                    img_avatar.setImageURI(selectedImageUri);
                }
            }
        }
    }
    // 获取应用所需要的权限
    private void getPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }


}