package com.example.traveldriving.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.traveldriving.R;
import com.example.traveldriving.activity.MainActivity;
import com.example.traveldriving.model.DrivingLog;
import com.example.traveldriving.model.MapPoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

public class MyService extends Service {
    private static final String NOTIFICATION_CHANNEL_ID = "channel1_ID";
    private static final String NOTIFICATION_CHANNEL_NAME = "channel1";
    private static final String TAG = "MyService";
    private static final long INTERVAL_TIME = 5000;
    private static final long FASTEST_INTERVAL_TIME = 2000;

    private List<MapPoint> mMapPoints = null;
    private DrivingLog mDrivingLog = null;

    private Location lastKnownLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;


    private Realm mRealm;
    private final MyServiceBinder myServiceBinder = new MyServiceBinder();

    private void getLocation() {
        Context context = getApplicationContext();
        FusedLocationProviderClient fusedLocationProviderClient = new FusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                lastKnownLocation = location;
                Log.d(TAG, String.valueOf(location.getLatitude()));
            }
        });
    }

    private void startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL_TIME);
        locationRequest.setFastestInterval(FASTEST_INTERVAL_TIME);

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        Location location = locationResult.getLastLocation();
                        Log.d(TAG, String.valueOf(location.getLatitude()));
                    }
                }, null);
    }

    public void stopLocationUpdates() {
        if (mFusedLocationProviderClient != null) {
            try {
                LocationCallback locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Log.d(TAG, "dd");
                    }
                    public void onLocationAvailability(LocationAvailability var1) {
                        Log.d(TAG, "bb");
                    }
                };

                final Task<Void> voidTask = mFusedLocationProviderClient.removeLocationUpdates(locationCallback);

            }
            catch (SecurityException exp) {
                Log.d(TAG, " Security exception while removeLocationUpdates");
            }
        }
    }


    public boolean isDriving() {
        return mDrivingLog != null;
    }

    @SuppressLint("MissingPermission")
    public void startDriving() {
        mDrivingLog = new DrivingLog();
        mMapPoints = new ArrayList<MapPoint>();
//        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 50, mLocationListener);
        startLocationUpdate();
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
//        mLocationManager.removeUpdates(mLocationListener);
        stopLocationUpdates();
        if (mMapPoints.size() > 0) {
            mRealm.beginTransaction();

            DrivingLog newDrivingLog = mRealm.copyToRealm(mDrivingLog); // 비관리 객체를 영속화합니다
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
        stopForeground(true);
    }

    public MyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Context context = getApplicationContext();

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Date date = new Date(location.getTime());

                MapPoint mapPoint = new MapPoint(latitude, longitude, date);
                mMapPoints.add(mapPoint);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        };

        mFusedLocationProviderClient = new FusedLocationProviderClient(context);


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


}

