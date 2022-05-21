package com.example.campusforum;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SubAdapter extends RecyclerView.Adapter<SubAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {
        public SubItemView subItemView;

        public ViewHolder(View view) {
            super(view);
            this.subItemView = (SubItemView) view;
        }
    }

    private List<Integer> mSubList;
    private Activity mActivity;

    public SubAdapter(List<Integer> subList, Activity activity) {
        mSubList = subList;
        mActivity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SubItemView view = new SubItemView(parent.getContext());
        view.setActivity(mActivity);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SubAdapter.ViewHolder viewHolder, int position) {
        int userId = mSubList.get(position);
        viewHolder.subItemView.setSub(userId);
    }

    @Override
    public int getItemCount() {
        return mSubList.size();
    }
}
