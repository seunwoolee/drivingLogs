package com.example.traveldriving.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.traveldriving.R;
import com.example.traveldriving.adapter.AdapterListDrivingLog;
import com.example.traveldriving.model.DrivingLog;
import com.example.traveldriving.model.MapPoint;
import com.example.traveldriving.service.MyService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";
    private int mSeconds = 0;
    private int mMeters = 0;
    private boolean isStarted = true;

    private TextView mDrivingTime;
    private TextView mDrivingDistance;
    private RecyclerView mRecyclerView;

    private Realm mRealm;
    private Geocoder mGeocoder;
    private TimerThread mStartTimerThread;
    private LocationManager mLocationManager;
    private AdapterListDrivingLog mAdapter;
    private static Handler mHandler;
    private BroadcastReceiver broadcastReceiver;

    private DrivingLog mDrivingLog;
    private List<MapPoint> mMapPoints;

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    MapPoint mapPoint = new MapPoint();
                    mapPoint.setLatitude((Double) intent.getExtras().get("latitude"));
                    mapPoint.setLongitude((Double) intent.getExtras().get("longitude"));
                    mapPoint.setCurrentDate((Date) intent.getExtras().get("date"));
                    mMapPoints.add(mapPoint);

                    int distance = (int) intent.getExtras().get("distance");
                    mMeters += distance;

                    Log.d(TAG, String.valueOf(mMeters));
                    mDrivingDistance.setText(String.format("%d.%ckm", (mMeters / 1000), String.valueOf(mMeters % 1000).charAt(0)));
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));
    }

    @Override
    protected void onDestroy() {
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
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mSeconds++;
                int mTempSecond = mSeconds;
                int hour = mTempSecond / 3600;
                mTempSecond -= hour * 3600;
                int minute = mTempSecond / 60;
                mTempSecond -= minute * 60;
                int second = mTempSecond;

                String time = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
                mDrivingTime.setText(time);
            }
        };

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
                    mStartTimerThread = new TimerThread();
                    mStartTimerThread.start();
                } else {
                    Intent intent = new Intent(getApplicationContext(), MyService.class);
                    stopService(intent);
                    Context context = getApplicationContext();
                    Toast.makeText(MainActivity.this, "종료", Toast.LENGTH_SHORT).show();

                    startBtn.setImageResource(R.drawable.btn_start);
                    mStartTimerThread.setStop(false);
                    mStartTimerThread.interrupt();
                    mStartTimerThread = null;

                    mSeconds = 0;
                    mMeters = 0;
                    mDrivingTime.setText("00:00:00");
                    mDrivingDistance.setText("0.0km");
                    mRealm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
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
                            double latitude = lastKnownLocation.getLatitude();
                            double longitude = lastKnownLocation.getLongitude();
                            Date date = new Date(lastKnownLocation.getTime());
                            mDrivingLog.setStopLatitude(latitude);
                            mDrivingLog.setStopLongitude(longitude);
                            mDrivingLog.setStopDate(date);

                            DrivingLog newDrivingLog = realm.copyToRealm(mDrivingLog); // 비관리 객체를 영속화합니다

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
                        }
                    });

                }
                isStarted = !isStarted;
                System.out.println(isStarted);
            }
        });
    }

    private void initComponent() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);

        List<DrivingLog> items = mRealm.where(DrivingLog.class).findAll();

        //set data and list adapter
        mAdapter = new AdapterListDrivingLog(this, items);
        mRecyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterListDrivingLog.OnItemClickListener() {
            @Override
            public void onItemClick(View view, DrivingLog obj, int pos) {
                DrivingLog drivingLog = items.get(pos);
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                intent.putExtra("drivingLogId", drivingLog.getId());
                startActivity(intent);
            }

        });
    }


    class TimerThread extends Thread {

        private boolean stop = true;

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run() {
            while (stop) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                mHandler.sendEmptyMessage(0);
            }
        }
    }


}
