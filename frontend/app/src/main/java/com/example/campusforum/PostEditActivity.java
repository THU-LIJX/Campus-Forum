package com.example.campusforum;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
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

import com.example.campusforum.databinding.ActivityPostEditBinding;
import com.example.campusforum.mediaselector.FullyGridLayoutManager;
import com.example.campusforum.mediaselector.GlideEngine;
import com.example.campusforum.mediaselector.GridImageAdapter;
import com.example.campusforum.mediaselector.RecordDialogFragment;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    boolean selectImage=true;//为false表示录音
    String audioPath; //存储选择的音频的路径
    Uri audioUri;
    private GridImageAdapter mAdapter;

    private final List<LocalMedia>mData=new ArrayList<>();

    private final int maxSelectNum=9;//最多选几个

    private final int maxSelectVideoNum=1;//最多选一个video

    private ImageEngine imageEngine;

    private ActivityResultLauncher<Intent> launcherResult;

    private String draftPath;

    private String draftFileName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent=getIntent();
        draftPath= Environment.getExternalStorageDirectory()+"/Campus/";
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
                    ActivityCompat.requestPermissions(getContext(),new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},1);
                }

                File file=new File(draftPath);
                if(!file.exists()){
                    file.mkdir();
                    Log.d(TAG, "onCreate: 创建草稿箱目录");
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

       

        binding = ActivityPostEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        //TODO: 添加对saveInstanceState的处理，从草稿箱跳入可以直接用
        // 配置录音相关
        FragmentManager fragmentManager=getSupportFragmentManager();
        RecordDialogFragment dialog=RecordDialogFragment.newInstance();
        //binding.postEditTopBar.set
        binding.recordFloatingBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                        return true;
                    default:
                        return false;
                }
            }
        });
        ArrayList<LocalMedia>l=new ArrayList<>();

        //OK!可以从网络获取图片
        //l.add(LocalMedia.generateLocalMedia("http://qiuyuhan.xyz:8080/static/src/36/0.jpg","image/jpg"));
        //LocalMedia media=new LocalMedia();
        //media.setPath("/storage/emulated/0/DCIM/Camera/IMG_20220514025554659.jpg");
        //media.setPath("content://media/external/images/media/34");
        //media.setRealPath("/storage/emulated/0/DCIM/Camera/IMG_20220514025554659.jpg");
        //media=buildLocalMedia("content://media/external/images/media/34");
        //l.add(media);

        //l.add(LocalMedia.generateLocalMedia("http://qiuyuhan.xyz:8080/static/src/39/0.wav","audio/wav"));
        analyticalSelectResults(l);
    }
    private void recoverContent(String jsonStr) throws JSONException {
        JSONObject obj=new JSONObject(jsonStr);
        binding.postText.setText(obj.getString("text"));
        draftFileName=obj.getString("draftFileName");
        Log.d(TAG, "recoverContent: "+draftFileName);
    }
    private void saveContent() throws JSONException, IOException {
        JSONObject obj=new JSONObject();
        obj.put("text",binding.postText.getText().toString());
        JSONArray srcs=new JSONArray();
        for(LocalMedia media:mAdapter.getData()){

            JSONObject mediaObj=new JSONObject();
            mediaObj.put("path",media.getPath());
            srcs.put(mediaObj);
        }
        obj.put("src",srcs);
        obj.put("selectImage",selectImage);
        obj.put("draftFileName",draftFileName);
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
        builder.addFormDataPart("type","sound");
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