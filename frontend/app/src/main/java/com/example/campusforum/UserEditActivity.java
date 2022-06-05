package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.campusforum.databinding.ActivityUserBinding;
import com.example.campusforum.databinding.ActivityUserEditBinding;
import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserEditActivity extends AppCompatActivity {
    private static final String TAG = "测试输出";
    private final String IMAGE_TYPE = "image/*";
    private User user;
    private Uri selectedImageUri;

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
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0x01);

            }
        });

        // 设置actionbar导航
        binding.activityEditUserActionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 设置Save按钮
        binding.activityEditUserActionbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.save) {
                    onSave();
                }
                return false;
            }
        });

        // 获取用户信息，填充内容
        init();
    }

    private void onSave() {
        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    Log.d(TAG, jsonObject.toString());
//                    Log.d(TAG, jsonObject.getString("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        // 保存头像
        if (selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                binding.activityUserAvatar.setImageBitmap(bitmap);

                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                String mimeType = getContentResolver().getType(selectedImageUri);
                String filepath = UriToFilePath.getFilePathByUri(getApplicationContext(), selectedImageUri);
                builder.addFormDataPart("img", filepath, RequestBody.create(new File(filepath), MediaType.parse(mimeType)));

                RequestBody requestBody = builder.build();
                HttpUtil.sendRequestBody("/api/user/change/avatar", requestBody, callback);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 保存姓名、描述
        if (binding.editProfileName.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "用户名不能为空", Toast.LENGTH_SHORT);
        } else {
            String username = binding.editProfileName.getText().toString();
            String description = binding.editProfileDescription.getText().toString();
            HashMap<String, String> data = new HashMap<>();
            data.put("name", username);
            data.put("description", description);
            HttpUtil.sendPostRequest("/api/user/change/info", data, callback);
        }

        // 保存密码
        if (!binding.editProfilePassword.getText().toString().equals("")) {
            String password = binding.editProfilePassword.getText().toString();
            HashMap<String, String> data = new HashMap<>();
            data.put("password", password);
            HttpUtil.sendPostRequest("/api/user/change/password", data, callback);
        }

        finish();
    }

    private void init() {
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
                    System.out.println(jsonObject.toString());
                    user = new User(jsonObject);
                    // 初始化界面
                    UserEditActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.editProfileName.setText(user.username);
                            binding.editProfileDescription.setText(user.description);

                            // 如果用户没设置，使用默认头像
                            if (user.avatar.equals("")) {
                                binding.activityUserAvatar.setImageResource(R.drawable.ranga);
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
                                        UserEditActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                binding.activityUserAvatar.setImageBitmap(bitmap);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == 0x01) {
                // Get the url of the image from data
                assert data != null;
                selectedImageUri = data.getData();
                binding.activityUserAvatar.setImageURI(selectedImageUri);
            }
        }
    }

    // 获取应用所需要的权限
    private void getPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

}