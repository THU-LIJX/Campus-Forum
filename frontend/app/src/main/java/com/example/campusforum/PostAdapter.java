package com.example.campusforum;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.circularreveal.CircularRevealGridLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.previewlibrary.GPreviewBuilder;
import com.previewlibrary.enitity.IThumbViewInfo;
import com.previewlibrary.loader.IZoomMediaLoader;
import com.previewlibrary.loader.MySimpleTarget;

import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    static class Post implements Serializable {
        static final int TEXT_TYPE = 1;
        static final int IMAGE_TYPE = 2;
        static final int AUDIO_TYPE = 4;
        static final int VIDEO_TYPE = 8;

        public int postId;
        public int userId;
        public int type;
        public String content;
        public List<String> dataSources;
        public List<Integer> likeBy;
        public int likeNum;
        public String location;

        public Post(int postId, int userId, int type, String content, List<String> dataSources, List<Integer> likeBy, int likeNum) {
            this.postId = postId;
            this.userId = userId;
            this.type = type;
            this.content = content;
            this.dataSources = dataSources;
            this.likeBy = likeBy;
            this.likeNum = likeNum;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private PostItemView postItemView;
        public ViewHolder(View view) {
            super(view);
            this.postItemView = (PostItemView) view;
        }

        public void setPost(Post post) {
            postItemView.setPost(post);
        }

        public void setPosition(int position) {
            postItemView.setPosition(position);
        }

        public void setCommentAction(View.OnClickListener callback) {
            postItemView.setCommentAction(callback);
        }
    }

    private List<Post> mPostList;
    private Fragment mFragment;
    public int audioPlaying = -1;      // 当前播放中的音频

    public PostAdapter(List<Post> postList, Fragment fragment) {
        mPostList = postList;
        mFragment = fragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PostItemView view = new PostItemView(parent.getContext());
        view.setActivity(mFragment.getActivity());
        view.setAdapter(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostAdapter.ViewHolder viewHolder, int position) {
        Post post = mPostList.get(position);
        viewHolder.setPosition(position);
        viewHolder.setPost(post);
        viewHolder.setCommentAction(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mFragment.getActivity(), PostInfoActivity.class);
                intent.putExtra("post", post);
                mFragment.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPostList.size();
    }

}

class UserViewInfo implements IThumbViewInfo {
    //图片地址
    private String url;
    // 记录坐标
    private Rect mBounds;
    private String user = "用户字段";
    private String videoUrl;

    public UserViewInfo(String url) {
        this.url = url;
    }
    public UserViewInfo(String videoUrl,String url) {
        this.url = url;
        this.videoUrl = videoUrl;
    }
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getUrl() {//将你的图片地址字段返回
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public Rect getBounds() {//将你的图片显示坐标字段返回
        return mBounds;
    }

    @Nullable
    @Override
    public String getVideoUrl() {
        return videoUrl;
    }

    public void setBounds(Rect bounds) {
        mBounds = bounds;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeParcelable(this.mBounds, flags);
        dest.writeString(this.user);
        dest.writeString(this.videoUrl);
    }

    protected UserViewInfo(Parcel in) {
        this.url = in.readString();
        this.mBounds = in.readParcelable(Rect.class.getClassLoader());
        this.user = in.readString();
        this.videoUrl = in.readString();
    }

    public static final Creator<UserViewInfo> CREATOR = new Creator<UserViewInfo>() {
        @Override
        public UserViewInfo createFromParcel(Parcel source) {
            return new UserViewInfo(source);
        }

        @Override
        public UserViewInfo[] newArray(int size) {
            return new UserViewInfo[size];
        }
    };

}

class TestImageLoader implements IZoomMediaLoader {
    @Override
    public void displayImage(@NonNull Fragment context, @NonNull String path, final ImageView imageView, @NonNull final MySimpleTarget simpleTarget) {
        Glide.with(context)
                .asBitmap()
                .load(path)
                .apply(new RequestOptions().fitCenter())
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        simpleTarget.onResourceReady();
                        imageView.setImageBitmap(resource);
                    }
                });
    }

    @Override
    public void displayGifImage(@NonNull Fragment context, @NonNull String path, ImageView imageView, @NonNull final MySimpleTarget simpleTarget) {
        Glide.with(context)
                .asGif()
                .load(path)
                //可以解决gif比较几种时 ，加载过慢  //DiskCacheStrategy.NONE
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).dontAnimate())
                //去掉显示动画
                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        simpleTarget.onResourceReady();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        simpleTarget.onLoadFailed(null);
                        return false;
                    }

                })
                .into(imageView);
    }

    @Override
    public void onStop(@NonNull Fragment context) {
        Glide.with(context).onStop();
    }

    @Override
    public void clearMemory(@NonNull Context c) {
        Glide.get(c).clearMemory();
    }

}