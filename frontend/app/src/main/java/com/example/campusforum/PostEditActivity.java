package com.example.campusforum;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.example.campusforum.databinding.ActivityMainPageBinding;
import com.example.campusforum.databinding.ActivityPostEditBinding;
import com.luck.picture.lib.basic.PictureSelectionModel;
import com.luck.picture.lib.basic.PictureSelectionSystemModel;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.engine.ImageEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.utils.DensityUtil;
import com.luck.picture.lib.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;


public class PostEditActivity extends AppCompatActivity {

    private static final String TAG = "PostEditActivity";
    ActivityPostEditBinding binding;

    private GridImageAdapter mAdapter;

    private final List<LocalMedia>mData=new ArrayList<>();

    private int maxSelectNum=9;//最多选几个

    private int maxSelectVideoNum=1;//最多选一个video

    private ImageEngine imageEngine;

    private ActivityResultLauncher<Intent> launcherResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPostEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //配置图片选择
        RecyclerView mRecyclerView =binding.imgRecycler;

        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                4, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(3,
                DensityUtil.dip2px(this, 8), false));
        mAdapter = new GridImageAdapter(this, mData);
        mAdapter.setSelectMax(maxSelectNum + maxSelectVideoNum);
        mRecyclerView.setAdapter(mAdapter);

        imageEngine=GlideEngine.createGlideEngine();
        mAdapter.setOnItemClickListener(new GridImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Log.d("Post Edit ", "onItemClick:");
                PictureSelector.create(PostEditActivity.this)
                        .openPreview()
                        .setImageEngine(imageEngine)
                        .startActivityPreview(position, true, mAdapter.getData());
            }

            @Override
            public void openPicture() {
                //打开相册
                PictureSelectionModel selectionModel = PictureSelector.create(PostEditActivity.this)
                        .openGallery(SelectMimeType.ofImage())
                        .setImageEngine(imageEngine)
                        .setMaxSelectNum(maxSelectNum)
                        .setMaxVideoSelectNum(maxSelectVideoNum)
                        .setSelectedData(mAdapter.getData());
                forSelectResult(selectionModel);
            }
        });

        launcherResult = createActivityResultLauncher();
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


}