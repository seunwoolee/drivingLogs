package com.example.traveldriving.service;

import android.Manifest;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.traveldriving.R;
import com.example.traveldriving.activity.MainActivity;
import com.example.traveldriving.model.MapPoint;

import java.util.ArrayList;
import java.util.Date;

public class MyService extends Service {
    private int mSeconds = 0;
    private int mMeters = 0;

    private TimerThread mStartTimerThread;

    private static final String TAG = "MyService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private Location mPreviousLocation;

    class TimerThread extends Thread {

        private boolean stop = true;

        public void setStop(boolean stop) {
            mSeconds = 0;
            mMeters = 0;
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

                Log.d(TAG, "서비스 실행 중(스레드 안에서)");

                mSeconds++;
                int mTempSecond = mSeconds;
                int hour = mTempSecond / 3600;
                mTempSecond -= hour * 3600;
                int minute = mTempSecond / 60;
                mTempSecond -= minute * 60;
                int second = mTempSecond;

                Intent intent = new Intent("timer_update");
                intent.putExtra("hour", hour);
                intent.putExtra("minute", minute);
                intent.putExtra("second", second);
                sendBroadcast(intent);
            }
        }
    }

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("포그라운드 실행중");
        builder.setContentText("포그라운드 서비스 실행");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));

        startForeground(1, builder.build());

        mStartTimerThread = new TimerThread();
        mStartTimerThread.start();

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mPreviousLocation == null) {
                    mPreviousLocation = location;
                }
                int distance = Math.round(location.distanceTo(mPreviousLocation));
                mMeters += distance;

                mPreviousLocation = location;
                Log.d(TAG, String.valueOf(distance));

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Date date = new Date(location.getTime());
                Intent intent = new Intent("location_update");
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("date", date);
                intent.putExtra("meter", mMeters);
                sendBroadcast(intent);
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
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Context context = getApplicationContext();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return START_STICKY_COMPATIBILITY;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        if (mStartTimerThread != null) {
            mStartTimerThread.setStop(false);
            mStartTimerThread.interrupt();
            mStartTimerThread = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundService(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("포그라운드 실행중");
        builder.setContentText("포그라운드 서비스 실행");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));

        startForeground(1, builder.build());
    }

}

