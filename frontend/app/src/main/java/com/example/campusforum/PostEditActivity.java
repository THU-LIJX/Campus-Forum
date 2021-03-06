package com.example.campusforum;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.campusforum.databinding.ActivityPostEditBinding;
import com.example.campusforum.mediaselector.FullyGridLayoutManager;
import com.example.campusforum.mediaselector.GlideEngine;
import com.example.campusforum.mediaselector.GridImageAdapter;
import com.example.campusforum.mediaselector.RecordDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.luck.picture.lib.basic.PictureSelectionModel;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.engine.ImageEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.interfaces.OnQueryAllAlbumListener;
import com.luck.picture.lib.interfaces.OnQueryDataSourceListener;
import com.luck.picture.lib.loader.IBridgeMediaLoader;
import com.luck.picture.lib.utils.ActivityCompatHelper;
import com.luck.picture.lib.utils.BitmapUtils;
import com.luck.picture.lib.utils.DensityUtil;
import com.luck.picture.lib.utils.MediaUtils;
import com.luck.picture.lib.utils.PictureFileUtils;
import com.luck.picture.lib.utils.SdkVersionUtils;
import com.luck.picture.lib.utils.ValueOf;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class PostEditActivity extends AppCompatActivity {
    ActivityPostEditBinding binding;
    private static final String TAG = "PostEditActivity";

    boolean selectImage = true;//???false????????????
    String audioPath; //??????????????????????????????
    Uri audioUri;
    private GridImageAdapter mAdapter;

    private final List<LocalMedia> mData = new ArrayList<>();

    private final int maxSelectNum = 9;//???????????????

    private final int maxSelectVideoNum = 1;//???????????????video

    private ImageEngine imageEngine;

    private ActivityResultLauncher<Intent> launcherResult;

    private String draftPath;

    private String draftFileName;

    private SimpleDateFormat simpleDateFormat;

    private FragmentManager fragmentManager;

    private RecordDialogFragment dialog;//???????????????

    private LocationManager locationManager;

    private String address="";

    private String getLocationAddress(Location location) {
        String add = "";
        Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.CHINESE);
        try {
            List<Address> addresses = geoCoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(),
                    1);
            Address address = addresses.get(0);
            Log.i(TAG, "getLocationAddress: " + address.toString());
            // Address[addressLines=[0:"??????",1:"??????????????????",2:"????????????????????????????????????????????????"]latitude=39.980973,hasLongitude=true,longitude=116.301712]
            int maxLine = address.getMaxAddressLineIndex();
            if (maxLine >= 3) {
                add = address.getAddressLine(1) + address.getAddressLine(2);
            } else {
                add = address.getAddressLine(0);
            }
        } catch (IOException e) {
            add = "";
            e.printStackTrace();
        }
        return add;
    }

    /**
     * ??????LocationManager??????????????????????????????
     * @param locationManager
     * @return
     */
    private static String getProvider(LocationManager locationManager){

        //?????????????????????????????????
        List<String> providerList = locationManager.getProviders(true);

        if (providerList.contains(LocationManager.NETWORK_PROVIDER)){
            //??????NETWORK??????
            return LocationManager.NETWORK_PROVIDER;
        }else if (providerList.contains(LocationManager.GPS_PROVIDER)){
            //??????GPS??????
            return LocationManager.GPS_PROVIDER;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        simpleDateFormat = new SimpleDateFormat();
        simpleDateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");


        binding = ActivityPostEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ??????????????????????????????
        locationManager=(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);;

        binding.getLocationBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Clicked");
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                    ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
                    //return;
                }

                try {
                    /*??????LocationManager??????*/
                    Context context = getContext();
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    String provider = getProvider(locationManager);
                    if (provider == null) {
                        Toast.makeText(context, "????????????", Toast.LENGTH_SHORT).show();
                    }
                    //????????????????????????????????????????????????
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling ActivityCompat#requestPermissions
                    }
                    Location location = locationManager.getLastKnownLocation(provider);
                    Log.d("location", location.getLongitude() + ":" + location.getLatitude());
                    Geocoder gc = new Geocoder(getContext(), Locale.CHINESE);
                    List<Address> locationList = null;
                    try {
                        locationList = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 10);
                        address = locationList.get(0).getAddressLine(0);
                        binding.getLocationBtn.setText(address);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null,
                            getApplication().getMainExecutor(), new Consumer<Location>() {
                                @Override
                                public void accept(Location location) {
                                    Log.d(TAG, "accept: Get location"+location);
                                    address=getLocationAddress(location);
                                    Log.d(TAG,"Get Address:"+address);
                                    binding.getLocationBtn.setText(address);
                                }
                            });
                }


            }
        });

        fragmentManager=getSupportFragmentManager();
        dialog=RecordDialogFragment.newInstance();
        //binding.postEditTopBar.set
        binding.recordFloatingBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchSelectState();

            }
        });

        //??????????????????
        RecyclerView mRecyclerView =binding.imgRecycler;
        // Manager???????????????????????????
        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                3, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3,
                DensityUtil.dip2px(this, 8), false));

        // ????????????????????????????????????????????????
        mAdapter = new GridImageAdapter(this, mData);
        mAdapter.setSelectMax(maxSelectNum + maxSelectVideoNum);
        mRecyclerView.setAdapter(mAdapter);

        imageEngine= GlideEngine.createGlideEngine();
        mAdapter.setOnItemClickListener(new GridImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Log.d("Post Edit ", "onItemClick:");
                PictureSelector.create(getContext())
                        .openPreview()
                        .setImageEngine(imageEngine)
                        .startActivityPreview(position, true, mAdapter.getData());
            }

            @Override
            public void openPicture() {
                Log.d(TAG, "openPicture: click");
                //????????????
                PictureSelectionModel selectionModel = PictureSelector.create(getContext())
                        .openGallery(SelectMimeType.ofAll())
                        .setImageEngine(imageEngine)
                        .setMaxSelectNum(maxSelectNum)
                        .setMaxVideoSelectNum(maxSelectVideoNum)
                        .setSelectedData(mAdapter.getData());
                forSelectResult(selectionModel);
            }
        });

        launcherResult = createActivityResultLauncher();

        binding.postEditTopBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onMenuItemClick: close");
                //???????????????????????????
                MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(getContext());
                builder.setTitle("????????????")
                        .setMessage("???????????????????????????")
                        .setPositiveButton(R.string.confirm_save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.confirm_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                File file=new File(draftPath+draftFileName);
                                file.delete();
                                finish();
                            }
                        }).show();
            }
        });

        //???????????????????????????
        binding.postEditTopBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "onOptionsItemSelected: ");
                switch (item.getItemId()){

                    case R.id.publish:
                        Log.d(TAG, "onOptionsItemSelected: selected");
                        publish();
                        return true;
//                    case R.id.close_edit:
//                        Log.d(TAG, "onMenuItemClick: close");
//                        //???????????????????????????
//                        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(getContext());
//                        builder.setTitle("????????????")
//                                .setMessage("???????????????????????????")
//                                .setPositiveButton(R.string.confirm_save, new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialogInterface, int i) {
//                                        finish();
//                                    }
//                                })
//                                .setNegativeButton(R.string.confirm_cancel, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int i) {
//                                    File file=new File(draftPath+draftFileName);
//                                    file.delete();
//                                    finish();
//                                }
//                        }).show();
//
//
//
//                        return true;
                    default:
                        return false;
                }
            }
        });

        //?????????????????????
        Intent intent=getIntent();
        draftPath= Environment.getExternalStorageDirectory()+"/Documents/Campus/";
        if(intent!=null){
            String draftStr=intent.getStringExtra("draft");
            if(draftStr!=null){
                try {
                    recoverContent(draftStr);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "onCreate: "+draftStr);
            }else{
                // ???????????????
                if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},1);
                    }
                }
                if(ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }

                File file=new File(draftPath);
                if(!file.exists()){
                    if(!file.mkdirs()){
                        Log.d(TAG, "onCreate: ??????????????????");
                    }else{
                        Log.d(TAG, "onCreate: ???????????????????????????");
                    }

                }
                draftFileName="draft_"+(System.currentTimeMillis())+".json";
                file=new File(draftPath+draftFileName);
                if(!file.exists()){
                    try {
                        file.createNewFile();
                        Log.d(TAG, "onCreate: ???????????????????????????");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        binding.postText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                Log.d(TAG, "onKey: ");
                try {
                    saveContent();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
        try {
            saveContent();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // ????????????????????????????????????
    private void switchSelectState(){
        if(selectImage){
            // ????????????????????????????????????
            if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onClick: ???????????????");
                ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.RECORD_AUDIO},1);
            }
            if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onClick: ???????????????");
                ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},1);
            }
            // ??????????????????
            MediaPlayer mediaPlayer = new MediaPlayer();
            if(audioPath!=null){
                try {

                    mediaPlayer.setDataSource(audioPath);
                    mediaPlayer.prepare();
                    long duration = mediaPlayer.getDuration(); //??????
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
                    dialog.setDuration(seconds);
                    dialog.setFilePath(audioPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            // ??????????????????fragment

            fragmentManager.beginTransaction().add(R.id.recorder_container,dialog).commit();
            fragmentManager.setFragmentResultListener("record", getContext(),
                    new FragmentResultListener() {
                        @Override
                        public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                            //???????????????????????????
                            String path=result.getString("path");
                            Log.d(TAG, "onFragmentResult: "+path);
                            //audioUri=result.getParcelable("uri");
                            // ????????????????????????????????????????????????????????????????????????audioPath??????
                            audioPath=path;
                            try {
                                saveContent();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            // ??????????????????????????????
            binding.imgRecycler.setEnabled(false);
            binding.imgRecycler.setVisibility(View.INVISIBLE);
            selectImage=false;
            //binding.recordFloatingBar.setBackgroundResource(R.drawable.ic_image_48px);
            binding.recordFloatingBar.setImageResource(R.drawable.ic_image_48px);
        }else{
            //???????????????????????????
            fragmentManager.beginTransaction().remove(dialog).commit();
            binding.imgRecycler.setEnabled(true);
            binding.imgRecycler.setVisibility(View.VISIBLE);
            selectImage=true;
            binding.recordFloatingBar.setImageResource(R.drawable.ic_voice_48px);
        }
    }
    private void recoverContent(String jsonStr) throws JSONException {
        JSONObject obj=new JSONObject(jsonStr);
        binding.postText.setText(obj.getString("text"));
        binding.titleText.setText(obj.getString("title"));
        draftFileName=obj.getString("draftFileName");
        selectImage=!obj.getBoolean("selectImage");//???????????????
        Log.d(TAG, "recoverContent: selectImage"+selectImage);

        JSONArray srcs=obj.getJSONArray("src");
        String t=obj.getString("type");
        if(t.equals("audio")){
            JSONObject o= (JSONObject) srcs.get(0);
            audioPath=o.getString("path");
        }else if (t.equals("image")||t.equals("video")){
            ArrayList<LocalMedia>l=new ArrayList<>();
            for(int i=0;i<srcs.length();i++){

                JSONObject o= (JSONObject) srcs.get(i);
                LocalMedia media=buildLocalMedia(o.getString("path"));
                l.add(media);
            }
            analyticalSelectResults(l);
        }
        switchSelectState();
        Log.d(TAG, "recoverContent: "+draftFileName);
    }
    private void saveContent() throws JSONException, IOException {
        JSONObject obj=new JSONObject();
        obj.put("id",User.currentUser.userId);
        obj.put("text",binding.postText.getText().toString());
        obj.put("title",binding.titleText.getText().toString());
        JSONArray srcs=new JSONArray();
        if(selectImage){
            for(LocalMedia media:mAdapter.getData()){

                JSONObject mediaObj=new JSONObject();
                mediaObj.put("path",media.getPath());
                srcs.put(mediaObj);
            }
        }else{
            JSONObject mediaObj=new JSONObject();
            mediaObj.put("path",audioPath);
            srcs.put(mediaObj);
        }
        if(srcs.length()==0){
            obj.put("type","text");
        }else if(selectImage){
            String fileType=mAdapter.getData().get(0).getMimeType().split("/")[0];
            //Log.d(TAG,"fileType:"+fileType);
            obj.put("type",fileType);
        }else{
            obj.put("type","audio");
        }

        obj.put("src",srcs);
        obj.put("selectImage",selectImage);

        obj.put("draftFileName",draftFileName);
        Date date=new Date();

        obj.put("time",simpleDateFormat.format(date));
        Log.d(TAG, "saveContent: "+obj);
        File file=new File(draftPath+draftFileName);
        FileOutputStream fileOutputStream=new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter=new OutputStreamWriter(fileOutputStream,"utf-8");
        outputStreamWriter.write(obj.toString());
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    private void analyticalSelectResults(ArrayList<LocalMedia> result) {
        for (LocalMedia media : result) {
            if (media.getWidth() == 0 || media.getHeight() == 0) {
                if (PictureMimeType.isHasImage(media.getMimeType())) {
                    MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(getContext(), media.getPath());
                    media.setWidth(imageExtraInfo.getWidth());
                    media.setHeight(imageExtraInfo.getHeight());
                } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                    MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(getContext(), media.getPath());
                    media.setWidth(videoExtraInfo.getWidth());
                    media.setHeight(videoExtraInfo.getHeight());
                }
            }
            Log.i(TAG, "?????????: " + media.getFileName());
            Log.i(TAG, "????????????:" + media.isCompressed());
            Log.i(TAG, "??????:" + media.getCompressPath());
            Log.i(TAG, "????????????:" + media.getPath());
            Log.i(TAG, "????????????:" + media.getRealPath());
            Log.i(TAG, "????????????:" + media.isCut());
            Log.i(TAG, "??????:" + media.getCutPath());
            Log.i(TAG, "??????????????????:" + media.isOriginal());
            Log.i(TAG, "????????????:" + media.getOriginalPath());
            Log.i(TAG, "????????????:" + media.getSandboxPath());
            Log.i(TAG, "????????????:" + media.getWatermarkPath());
            Log.i(TAG, "???????????????:" + media.getVideoThumbnailPath());
            Log.i(TAG, "????????????: " + media.getWidth() + "x" + media.getHeight());
            Log.i(TAG, "????????????: " + media.getCropImageWidth() + "x" + media.getCropImageHeight());
            Log.i(TAG, "????????????: " + media.getSize());


        }

        //????????????
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isMaxSize = result.size() == mAdapter.getSelectMax();
                int oldSize = mAdapter.getData().size();
                mAdapter.notifyItemRangeRemoved(0, isMaxSize ? oldSize + 1 : oldSize);
                mAdapter.getData().clear();

                mAdapter.getData().addAll(result);
                mAdapter.notifyItemRangeInserted(0, result.size());
                try {
                    saveContent();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    private ActivityResultLauncher<Intent> createActivityResultLauncher() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        int resultCode = result.getResultCode();
                        if (resultCode == RESULT_OK) {
                            ArrayList<LocalMedia> selectList = PictureSelector.obtainSelectorList(result.getData());
                            analyticalSelectResults(selectList);
                        } else if (resultCode == RESULT_CANCELED) {
                            Log.i(TAG, "onActivityResult PictureSelector Cancel");
                        }
                    }
                });
    }
    private void forSelectResult(PictureSelectionModel model) {
                model.forResult(launcherResult);
    }

    PostEditActivity getContext(){
        return this;
    }

    // ???????????????
    private void publishPureText(){
        String text=binding.postText.getText().toString();
        HashMap<String,String>data=new HashMap<>();
        data.put("text",text);
        data.put("type","text");
        data.put("title",binding.titleText.getText().toString());
        data.put("location",address);
        HttpUtil.sendPostRequest("/api/user/post",data, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "onFailure: "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code()==200){
                    postSuccess();
                    finish();
//                    Intent intent=new Intent(getContext(),MainPageActivity.class);
//                    startActivity(intent);
                }else{
                    Log.d(TAG, "onResponse: "+response.body().string());
                }
            }
        });
    }

    // ????????????
    private void publishAudio(){
        String text=binding.postText.getText().toString();
        MultipartBody.Builder builder=new MultipartBody.Builder().setType(MultipartBody.FORM);
        //String fileType=mAdapter.getData().get(0).getMimeType().split("/")[0];
        //Log.d(TAG,"fileType:"+fileType);
        builder.addFormDataPart("text",text);
        builder.addFormDataPart("title",binding.titleText.getText().toString());
        builder.addFormDataPart("type","sound");
        builder.addFormDataPart("location",address);
        File file=new File(audioPath);
        String filename=file.getName();
        String type=filename.substring(filename.lastIndexOf("."));
        Log.d(TAG, "publish: file exists"+file.exists()+audioPath);

        if(!file.exists()){

            return ;
        }
        builder.addFormDataPart(
                "src",
                file.getName(),
                RequestBody.create(file,MediaType.get("audio/"+type))
        );
//        try {
//            FileDescriptor fd=getContext().getContentResolver().openFileDescriptor(audioUri,"r")
//                    .getFileDescriptor();
//            FileInputStream inputStream=new FileInputStream(fd);
//            byte[] buffer=new byte[4096];
//            ByteArrayOutputStream out=new ByteArrayOutputStream();
//            int nRead;
//
//            while((nRead=inputStream.read(buffer,0,buffer.length))!=-1){
//                out.write(buffer,0,nRead);
//            }
//            //TODO ???????????????
//            builder.addFormDataPart(
//                    "src",
//                    "audio.wav",
//                    RequestBody.create(out.toByteArray(),MediaType.get("audio/wav"))
//            );
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        RequestBody requestBody=builder.build();
        HttpUtil.sendRequestBody("/api/user/post", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "onFailure: "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code()==200){
                    postSuccess();
                    finish();
//                    Intent intent=new Intent(getContext(),MainPageActivity.class);
//                    startActivity(intent);
                }else{
                    Log.d(TAG, "onResponse: "+response.body().string());
                }
            }
        });

    }

    // ?????????????????????
    private void publishImageOrVideo(){
        String text=binding.postText.getText().toString();
        MultipartBody.Builder builder=new MultipartBody.Builder().setType(MultipartBody.FORM);
        String fileType=mAdapter.getData().get(0).getMimeType().split("/")[0];
        Log.d(TAG,"fileType:"+fileType);
        builder.addFormDataPart("text",text);
        builder.addFormDataPart("type",fileType);
        builder.addFormDataPart("location",address);
        builder.addFormDataPart("title",binding.titleText.getText().toString());
        for(LocalMedia media:mAdapter.getData()){
            //Uri uri=Uri.parse(media.getAvailablePath());
            // ?????????????????????????????????
            File file;
            if(media.getRealPath()!=null){
                file=new File(media.getRealPath());
                Log.d(TAG, "publish: file exists"+file.exists()+file.getName());
                if(!file.exists()){
                    return ;
                }
                fileType=media.getMimeType();
                Log.d(TAG, "publish : filetype"+fileType);
                builder.addFormDataPart(
                        "src",
                        media.getFileName(),
                        RequestBody.create(file,MediaType.get(fileType))
                );
            }
            else{
                // ??????????????????
                OkHttpClient client = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).build();

                Request request = new Request.Builder().url(media.getPath()).get().build();

                Call call = client.newCall(request);

                try {

                    Response response = call.execute();
                    fileType=media.getMimeType();

                    Log.d(TAG, "publish : filetype"+fileType);

                    builder.addFormDataPart(
                            "src",
                            System.currentTimeMillis()+"."+media.getMimeType().split("/")[1],
                            RequestBody.create(response.body().bytes(),MediaType.get(fileType))
                    );


                } catch (IOException e) {

                    e.printStackTrace();

                }
            }


        }

        RequestBody requestBody=builder.build();
        HttpUtil.sendRequestBody("/api/user/post", requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "onFailure: "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code()==200){
                    postSuccess();
                    finish();
//                    Intent intent=new Intent(getContext(),MainPageActivity.class);
//                    startActivity(intent);
                }else{
                    Log.d(TAG, "onResponse: "+response.body().string());
                }
            }
        });
    }
    private void postSuccess(){
        Log.d(TAG, "postSuccess: ???????????????????????????");
        File file=new File(draftPath+draftFileName);
        file.delete();
    }
    private void publish(){
        new Thread("publish") {
            @Override
            public void run() {
                if (!selectImage) {
                    publishAudio();
                    return;
                }
                if (mAdapter.getData().size() == 0) {
                    publishPureText();
                    return;
                }
                publishImageOrVideo();
            }
        }.start();


    }

    //?????????????????????uri???????????????
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(this, uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    LocalMedia buildLocalMedia(String generatePath) {
        if (ActivityCompatHelper.isDestroy(getContext())) {
            return null;
        }
        long id = 0, bucketId=0;
        File cameraFile;
        String mimeType;
        if (PictureMimeType.isContent(generatePath)) {
            Uri cameraUri = Uri.parse(generatePath);
            String path = PictureFileUtils.getPath(getContext(), cameraUri);
            cameraFile = new File(path);
            mimeType = MediaUtils.getMimeTypeFromMediaUrl(cameraFile.getAbsolutePath());
            if (PictureFileUtils.isMediaDocument(cameraUri)) {
                String documentId = DocumentsContract.getDocumentId(cameraUri);
                if (!TextUtils.isEmpty(documentId)) {
                    String[] split = documentId.split(":");
                    if (split.length > 1) {
                        id = ValueOf.toLong(split[1]);
                    }
                }
            } else if (PictureFileUtils.isDownloadsDocument(cameraUri)) {
                id = ValueOf.toLong(DocumentsContract.getDocumentId(cameraUri));
            } else {
                int lastIndexOf = generatePath.lastIndexOf("/") + 1;
                id = lastIndexOf > 0 ? ValueOf.toLong(generatePath.substring(lastIndexOf)) : System.currentTimeMillis();
            }
            if (PictureMimeType.isHasAudio(mimeType)) {
                bucketId = MediaUtils.generateSoundsBucketId(getContext(), cameraFile, "");
            } else {
                bucketId = MediaUtils.generateCameraBucketId(getContext(), cameraFile, "");
            }
        } else {
            cameraFile = new File(generatePath);
            mimeType = MediaUtils.getMimeTypeFromMediaUrl(cameraFile.getAbsolutePath());
            id = System.currentTimeMillis();
//            if (PictureMimeType.isHasAudio(mimeType)) {
//                bucketId = MediaUtils.generateSoundsBucketId(getContext(), cameraFile, config.outPutCameraDir);
//            } else {
//                bucketId = MediaUtils.generateCameraBucketId(getContext(), cameraFile, config.outPutCameraDir);
//            }
        }
        if (PictureMimeType.isHasImage(mimeType)) {
//            if (config.isCameraRotateImage) {
//                BitmapUtils.rotateImage(getContext(), generatePath);
//            }
        }
        MediaExtraInfo mediaExtraInfo;
        if (PictureMimeType.isHasVideo(mimeType)) {
            mediaExtraInfo = MediaUtils.getVideoSize(getContext(), generatePath);
        } else if (PictureMimeType.isHasAudio(mimeType)) {
            mediaExtraInfo = MediaUtils.getAudioSize(getContext(), generatePath);
        } else {
            mediaExtraInfo = MediaUtils.getImageSize(getContext(), generatePath);
        }
        String folderName = MediaUtils.generateCameraFolderName(cameraFile.getAbsolutePath());
        LocalMedia media = LocalMedia.parseLocalMedia(id, generatePath, cameraFile.getAbsolutePath(),
                cameraFile.getName(), folderName, mediaExtraInfo.getDuration(),SelectMimeType.ofAll() ,
                mimeType, mediaExtraInfo.getWidth(), mediaExtraInfo.getHeight(), cameraFile.length(), bucketId,
                cameraFile.lastModified() / 1000);
        if (SdkVersionUtils.isQ()) {
            media.setSandboxPath(PictureMimeType.isContent(generatePath) ? null : generatePath);
        }
        return media;
    }


}