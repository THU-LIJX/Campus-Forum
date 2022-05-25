package com.example.campusforum;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotiAdapter extends RecyclerView.Adapter<NotiAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {
        public NotiItemView notiItemView;

        public ViewHolder(View view) {
            super(view);
            this.notiItemView = (NotiItemView) view;
        }
    }

    private List<Notification> mNotificationList;
    private Activity mActivity;
    private int unreadNum = 0;

    public NotiAdapter(List<Notification> notificationList, Activity activity) {
        mNotificationList = notificationList;
        mActivity = activity;
    }

    public void setUnreadNum(int unreadNum) {
        this.unreadNum = unreadNum;
    }

    @Override
    public NotiAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        NotiItemView view = new NotiItemView(parent.getContext());
        view.setActivity(mActivity);
        return new NotiAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotiAdapter.ViewHolder viewHolder, int position) {
        Notification notification = mNotificationList.get(position);
        viewHolder.notiItemView.setNoti(notification);
        if (position < unreadNum) {
            viewHolder.notiItemView.setState(false);
        } else {
            viewHolder.notiItemView.setState(true);
        }
    }

    @Override
    public int getItemCount() {
        return mNotificationList.size();
    }
}
