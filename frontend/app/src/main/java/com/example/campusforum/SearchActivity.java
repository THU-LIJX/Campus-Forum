package com.example.campusforum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        //? 搜索中的信息如下
//        Editable search = binding.searchContentText.getText();
//        if (search == null) {
//            Log.d(TAG, "Trash");
//

        ArrayList<String> items = new ArrayList<>();
        //! text、image、sound、video
        items.add("文字"); //text
        items.add("图像"); // image
        items.add("语音"); // sound
        items.add("视频"); // video
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.serach_list_item, items);
        binding.searchSelect.setAdapter(adapter);
        setContentView(binding.getRoot());

        binding.btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                Log.d(TAG, jsonArray.toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                   }
               });
            }
        });

    }
//    private showMenu(View v, ) {

//}
}

