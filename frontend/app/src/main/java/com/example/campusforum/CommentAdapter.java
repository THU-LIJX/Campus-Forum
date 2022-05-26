package com.example.campusforum;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {
        public CommentItemView commentItemView;

        public ViewHolder(View view) {
            super(view);
            this.commentItemView = (CommentItemView) view;
        }
    }

    private List<Comment> mCommentList;
    private Activity mActivity;
    private PostItemView mPostItemView; // 添加/删除评论时需要更新评论数
    private PostAdapter.Post mPost;      // 添加/删除评论时需要更新评论数

    public CommentAdapter(List<Comment> commentList, Activity activity, PostItemView postItemView, PostAdapter.Post post) {
        mCommentList = commentList;
        mActivity = activity;
        mPostItemView = postItemView;
        mPost = post;
    }

    public void setCommentList(List<Comment> commentList) {
        mCommentList = commentList;
        mPost.comments = commentList;
        mPostItemView.refresh();
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CommentAdapter.this.notifyDataSetChanged();
            }
        });
    }

    @Override
    public CommentAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CommentItemView view = new CommentItemView(parent.getContext());
        view.setActivity(mActivity);
        view.setCommentAdapter(this);
        return new CommentAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CommentAdapter.ViewHolder viewHolder, int position) {
        Comment comment = mCommentList.get(position);
        viewHolder.commentItemView.setComment(comment);
    }

    @Override
    public int getItemCount() {
        return mCommentList.size();
    }
}
