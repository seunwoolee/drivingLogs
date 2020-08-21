package com.example.traveldriving.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.traveldriving.R;
import com.example.traveldriving.activity.MainActivity;

public class DrivingService extends Service {

    private static final String TAG = DrivingService.class.getSimpleName();
    private MainActivity.TimerThread mStartTimerThread;
    private Thread mTread;
    private int mCount = 0;

    public DrivingService() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if("startForeground".equals(intent.getAction())){
            startForegroundService();
        }

        if(mTread == null) {
            mTread = new Thread("MY Thread"){
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        try{
                            mCount++;
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                        Log.d("MY Service", "서비스 동작 중" + mCount);
                    }
                }
            };
            mTread.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destory");
        if(mTread != null){
            mTread.interrupt();
            mTread = null;
            mCount = 0;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundService(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("포그라운드 서비스");
        builder.setContentText("포그라운드 서비스 실행 중");

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));

        startForeground(1, builder.build());
    }



}
