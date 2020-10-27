package com.example.traveldriving.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.traveldriving.R;
import com.example.traveldriving.activity.MainActivity;
import com.example.traveldriving.model.DrivingLog;
import com.example.traveldriving.model.MapPoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class MyService extends Service {
    private static final String NOTIFICATION_CHANNEL_ID = "channel1_ID";
    private static final String NOTIFICATION_CHANNEL_NAME = "channel1";
    private static final String TAG = "MyService";

    public int mSeconds = 0;

    private List<MapPoint> mMapPoints = null;
    private DrivingLog mDrivingLog = null;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private LocationListener mLocationListener;
    private LocationUpdateThread mLocationUpdateThread;

    private Realm mRealm;
    private final MyServiceBinder myServiceBinder = new MyServiceBinder();

    public boolean isDriving() {
        return mDrivingLog != null;
    }

    @SuppressLint("MissingPermission")
    public void startDriving() {
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        mDrivingLog = new DrivingLog();
        mMapPoints = new ArrayList<MapPoint>();
        mLocationUpdateThread = new LocationUpdateThread();
        mLocationUpdateThread.start();

        Number currentIdNum = mRealm.where(DrivingLog.class).max("id");
        Log.d(TAG, String.valueOf(currentIdNum));
        int nextId;
        if (currentIdNum == null) {
            nextId = 1;
        } else {
            nextId = currentIdNum.intValue() + 1;
        }
        mDrivingLog.setId(nextId);

        makeForegroundService();
    }

    public void stopDriving() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);

        if (mLocationUpdateThread != null) {
            mLocationUpdateThread.setStop(false);
            mLocationUpdateThread.interrupt();
            mLocationUpdateThread = null;
        }

        if (mMapPoints.size() > 0) {
            mRealm.beginTransaction();

            DrivingLog newDrivingLog = mRealm.copyToRealm(mDrivingLog);
            RealmList<MapPoint> newMapPoints = new RealmList<>();
            int length = mMapPoints.size();

            newDrivingLog.setStartLatitude(mMapPoints.get(0).getLatitude());
            newDrivingLog.setStartLongitude(mMapPoints.get(0).getLongitude());
            newDrivingLog.setStartDate(mMapPoints.get(0).getCurrentDate());

            for (int i = 0; i < length; i++) {
                MapPoint newMapPoint = mRealm.createObject(MapPoint.class);
                newMapPoint.setCurrentDate(mMapPoints.get(i).getCurrentDate());
                newMapPoint.setLatitude(mMapPoints.get(i).getLatitude());
                newMapPoint.setLongitude(mMapPoints.get(i).getLongitude());
                newMapPoints.add(newMapPoint);
            }

            newDrivingLog.setStopLatitude(mMapPoints.get(length - 1).getLatitude());
            newDrivingLog.setStopLongitude(mMapPoints.get(length - 1).getLongitude());
            newDrivingLog.setStopDate(mMapPoints.get(length - 1).getCurrentDate());

            newDrivingLog.setMapPoints(newMapPoints);
            mRealm.commitTransaction();
        }
        mMapPoints = null;
        mDrivingLog = null;
        mSeconds = 0;
        stopForeground(true);
    }

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Date date = new Date(location.getTime());

                MapPoint mapPoint = new MapPoint(latitude, longitude, date);
                mMapPoints.add(mapPoint);
                Log.d(TAG, String.valueOf(location.getLatitude()) + String.valueOf(location.getLongitude()));
            }
        };

        mFusedLocationProviderClient = new FusedLocationProviderClient(this);
        Realm.init(this);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myServiceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationListener != null) {
            mLocationListener = null;
        }
    }

    public class MyServiceBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }

    private void makeForegroundService() {
        NotificationManager notificationManager
                = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel notificationChannel
                    = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    importance);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        int notificationId = 1010;

        NotificationCompat.Builder builder
                = new NotificationCompat.Builder(getApplicationContext(),
                NOTIFICATION_CHANNEL_ID);

        builder.setSmallIcon(R.drawable.ic_monetization_on);
        builder.setContentTitle("운행기록 저장 중");
        builder.setContentText("운행기록 저장 중");

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent =
                PendingIntent.getActivity(getApplicationContext(), 1010, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        startForeground(notificationId, builder.build());
    }


    class LocationUpdateThread extends Thread {
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
                }


                mSeconds++;
                int mTempSecond = mSeconds;
                int hour = mTempSecond / 3600;
                mTempSecond -= hour * 3600;
                int minute = mTempSecond / 60;
                mTempSecond -= minute * 60;
                int second = mTempSecond;

                Intent intent = new Intent("timer_update");
                @SuppressLint("DefaultLocale")
                String time = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
                intent.putExtra("time", time);

                sendBroadcast(intent);
            }
        }
    }

}

