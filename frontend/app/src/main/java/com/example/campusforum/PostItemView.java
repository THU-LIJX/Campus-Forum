package com.example.campusforum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.circularreveal.CircularRevealGridLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.previewlibrary.GPreviewBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PostItemView extends LinearLayoutCompat {

    // 在java代码里new时会用到
    public PostItemView(Context context) {
        this(context, null);
    }

    // 在xml布局文件中使用时自动调用
    public PostItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // 不会自动调用，如果有默认style时，在第二个构造函数中调用
    public PostItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private CircularRevealGridLayout gridLayoutImages; // 图片容器
    private List<ShapeableImageView> images;           // 图片
    private LinearLayoutCompat videoContainer;         // 视频容器
    private JCVideoPlayerStandard videoPlayer;         // 视频播放器
    private MaterialCardView audioContainer;           // 音频容器
    private TextView username;                         // 用户名
    private TextView content;                          // 正文
    private TextView commentNum;                       // 评论数
    private TextView likeNum;                          // 点赞数
    private ShapeableImageView commentIcon;            // 评论图标
    private ShapeableImageView likeIcon;               // 点赞图标
    private ShapeableImageView shareIcon;              // 分享图标

    private Activity activity;                         // view所在的activity
    private PostAdapter postAdapter;                   // view所在的adapter(如果使用list展示)
    private Integer position;                          // view的位置(如果使用list展示)
    private String audioState = PAUSING;               // audio状态

    static private final String PLAYING = "PLAYING";
    static private final String PAUSING = "PAUSING";

    // 初始化控件
    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.post_item, this);
        gridLayoutImages = (CircularRevealGridLayout) view.findViewById(R.id.post_item_images);
        videoContainer = (LinearLayoutCompat) view.findViewById(R.id.post_item_video_container);
        videoPlayer = (JCVideoPlayerStandard) view.findViewById(R.id.post_item_video);
        audioContainer = (MaterialCardView) view.findViewById(R.id.post_item_audio);
        username = (TextView) view.findViewById(R.id.post_item_user_name);
        content = (TextView) view.findViewById(R.id.post_item_content);
        commentNum = (TextView) view.findViewById(R.id.post_item_comment_num);
        likeNum = (TextView) view.findViewById(R.id.post_item_like_num);
        commentIcon = (ShapeableImageView) view.findViewById(R.id.post_item_comment_icon);
        likeIcon = (ShapeableImageView) view.findViewById(R.id.post_item_like_icon);
        shareIcon = (ShapeableImageView) view.findViewById(R.id.post_item_share_icon);
        shareIcon = (ShapeableImageView) view.findViewById(R.id.post_item_image1);

        images = new ArrayList<>();
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image1));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image2));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image3));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image4));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image5));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image6));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image7));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image8));
        images.add((ShapeableImageView) view.findViewById(R.id.post_item_image9));
    }

    // 在setPost之前必须调用
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    // 如果使用list展示，则需要在setPost之前调用
    public void setAdapter(PostAdapter postAdapter) {
        this.postAdapter = postAdapter;
    }

    // 如果使用list展示，则需要在setPost之前调用
    public void setPosition(int position) {
        this.position = position;
    }

    // 点击评论图标时的动作
    public void setCommentAction(OnClickListener callback) {
        commentIcon.setOnClickListener(callback);
    }

    public void setPost(PostAdapter.Post post) {
        // 根据Post类型显示不同组件
        switch (post.type) {
            case PostAdapter.Post.IMAGE_TYPE:
                gridLayoutImages.setVisibility(View.VISIBLE);
                setImages(post);
                break;
            case PostAdapter.Post.VIDEO_TYPE:
                videoContainer.setVisibility(View.VISIBLE);
                videoPlayer.setUp(HttpUtil.baseUrl + post.dataSources.get(0), JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL, "");
                break;
            case PostAdapter.Post.AUDIO_TYPE:
                audioContainer.setVisibility(View.VISIBLE);
                setAudio(post);
                break;
        }

        username.setText(Integer.toString(post.postId));          // 设置用户名
        content.setText(post.content);                            // 设置正文
        likeNum.setText(Integer.toString(post.likeNum));          // 设置点赞数

        // 点赞/取消点赞
        likeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> data = new HashMap<>();
                data.put("id", Integer.toString(post.userId));
                Callback callback = new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        String result = Objects.requireNonNull(response.body()).string();
                        JSONObject jsonOb;
                        try {
                            jsonOb = new JSONObject(result);
                            if (jsonOb.getString("message").equals("ok")) {
                                JSONArray jsonArray = jsonOb.getJSONArray("likeby");
                                List<Integer> tmpList = new ArrayList<>();
                                for (int i = 0; i < jsonArray.length(); i++) tmpList.add(jsonArray.getInt(i));
                                post.likeBy = tmpList;
                                setLikeInfo(post);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                if (post.likeBy.contains(post.userId)) {
                    // 取消点赞
                    HttpUtil.sendPostRequest("/api/user/dislike", data, callback);
                } else {
                    // 点赞
                    HttpUtil.sendPostRequest("/api/user/like", data, callback);
                }
            }
        });

        // 分享
        shareIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void setImages(PostAdapter.Post post) {
        // 行列数，当图片数为4时是2x2的，否则每行3x3，即使没有足够的图片
        int rowNum;
        int colNum;
        if (post.dataSources.size() == 4) {
            rowNum = 2;
            colNum = 2;
        } else {
            colNum = 3;
            rowNum = (int) Math.ceil((double) post.dataSources.size() / 3.0);
        }
        gridLayoutImages.setRowCount(rowNum);
        gridLayoutImages.setColumnCount(colNum);

        // 图片预览
        List<UserViewInfo> userViewInfoList = new ArrayList<>();
        for (int i = 0; i < post.dataSources.size(); i++) {
            int finalI = i;
            userViewInfoList.add(new UserViewInfo(HttpUtil.baseUrl + post.dataSources.get(i)));
            images.get(finalI).setVisibility(View.VISIBLE);
            HttpUtil.sendGetRequest(post.dataSources.get(i), null, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    InputStream inputStream = response.body().byteStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            images.get(finalI).setImageBitmap(bitmap);
                            // 设置高度与宽度相同
                            images.get(finalI).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    images.get(finalI).getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    ViewGroup.LayoutParams layoutParams = images.get(finalI).getLayoutParams();
                                    layoutParams.height = images.get(finalI).getWidth();
                                    images.get(finalI).setLayoutParams(layoutParams);
                                }
                            });
                            // 设置图片预览
                            images.get(finalI).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    GPreviewBuilder.from(activity).setData(userViewInfoList).setCurrentIndex(finalI).setSingleFling(true).setDrag(false).setType(GPreviewBuilder.IndicatorType.Number).start();
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void setAudio(PostAdapter.Post post) {
        if (postAdapter != null) {
            // list展示
            audioContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(activity, AudioService.class);
                    if (position == postAdapter.audioPlaying) {
                        // 暂停播放
                        postAdapter.audioPlaying = -1;
                        intent.putExtra("action", AudioService.ACTION_PAUSE);
                    } else {
                        // 播放当前位置的音频
                        postAdapter.audioPlaying = position;
                        intent.putExtra("action", AudioService.ACTION_START);
                        System.out.println(post.dataSources.get(0));
                        intent.putExtra("audio_src", post.dataSources.get(0));
                    }
                    activity.startService(intent);
                }
            });
        } else {
            // 动态详情当中
            audioContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(activity, AudioService.class);
                    switch (audioState) {
                        case PLAYING:
                            intent.putExtra("action", AudioService.ACTION_PAUSE);
                            audioState = PAUSING;
                            break;
                        case PAUSING:
                            intent.putExtra("action", AudioService.ACTION_START);
                            intent.putExtra("audio_src", post.dataSources.get(0));
                            audioState = PLAYING;
                            break;
                    }
                    activity.startService(intent);
                }
            });
        }

    }

    private void setLikeInfo(PostAdapter.Post post) {
        // 根据用户点赞与否设置点赞图标
        if (post.likeBy.contains(post.userId)) {
            likeIcon.setImageResource(R.drawable.ic_thumb_up_fill_20px);
        } else {
            likeIcon.setImageResource(R.drawable.ic_thumb_up_20px);
        }
        // 设置点赞数
        likeNum.setText(Integer.toString(post.likeBy.size()));
    }

}
