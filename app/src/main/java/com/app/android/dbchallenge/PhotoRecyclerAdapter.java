package com.app.android.dbchallenge;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class PhotoRecyclerAdapter extends RecyclerView.Adapter<PhotoRecyclerAdapter.ViewHolder>{
    int layout;
    Context context;
    LayoutInflater inflater;
    ArrayList<Photo> mPhotoList;

    public PhotoRecyclerAdapter(Context context, int resource, ArrayList<Photo> objects) {
        mPhotoList = objects;
        inflater = LayoutInflater.from(context);
        layout = resource;
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return mPhotoList.size();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(context).inflate(layout, viewGroup, false);
        ViewHolder pvh = new ViewHolder(v, context);
        return pvh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        viewHolder.fileNameTextView.setText(mPhotoList.get(i).getFileName());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView fileNameTextView;
        private Context mContext;

        ViewHolder(View itemView, Context context) {
            super(itemView);
            fileNameTextView = (TextView)itemView.findViewById(R.id.fileName);
            mContext = context;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            ((MainActivity)mContext).showPhoto(getPosition());
        }
    }
}
