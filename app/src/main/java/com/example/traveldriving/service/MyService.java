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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.traveldriving.R;
import com.example.traveldriving.activity.MainActivity;
import com.example.traveldriving.model.MapPoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyService extends Service {
    private int mSeconds = 0;
    private int mMeters = 0;

    private TimerThread mStartTimerThread;

    private static final String TAG = "MyService";

    class TimerThread extends Thread {
//        private Context context = getApplicationContext();
        private boolean stop = true;

        public void setStop(boolean stop) {
            mSeconds = 0;
            mMeters = 0;
            this.stop = stop;
        }


        @SuppressLint("MissingPermission")
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
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));

        startForeground(1, builder.build());

        mStartTimerThread = new TimerThread();
        mStartTimerThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mStartTimerThread != null) {
            mStartTimerThread.setStop(false);
            mStartTimerThread.interrupt();
            mStartTimerThread = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundService() {
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

