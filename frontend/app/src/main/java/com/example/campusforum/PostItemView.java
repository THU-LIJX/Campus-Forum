package com.example.campusforum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SymbolTable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ShareCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.circularreveal.CircularRevealGridLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.makeramen.roundedimageview.RoundedImageView;
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

public class  PostItemView extends LinearLayoutCompat {

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
    private TextView userId;                           // 用户id
    private TextView postId;                           // 动态id
    private TextView title;                            // 标题
    private TextView content;                          // 正文
    private TextView location;                         // 位置
    private TextView commentNum;                       // 评论数
    private TextView likeNum;                          // 点赞数
    private ShapeableImageView commentIcon;            // 评论图标
    private ShapeableImageView likeIcon;               // 点赞图标
    private ShapeableImageView shareIcon;              // 分享图标
    private RoundedImageView avatar;                   // 头像
    private MaterialButton funcBtn;                    // 关注/取消关注按钮

    private List<GridLayout.LayoutParams> imageParams;  // 保存参数

    private Activity activity;                         // view所在的activity
    private PostAdapter postAdapter;                   // view所在的adapter(如果使用list展示)
    private Integer position;                          // view的位置(如果使用list展示)
    private String audioState = PAUSING;               // audio状态
    private PostAdapter.Post post;                     // post

    static private final String PLAYING = "PLAYING";
    static private final String PAUSING = "PAUSING";

    // 初始化控件
    private void initView(Context context) {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setLayoutDirection(VERTICAL);
        this.setLayoutParams(layoutParams);
        View view = LayoutInflater.from(context).inflate(R.layout.post_item, this);
        gridLayoutImages = (CircularRevealGridLayout) view.findViewById(R.id.post_item_images);
        videoContainer = (LinearLayoutCompat) view.findViewById(R.id.post_item_video_container);
        videoPlayer = (JCVideoPlayerStandard) view.findViewById(R.id.post_item_video);
        audioContainer = (MaterialCardView) view.findViewById(R.id.post_item_audio);
        username = (TextView) view.findViewById(R.id.post_item_user_name);
        userId = (TextView) view.findViewById(R.id.post_item_user_id);
        postId = (TextView) view.findViewById(R.id.post_item_post_id);
        title = (TextView) view.findViewById(R.id.post_item_title);
        content = (TextView) view.findViewById(R.id.post_item_content);
        location = (TextView) view.findViewById(R.id.post_item_location);
        commentNum = (TextView) view.findViewById(R.id.post_item_comment_num);
        likeNum = (TextView) view.findViewById(R.id.post_item_like_num);
        commentIcon = (ShapeableImageView) view.findViewById(R.id.post_item_comment_icon);
        likeIcon = (ShapeableImageView) view.findViewById(R.id.post_item_like_icon);
        shareIcon = (ShapeableImageView) view.findViewById(R.id.post_item_share_icon);
        avatar = (RoundedImageView) view.findViewById(R.id.post_item_avatar);
        funcBtn = (MaterialButton) view.findViewById(R.id.post_item_func_btn);

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

        imageParams = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            imageParams.add(new GridLayout.LayoutParams(images.get(i).getLayoutParams()));
        }
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
        this.post = post;
        // 根据Post类型显示不同组件
        switch (post.type) {
            case PostAdapter.Post.IMAGE_TYPE:
                gridLayoutImages.setVisibility(View.VISIBLE);
                videoContainer.setVisibility(View.GONE);
                audioContainer.setVisibility(View.GONE);
                setImages(post);
                break;
            case PostAdapter.Post.VIDEO_TYPE:
                gridLayoutImages.setVisibility(View.GONE);
                videoContainer.setVisibility(View.VISIBLE);
                audioContainer.setVisibility(View.GONE);
                videoPlayer.setUp(HttpUtil.baseUrl + post.dataSources.get(0), JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL, "");
                break;
            case PostAdapter.Post.AUDIO_TYPE:
                gridLayoutImages.setVisibility(View.GONE);
                videoContainer.setVisibility(View.GONE);
                audioContainer.setVisibility(View.VISIBLE);
                setAudio(post);
                break;
            case PostAdapter.Post.TEXT_TYPE:
                gridLayoutImages.setVisibility(View.GONE);
                videoContainer.setVisibility(View.GONE);
                audioContainer.setVisibility(View.GONE);
                break;
        }

        username.setText(post.username);                            // 设置用户名
        userId.setText("@" + Integer.toString(post.userId));              // 设置用户id
        postId.setText("#" + post.postId);                     // 设置动态id
        title.setText(post.title);                                  // 设置标题
        content.setText(post.content);                              // 设置正文
        location.setText(post.location);                            // 设置位置
        commentNum.setText(Integer.toString(post.comments.size())); // 设置评论数
        setLikeInfo(post);
        // 如果位置为空，则不显示
        if (post.location.equals("")) {
            location.setVisibility(GONE);
        } else {
            location.setVisibility(VISIBLE);
        }

        // 点赞/取消点赞
        likeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> data = new HashMap<>();
                data.put("blog", Integer.toString(post.postId));
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
                                Log.d("fuck", "here");
                                JSONArray jsonArray = jsonOb.getJSONArray("likedby");
                                List<Integer> tmpList = new ArrayList<>();
                                for (int i = 0; i < jsonArray.length(); i++) tmpList.add(jsonArray.getInt(i));
                                post.likedBy = tmpList;
                                setLikeInfo(post);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                if (post.likedBy.contains(post.userId)) {
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
                String txt = post.content;
                String mimeType = "text/plain";
                ShareCompat.IntentBuilder
                        .from(activity)
                        .setType(mimeType)
                        .setChooserTitle("分享到")
                        .setText(txt)
                        .startChooser();
            }
        });

        // 点击头像跳转到相应的个人主页
        avatar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, UserActivity.class);
                intent.putExtra("id", post.userId);
                activity.startActivity(intent);
            }
        });

        // 设置头像
        if (post.avatar.equals("")) {
            // 如果用户没有设置头像，则使用默认头像
            avatar.setImageResource(R.drawable.ranga);
        } else {
            // 获取用户的头像
            HttpUtil.sendGetRequest(post.avatar, null, new Callback() {
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
                            avatar.setImageBitmap(bitmap);
                        }
                    });
                }
            });
        }

        // 设置关注/取消关注按钮
        setFuncBtn(post);
    }

    private void setImages(PostAdapter.Post post) {
        // 行列数，当图片数为4时是2x2的，否则每行3x3，即使没有足够的图片
        int rowNum;
        int colNum;
        if (post.dataSources.size() == 4) {
            rowNum = 2;
            colNum = 2;
        }
        else {
            colNum = 3;
            rowNum = (int) Math.ceil((double) post.dataSources.size() / 3.0);
        }
        gridLayoutImages.removeAllViews();
        gridLayoutImages.setColumnCount(colNum);
        gridLayoutImages.setRowCount(rowNum);
        for (int i = 0; i < rowNum * colNum; i++) {
            gridLayoutImages.addView(images.get(i));
            images.get(i).setVisibility(View.GONE);
        }

        // 图片预览
        List<UserViewInfo> userViewInfoList = new ArrayList<>();
        for (int i = 0; i < post.dataSources.size(); i++) {
            int finalI = i;
            userViewInfoList.add(new UserViewInfo(HttpUtil.baseUrl + post.dataSources.get(i)));
            images.get(finalI).setVisibility(View.VISIBLE);
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
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 根据用户点赞与否设置点赞图标
                Log.d("hello", Integer.toString(User.currentUser.userId));
                if (post.likedBy.contains(User.currentUser.userId)) {
                    likeIcon.setImageResource(R.drawable.ic_thumb_up_fill_20px);
                } else {
                    likeIcon.setImageResource(R.drawable.ic_thumb_up_20px);
                }
                // 设置点赞数
                likeNum.setText(Integer.toString(post.likedBy.size()));
            }
        });
    }

    private void setFuncBtn(PostAdapter.Post post) {
        // 已关注
        if (User.currentUser.subscriptions.contains(post.userId)) {
            funcBtn.setText("取关");
            funcBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onFollowedClick(post);
                }
            });
        } else {
            funcBtn.setText("关注");
            funcBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onFollowClick(post);
                }
            });
        }
    }

    private void onFollowClick(PostAdapter.Post post) {
        funcBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onFollowedClick(post);
            }
        });
        funcBtn.setText("取关");
        HashMap<String, String> data = new HashMap<>();
        data.put("id", Integer.toString(post.userId));
        HttpUtil.sendPostRequest("/api/user/subscribe", data, new Callback() {
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
                        User.currentUser.subscriptions.add(post.userId);
                        System.out.println(User.currentUser.subscriptions);
                    }
                    if (postAdapter != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onFollowedClick(PostAdapter.Post post) {
        funcBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onFollowClick(post);
            }
        });
        funcBtn.setText("关注");
        HashMap<String, String> data = new HashMap<>();
        data.put("id", Integer.toString(post.userId));
        HttpUtil.sendPostRequest("/api/user/unsubscribe", data, new Callback() {
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
                        User.currentUser.subscriptions.remove(Integer.valueOf(post.userId));
                        System.out.println(User.currentUser.subscriptions);
                    }
                    if (postAdapter != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                postAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void refresh() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commentNum.setText(Integer.toString(post.comments.size()));
            }
        });
    }
}
