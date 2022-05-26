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

    boolean selectImage = true;//为false表示录音
    String audioPath; //存储选择的音频的路径
    Uri audioUri;
    private GridImageAdapter mAdapter;

    private final List<LocalMedia> mData = new ArrayList<>();

    private final int maxSelectNum = 9;//最多选几个

    private final int maxSelectVideoNum = 1;//最多选一个video

    private ImageEngine imageEngine;

    private ActivityResultLauncher<Intent> launcherResult;

    private String draftPath;

    private String draftFileName;

    private SimpleDateFormat simpleDateFormat;

    private FragmentManager fragmentManager;

    private RecordDialogFragment dialog;//录音的组件

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
            // Address[addressLines=[0:"中国",1:"北京市海淀区",2:"华奥饭店公司写字间中关村创业大街"]latitude=39.980973,hasLongitude=true,longitude=116.301712]
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
     * 根据LocationManager获取定位信息的提供者
     * @param locationManager
     * @return
     */
    private static String getProvider(LocationManager locationManager){

        //获取位置信息提供者列表
        List<String> providerList = locationManager.getProviders(true);

        if (providerList.contains(LocationManager.NETWORK_PROVIDER)){
            //获取NETWORK定位
            return LocationManager.NETWORK_PROVIDER;
        }else if (providerList.contains(LocationManager.GPS_PROVIDER)){
            //获取GPS定位
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

        // 配置获取位置相关信息
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
//                // 生成一个Criteria对象
//                Criteria criteria = new Criteria();
//                // 设置查询条件
//                criteria.setAccuracy(Criteria.ACCURACY_FINE); // 设置准确而非粗糙的精度
//                criteria.setPowerRequirement(Criteria.POWER_LOW); // 设置相对省电而非耗电，一般高耗电量会换来更精确的位置信息
//                criteria.setAltitudeRequired(false); // 不需要提供海拔信息
//                criteria.setSpeedRequired(false); // 不需要速度信息
//                criteria.setCostAllowed(false); // 不能产生费用
//                // 第一个参数，传递criteria对象
//                // 第二个参数，若为false,在所有Provider中寻找，不管该Provider是否处于可用状态，均使用该Provider。
//                // 若为true，则在所有可用的Provider中寻找。比如GPS处于禁用状态，则忽略GPS Provider。
//                // 1、可用中最好的
//                String locationProvider = locationManager.getBestProvider(criteria, true);
//                if(locationProvider==null){
//                    List<String> providers = locationManager.getProviders(true);
//                    if (providers != null && providers.size() > 0) {
//                        locationProvider = providers.get(0);
//                    }
//
//                }
//                // 都不支持则直接返回
//                if (TextUtils.isEmpty(locationProvider)) {
//                    return;
//                }
//                Log.d(TAG, "locationProvider:"+locationProvider);
//                Location location = locationManager.getLastKnownLocation(locationProvider);
//                Log.i(TAG, "requestLatitudeAndLongtitude: location 1 =" + location);
//
//                if (location != null) {
//                    //updateCacheLocation(context, location.getLatitude(), location.getLongitude());
//
//                    Log.d(TAG, "Get location and not null!"+location);
//                } else {
//                    locationManager.requestLocationUpdates(locationProvider, 1000 * 60 * 60, 1000, new LocationListener() {
//                        @Override
//                        public void onLocationChanged(@NonNull Location location) {
//                            if(location==null){
//                                Log.d(TAG, "onLocationChanged: can not get!");
//                            }else{
//                                Log.d(TAG, "onLocationChanged: get new location!"+location);
//                            }
//                        }
//                    });
//
//                }

//                LocationRequest mLocationRequest=LocationRequest.create();
//                mLocationRequest.setInterval(60000);
//                mLocationRequest.setFastestInterval(5000);
//                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//                LocationCallback mLocationCallback = new LocationCallback() {
//                    @Override
//                    public void onLocationResult(LocationResult locationResult) {
//                        Log.d(TAG, "onLocationResult: get result");
//                        if (locationResult == null) {
//                            Log.d(TAG, "onLocationResult: null");
//                            return;
//                        }
//                        for (Location location : locationResult.getLocations()) {
//
//                            if (location != null) {
//                                //TODO: UI updates.
//                                Log.d(TAG, "onLocationResult: result"+location);
//                            }
//                        }
//                    }
//                };
//                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
//                fusedLocationProviderClient.getLastLocation()
//                        .addOnSuccessListener(getContext(), new OnSuccessListener<Location>() {
//                            @Override
//                            public void onSuccess(Location location) {
//                                Log.d(TAG, "onSuccess: Get result");
//                                if (location!=null){
//                                    Log.d(TAG, "onSuccess: "+location);
//                                }
//                            }
//                        });
                try {
                    /*获取LocationManager对象*/
                    Context context = getContext();
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    String provider = getProvider(locationManager);
                    if (provider == null) {
                        Toast.makeText(context, "定位失败", Toast.LENGTH_SHORT).show();
                    }
                    //系统权限检查警告，需要做权限判断
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

        //配置图片选择
        RecyclerView mRecyclerView =binding.imgRecycler;
        // Manager用于管理图片的显示
        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                3, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3,
                DensityUtil.dip2px(this, 8), false));

        // 适配器会把选择的图片数据同步进去
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
                //打开相册
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

        //处理菜单栏点击事件
        binding.postEditTopBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "onOptionsItemSelected: ");
                switch (item.getItemId()){

                    case R.id.publish:
                        Log.d(TAG, "onOptionsItemSelected: selected");
                        publish();
                        return true;
                    case R.id.close_edit:
                        Log.d(TAG, "onMenuItemClick: close");
                        //弹出是否保存到草稿
                        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(getContext());
                        builder.setTitle("退出确认")
                                .setMessage("是否保存到草稿箱？")
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



                        return true;
                    default:
                        return false;
                }
            }
        });

        //从草稿箱中恢复
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
                // 草稿箱存储
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
                        Log.d(TAG, "onCreate: 创建目录失败");
                    }else{
                        Log.d(TAG, "onCreate: 创建草稿箱目录成功");
                    }

                }
                draftFileName="draft_"+(System.currentTimeMillis())+".json";
                file=new File(draftPath+draftFileName);
                if(!file.exists()){
                    try {
                        file.createNewFile();
                        Log.d(TAG, "onCreate: 创建新文件用于草稿");
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
        //ArrayList<LocalMedia>l=new ArrayList<>();

        //OK!可以从网络获取图片
        //l.add(LocalMedia.generateLocalMedia("http://qiuyuhan.xyz:8080/static/src/36/0.jpg","image/jpg"));
        //LocalMedia media=new LocalMedia();
        //media.setPath("/storage/emulated/0/DCIM/Camera/IMG_20220514025554659.jpg");
        //media.setPath("content://media/external/images/media/34");
        //media.setRealPath("/storage/emulated/0/DCIM/Camera/IMG_20220514025554659.jpg");
        //media=buildLocalMedia("content://media/external/images/media/34");
        //l.add(media);

        //l.add(LocalMedia.generateLocalMedia("http://qiuyuhan.xyz:8080/static/src/39/0.wav","audio/wav"));
        //analyticalSelectResults(l);

    }

    // 用于切换到图片选择或录音
    private void switchSelectState(){
        if(selectImage){
            // 获取录音和写入文件的权限
            if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onClick: 无录音权限");
                ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.RECORD_AUDIO},1);
            }
            if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onClick: 无文件权限");
                ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},1);
            }

            // 创建录音用的fragment

            fragmentManager.beginTransaction().add(R.id.recorder_container,dialog).commit();
            fragmentManager.setFragmentResultListener("record", getContext(),
                    new FragmentResultListener() {
                        @Override
                        public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                            //获取录音结果的路径
                            String path=result.getString("path");
                            Log.d(TAG, "onFragmentResult: "+path);
                            //audioUri=result.getParcelable("uri");
                            // 可以获取录音的结果或者选择文件的结果，把结果放在audioPath中去
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
            // 关闭与图片相关的显示
            binding.imgRecycler.setEnabled(false);
            binding.imgRecycler.setVisibility(View.INVISIBLE);
            selectImage=false;
            //binding.recordFloatingBar.setBackgroundResource(R.drawable.ic_image_48px);
            binding.recordFloatingBar.setImageResource(R.drawable.ic_image_48px);
        }else{
            //切回显示图片的选择
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
        selectImage=!obj.getBoolean("selectImage");//要取反才对
        Log.d(TAG, "recoverContent: selectImage"+selectImage);
        switchSelectState();
        JSONArray srcs=obj.getJSONArray("src");
        String t=obj.getString("type");
        if(t.equals("audio")){
            JSONObject o= (JSONObject) srcs.get(0);
            audioPath=o.getString("path");
        }else if (t.equals("image")||t.equals("video")){
            ArrayList<LocalMedia>l=new ArrayList<>();
            for(int i=0;i<srcs.length();i++){

                JSONObject o= (JSONObject) srcs.get(0);
                LocalMedia media=buildLocalMedia(o.getString("path"));
                l.add(media);
            }
            analyticalSelectResults(l);
        }

        Log.d(TAG, "recoverContent: "+draftFileName);
    }
    private void saveContent() throws JSONException, IOException {
        JSONObject obj=new JSONObject();
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
            Log.i(TAG, "文件名: " + media.getFileName());
            Log.i(TAG, "是否压缩:" + media.isCompressed());
            Log.i(TAG, "压缩:" + media.getCompressPath());
            Log.i(TAG, "初始路径:" + media.getPath());
            Log.i(TAG, "绝对路径:" + media.getRealPath());
            Log.i(TAG, "是否裁剪:" + media.isCut());
            Log.i(TAG, "裁剪:" + media.getCutPath());
            Log.i(TAG, "是否开启原图:" + media.isOriginal());
            Log.i(TAG, "原图路径:" + media.getOriginalPath());
            Log.i(TAG, "沙盒路径:" + media.getSandboxPath());
            Log.i(TAG, "水印路径:" + media.getWatermarkPath());
            Log.i(TAG, "视频缩略图:" + media.getVideoThumbnailPath());
            Log.i(TAG, "原始宽高: " + media.getWidth() + "x" + media.getHeight());
            Log.i(TAG, "裁剪宽高: " + media.getCropImageWidth() + "x" + media.getCropImageHeight());
            Log.i(TAG, "文件大小: " + media.getSize());


        }

        //更新显示
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

    // 纯文字上传
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
                    Intent intent=new Intent(getContext(),MainPageActivity.class);
                    startActivity(intent);
                }else{
                    Log.d(TAG, "onResponse: "+response.body().string());
                }
            }
        });
    }

    // 音频上传
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
//            //TODO 不要硬编码
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
                    Intent intent=new Intent(getContext(),MainPageActivity.class);
                    startActivity(intent);
                }else{
                    Log.d(TAG, "onResponse: "+response.body().string());
                }
            }
        });

    }

    // 图片或视频上传
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
            // 处理本地图片和在线图片
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
                // 下载在线图片
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
                    Intent intent=new Intent(getContext(),MainPageActivity.class);
                    startActivity(intent);
                }else{
                    Log.d(TAG, "onResponse: "+response.body().string());
                }
            }
        });
    }
    private void postSuccess(){
        Log.d(TAG, "postSuccess: 成功发送，删除文件");
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

    //将图片或视频的uri转化为路径
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