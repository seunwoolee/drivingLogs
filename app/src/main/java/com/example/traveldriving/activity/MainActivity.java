package com.example.traveldriving.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.traveldriving.BuildConfig;
import com.example.traveldriving.R;
import com.example.traveldriving.adapter.AdapterListDrivingLog;
import com.example.traveldriving.model.DrivingLog;
import com.example.traveldriving.service.MyService;
import com.example.traveldriving.utils.Tools;
import com.example.traveldriving.widget.LineItemDecoration;

import java.util.List;

import io.realm.Realm;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AppCompatActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 200;

    private RecyclerView mRecyclerView;

    private Realm mRealm;

    private AdapterListDrivingLog mAdapter;
    private ActionModeCallback mActionModeCallback;
    private ActionMode mActionMode;

    private boolean mIsBound = false;
    private MyService mMyService;

    ImageButton mStartBtn;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBound = true;
            MyService.MyServiceBinder binder = (MyService.MyServiceBinder) service;
            mMyService = binder.getService();
            if (mMyService.isDriving()) {
                mStartBtn.setImageResource(R.drawable.btn_stop);
            } else {
                mStartBtn.setImageResource(R.drawable.btn_start);
            }

            mStartBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mMyService.isDriving()) {
                        mMyService.startDriving();
                        mStartBtn.setImageResource(R.drawable.btn_stop);
                    } else {
                        mMyService.stopDriving();
                        mStartBtn.setImageResource(R.drawable.btn_start);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
        }
    };

    public void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mIsBound) {
            mIsBound = false;
            unbindService(serviceConnection);
        }
        mRealm.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionToRecordAccepted = false;
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) {
            Toast.makeText(MainActivity.this, "권한이 거부되었습니다. 권한을 승인해주세요.", Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.setAction(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }, 1500);

        }
    }


    @SuppressLint({"SetTextI18n", "MissingPermission"})
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartBtn = findViewById(R.id.startBtn);

        Realm.init(this);
        mRealm = Realm.getDefaultInstance();

        initComponent();

        requestPermission();

        Intent intent = new Intent(this, MyService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initComponent() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new LineItemDecoration(this, LinearLayout.VERTICAL));
        mRecyclerView.setHasFixedSize(true);

        List<DrivingLog> items = mRealm.where(DrivingLog.class).findAll();

        mAdapter = new AdapterListDrivingLog(this, items);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnCheckBoxClickListener(new AdapterListDrivingLog.OnCheckboxClickListener() {
            @Override
            public void onItemClick(int pos) {
                toggleSelection(pos);
            }
        });

        mAdapter.setOnItemClickListener(new AdapterListDrivingLog.OnItemClickListener() {
            @Override
            public void onItemClick(View view, DrivingLog obj, int pos) {
                if (mAdapter.getSelectedItemCount() > 0) {
                    enableActionMode(pos);
                } else {
                    DrivingLog drivingLog = items.get(pos);
                    Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                    intent.putExtra("drivingLogId", drivingLog.getId());
                    startActivity(intent);
                }

            }
        });

        mAdapter.setOnMoreButtonClickListener(new AdapterListDrivingLog.OnMoreButtonClickListener() {
            @Override
            public void onItemClick(View view, DrivingLog obj, int pos) {
                enableActionMode(pos);
            }

        });

        mActionModeCallback = new ActionModeCallback();

    }

    private void enableActionMode(int position) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModeCallback);

        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mActionMode.finish();
        } else {
            mActionMode.invalidate();
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mAdapter.setActionMode(true);
            mAdapter.notifyDataSetChanged();
            Tools.setSystemBarColor(MainActivity.this, R.color.blue_grey_700);
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                deleteInboxes();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            mActionMode = null;
            mAdapter.setActionMode(false);
            mAdapter.notifyDataSetChanged();
            Tools.setSystemBarColor(MainActivity.this, R.color.colorPrimary);
        }
    }

    private void deleteInboxes() {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        List<DrivingLog> drivingLogs = mAdapter.getItems();

        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            int selectedPos = selectedItemPositions.get(i);
            mRealm.beginTransaction();
            drivingLogs.get(selectedPos).deleteFromRealm();
            mRealm.commitTransaction();
            mAdapter.resetCurrentIndex();
        }
        mAdapter.notifyDataSetChanged();
    }


}
