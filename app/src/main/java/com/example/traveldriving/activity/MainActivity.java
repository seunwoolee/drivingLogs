package com.example.traveldriving.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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


@RequiresApi(api = Build.VERSION_CODES.Q)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AppCompatActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 200;
    private static final String[] needPermissions = {
            permission.ACCESS_COARSE_LOCATION,
            permission.ACCESS_FINE_LOCATION,
            permission.ACCESS_BACKGROUND_LOCATION,
    };

    private RecyclerView mRecyclerView;
    private Realm mRealm;

    private AdapterListDrivingLog mAdapter;
    private ActionModeCallback mActionModeCallback;
    private ActionMode mActionMode;

    private boolean mIsBound = false;
    private MyService mMyService;

    ImageButton mStartBtn;
    TextView mDrivingTime;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBound = true;
            MyService.MyServiceBinder binder = (MyService.MyServiceBinder) service;
            mMyService = binder.getService();
            if (mMyService.isDriving()) {
                mStartBtn.setImageResource(R.drawable.btn_stop);

//                int mTempSecond = mMyService.mSeconds;
//                int hour = mTempSecond / 3600;
//                mTempSecond -= hour * 3600;
//                int minute = mTempSecond / 60;
//                mTempSecond -= minute * 60;
//                int second = mTempSecond;
//
//                String time = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
//                mDrivingTime.setText(time);

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
        for (String permission : needPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        needPermissions,
                        REQUEST_LOCATION_PERMISSION);
            }
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
        boolean permissionToRecordAccepted = true;

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionToRecordAccepted = false;
                    break;
                }
            }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartBtn = findViewById(R.id.startBtn);
        mDrivingTime = findViewById(R.id.drivingTime);

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
