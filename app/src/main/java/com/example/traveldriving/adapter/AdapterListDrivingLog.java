package com.example.traveldriving.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;


import com.example.traveldriving.R;
import com.example.traveldriving.model.DrivingLog;

import java.util.ArrayList;
import java.util.List;

public class AdapterListDrivingLog extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<DrivingLog> items = new ArrayList<>();

    private Context ctx;
    private OnItemClickListener mOnItemClickListener;
    private OnMoreButtonClickListener onMoreButtonClickListener;

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public void setOnMoreButtonClickListener(final OnMoreButtonClickListener onMoreButtonClickListener) {
        this.onMoreButtonClickListener = onMoreButtonClickListener;
    }

    public AdapterListDrivingLog(Context context, List<DrivingLog> items) {
        this.items = items;
        ctx = context;
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        public TextView stopTime;
        public TextView stopLocation;
        public TextView startTime;
        public TextView startLocation;
        public View lyt_parent;

        public OriginalViewHolder(View v) {
            super(v);
            startTime = (TextView) v.findViewById(R.id.startTime);
            startLocation = (TextView) v.findViewById(R.id.startLocation);
            stopTime = (TextView) v.findViewById(R.id.stopTime);
            stopLocation = (TextView) v.findViewById(R.id.stopLocation);
            lyt_parent = (View) v.findViewById(R.id.lyt_parent);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driving_logs, parent, false);
        vh = new OriginalViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            OriginalViewHolder view = (OriginalViewHolder) holder;

            final DrivingLog drivingLog = items.get(position);
            if(drivingLog.getStartDate() != null) {
                view.startTime.setText(drivingLog.getStartDate().toString());
                view.startLocation.setText(drivingLog.getReadableLocation(ctx, true));
                view.stopTime.setText(drivingLog.getStopDate().toString());
                view.stopLocation.setText(drivingLog.getReadableLocation(ctx, false));
            }

//            view.lyt_parent.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (mOnItemClickListener != null) {
//                        mOnItemClickListener.onItemClick(view, items.get(position), position);
//                    }
//                }
//            });

//            view.more.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (onMoreButtonClickListener == null) return;
//                    onMoreButtonClick(view, p);
//                }
//            });
        }
    }

//    private void onMoreButtonClick(final View view, final DrivingLog p) {
//        PopupMenu popupMenu = new PopupMenu(ctx, view);
//        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                onMoreButtonClickListener.onItemClick(view, p, item);
//                return true;
//            }
//        });
//        popupMenu.inflate(R.menu.menu_song_more);
//        popupMenu.show();
//    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, DrivingLog obj, int pos);
    }

    public interface OnMoreButtonClickListener {
        void onItemClick(View view, DrivingLog obj, MenuItem item);
    }

}