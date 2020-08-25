package com.example.traveldriving.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.traveldriving.R;
import com.example.traveldriving.model.DrivingLog;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class AdapterListDrivingLog extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<DrivingLog> items = new ArrayList<>();
    private Context ctx;
    private SparseBooleanArray selected_items;
    private int current_selected_idx = -1;
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
        selected_items = new SparseBooleanArray();
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        public TextView stopTime;
        public TextView stopLocation;
        public TextView startTime;
        public TextView startLocation;
        public View lyt_parent;
        public CheckBox checkBox;

        public OriginalViewHolder(View v) {
            super(v);
            startTime = (TextView) v.findViewById(R.id.startTime);
            startLocation = (TextView) v.findViewById(R.id.startLocation);
            stopTime = (TextView) v.findViewById(R.id.stopTime);
            stopLocation = (TextView) v.findViewById(R.id.stopLocation);
            lyt_parent = (View) v.findViewById(R.id.lyt_parent);
            checkBox = (CheckBox) v.findViewById(R.id.selected_checkbox);
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
    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            OriginalViewHolder view = (OriginalViewHolder) holder;

            final DrivingLog drivingLog = items.get(position);
            view.startTime.setText(drivingLog.getStartDate().toString());
            view.startLocation.setText(drivingLog.getReadableLocation(ctx, true));
            view.stopTime.setText(drivingLog.getStopDate().toString());
            view.stopLocation.setText(drivingLog.getReadableLocation(ctx, false));
            view.lyt_parent.setActivated(selected_items.get(position, false));
            if(selected_items.get(position, false)){
                view.lyt_parent.setBackgroundColor(R.color.overlay_dark_20);
            }

        //            view.checkBox.setVisibility(View.VISIBLE);

            view.lyt_parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, items.get(position), position);
                    }
                }
            });

            view.lyt_parent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onMoreButtonClickListener == null) return false;
                    onMoreButtonClickListener.onItemClick(v, drivingLog, position);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, DrivingLog obj, int pos);
    }

    public interface OnMoreButtonClickListener {
        //                void onItemClick(View view, DrivingLog obj, MenuItem item);
        void onItemClick(View view, DrivingLog obj, int pos);
    }

    private void resetCurrentIndex() {
        current_selected_idx = -1;
    }

    public void removeData(int position) {
        items.get(0).deleteFromRealm();
        resetCurrentIndex();
    }

    public void toggleSelection(int pos) {
        current_selected_idx = pos;
        if (selected_items.get(pos, false)) {
            selected_items.delete(pos);
        } else {
            selected_items.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selected_items.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selected_items.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selected_items.size());
        for (int i = 0; i < selected_items.size(); i++) {
            items.add(selected_items.keyAt(i));
        }
        return items;
    }

    public DrivingLog getItem(int position) {
        return items.get(position);
    }

    public List<DrivingLog> getItems() {return items;}

}