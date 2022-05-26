package com.example.campusforum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Parcel;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.previewlibrary.enitity.IThumbViewInfo;
import com.previewlibrary.loader.IZoomMediaLoader;
import com.previewlibrary.loader.MySimpleTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    static class Post implements Serializable {
        static final int TEXT_TYPE = 1;
        static final int IMAGE_TYPE = 2;
        static final int AUDIO_TYPE = 4;
        static final int VIDEO_TYPE = 8;

        public int postId;
        public int userId;
        public int type;
        public String title;
        public String content;
        public int likeNum;
        public String location;
        public String username;
        public String avatar;
        public List<String> dataSources;
        public List<Integer> likedBy;
        public List<Comment> comments;

        public Post(JSONObject jsonObject) {
            try {
                postId = jsonObject.getInt("id");
                userId = jsonObject.getInt("user");
                type = jsonObject.getInt("type");
                title = jsonObject.getString("title");
                content = jsonObject.getString("text");
                location = jsonObject.getString("location");
                username = jsonObject.getString("user_name");
                avatar = jsonObject.getString("avatar");
                likeNum = jsonObject.getInt("liked");

                dataSources = new ArrayList<>();
                JSONArray jsonArray = jsonObject.getJSONArray("src");
                for (int i = 0; i < jsonArray.length(); i++) dataSources.add(jsonArray.getString(i));

                likedBy = new ArrayList<>();
                jsonArray = jsonObject.getJSONArray("likedby");
                for (int i = 0; i < jsonArray.length(); i++) likedBy.add(jsonArray.getInt(i));

                comments = new ArrayList<>();
                jsonArray = jsonObject.getJSONArray("comment");
                for (int i = 0; i < jsonArray.length(); i++) comments.add(new Comment(jsonArray.getJSONObject(i)));

            } catch (JSONException e) {
                e.printStackTrace();
            }
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

        public void setPostItemView(PostItemView postItemView) {
            this.postItemView = postItemView;
        }
    }

    private List<Post> mPostList;
    private Activity mActivity;
    public int audioPlaying = -1;      // 当前播放中的音频

    public PostAdapter(List<Post> postList, Activity activity) {
        mPostList = postList;
        mActivity = activity;
    }

    int count = 0;
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PostItemView view = new PostItemView(parent.getContext());
        view.setActivity(mActivity);
        view.setAdapter(this);
        count += 1;
        System.out.println("create viewholder" + count);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostAdapter.ViewHolder viewHolder, int position) {
        Post post = mPostList.get(position);
        System.out.println("bind post:" + post.postId);
        viewHolder.setPosition(position);
        viewHolder.setPost(post);
        viewHolder.setCommentAction(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mActivity, PostInfoActivity.class);
                intent.putExtra("post", post);
                mActivity.startActivity(intent);
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