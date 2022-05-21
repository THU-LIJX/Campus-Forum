package com.example.campusforum;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.DraftCardHolder> {
    private static final String TAG = "DraftAdapter";
    private final LayoutInflater inflater;
    private final List<JSONObject> draftList;
    Context mContext;
    public DraftAdapter(Context context,List<JSONObject>list){
        inflater=LayoutInflater.from(context);
        mContext=context;
        draftList=list;
    }
    @NonNull
    @Override
    public DraftCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView=inflater.inflate(R.layout.draft_item,parent,false);
        return new DraftCardHolder(itemView,this);
    }

    @Override
    public void onBindViewHolder(@NonNull DraftCardHolder holder, int position) {
        try {
            String text= draftList.get(position).getString("text");
            holder.textView.setText(text);
            Log.d(TAG, "onBindViewHolder: position:"+position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");

                Intent intent=new Intent(mContext,PostEditActivity.class);

                intent.putExtra("draft",draftList.get(holder.getLayoutPosition()).toString());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return draftList.size();
    }

    class DraftCardHolder extends RecyclerView.ViewHolder{
        public final TextView textView;
        final DraftAdapter adapter;
        CardView cardView;
        public DraftCardHolder(@NonNull View itemView,DraftAdapter adapter) {
            super(itemView);
            textView=itemView.findViewById(R.id.draft_text);
            cardView=itemView.findViewById(R.id.draft_card);
            this.adapter=adapter;
        }
    }
}
