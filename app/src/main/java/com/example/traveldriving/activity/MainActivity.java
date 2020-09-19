package com.example.traveldriving.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.traveldriving.R;
import com.example.traveldriving.adapter.AdapterListDrivingLog;
import com.example.traveldriving.model.DrivingLog;
import com.example.traveldriving.model.MapPoint;
import com.example.traveldriving.service.MyService;
import com.example.traveldriving.utils.Tools;
import com.example.traveldriving.widget.LineItemDecoration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";
    //    private int mMeters = 0;
    private boolean isStarted = true;

    private TextView mDrivingTime;
    private TextView mDrivingDistance;
    private RecyclerView mRecyclerView;

    private Realm mRealm;
    private Geocoder mGeocoder;
    //    private TimerThread mStartTimerThread;
    private LocationManager mLocationManager;
    private AdapterListDrivingLog mAdapter;
    private static Handler mHandler;
    private BroadcastReceiver mapChangedBroadcastReceiver;
    private BroadcastReceiver timerBroadcastReceiver;
    private ActionModeCallback mActionModeCallback;
    private ActionMode mActionMode;

    private DrivingLog mDrivingLog;
    private List<MapPoint> mMapPoints;

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (mapChangedBroadcastReceiver == null) {
            mapChangedBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    MapPoint mapPoint = new MapPoint();
                    mapPoint.setLatitude((Double) intent.getExtras().get("latitude"));
                    mapPoint.setLongitude((Double) intent.getExtras().get("longitude"));
                    mapPoint.setCurrentDate((Date) intent.getExtras().get("date"));
                    mMapPoints.add(mapPoint);

                    int meter = (int) intent.getExtras().get("meter");
                    mDrivingDistance.setText(String.format("%d.%ckm", (meter / 1000), String.valueOf(meter % 1000).charAt(0)));
                }
            };
        }
        if (timerBroadcastReceiver == null) {
            timerBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int hour = (int) intent.getExtras().get("hour");
                    int minute = (int) intent.getExtras().get("minute");
                    int second = (int) intent.getExtras().get("second");
                    String time = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
                    mDrivingTime.setText(time);
                }
            };
        }

        registerReceiver(mapChangedBroadcastReceiver, new IntentFilter("location_update"));
        registerReceiver(timerBroadcastReceiver, new IntentFilter("timer_update"));
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        super.onDestroy();
        mRealm.close();
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Realm.init(this);
        mRealm = Realm.getDefaultInstance();

        initComponent();

        mMapPoints = new ArrayList<MapPoint>();

        ImageButton startBtn = findViewById(R.id.startBtn);
        mDrivingTime = findViewById(R.id.drivingTime);
        mDrivingDistance = findViewById(R.id.drivingDistance);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mGeocoder = new Geocoder(this);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStarted) {
                    Intent intent = new Intent(getApplicationContext(), MyService.class);
                    startService(intent);
                    Context context = getApplicationContext();
                    if (ActivityCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission.ACCESS_FINE_LOCATION)
                                && ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission.ACCESS_COARSE_LOCATION)) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION}, 100);
                            return;
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION}, 100);
                            return;
                        }

                    }

                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    mDrivingLog = new DrivingLog();
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    Date date = new Date(lastKnownLocation.getTime());

                    Number currentIdNum = mRealm.where(DrivingLog.class).max("id");
                    int nextId;
                    if (currentIdNum == null) {
                        nextId = 1;
                    } else {
                        nextId = currentIdNum.intValue() + 1;
                    }

                    mDrivingLog.setStartLatitude(latitude);
                    mDrivingLog.setStartLongitude(longitude);
                    mDrivingLog.setStartDate(date);
                    mDrivingLog.setId(nextId);
                    List<Address> list;

                    try {
                        list = mGeocoder.getFromLocation(mDrivingLog.getStartLatitude(), mDrivingLog.getStartLongitude(), 1);
                        if (list.size() > 0) {
                            Toast.makeText(MainActivity.this, mDrivingLog.getReadableLocation(context, true), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "출발", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "입출력 오류 - 서버에서 주소변환시 에러발생");
                    }

                    startBtn.setImageResource(R.drawable.btn_stop);
                } else {
                    Intent intent = new Intent(getApplicationContext(), MyService.class);
                    stopService(intent);

                    Toast.makeText(MainActivity.this, "종료", Toast.LENGTH_SHORT).show();
                    mDrivingTime.setText("00:00:00");
                    mDrivingDistance.setText("0.0km");
                    startBtn.setImageResource(R.drawable.btn_start);
                    mRealm.beginTransaction();
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    Date date = new Date(lastKnownLocation.getTime());
                    mDrivingLog.setStopLatitude(latitude);
                    mDrivingLog.setStopLongitude(longitude);
                    mDrivingLog.setStopDate(date);

                    DrivingLog newDrivingLog = mRealm.copyToRealm(mDrivingLog); // 비관리 객체를 영속화합니다

                    RealmList<MapPoint> newMapPoints = new RealmList<>();
                    for (int i = 0; i < mMapPoints.size(); i++) {
                        MapPoint newMapPoint = mRealm.createObject(MapPoint.class);
                        newMapPoint.setCurrentDate(mMapPoints.get(i).getCurrentDate());
                        newMapPoint.setLatitude(mMapPoints.get(i).getLatitude());
                        newMapPoint.setLongitude(mMapPoints.get(i).getLongitude());
                        newMapPoints.add(newMapPoint);
                    }

                    newDrivingLog.setMapPoints(newMapPoints);
                    mMapPoints = new ArrayList<MapPoint>();
                    newMapPoints = null;
                    mRealm.commitTransaction();
                }

                isStarted = !isStarted;
                System.out.println(isStarted);
            }
        });
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
