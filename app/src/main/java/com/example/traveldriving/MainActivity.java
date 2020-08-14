package com.example.traveldriving;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";
    private boolean isStarted = true;
    private int cnt = 0;
    TextView mDrivingTime;
    int mSeconds = 0;
    TimerThread mStartTimerThread;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton startBtn = findViewById(R.id.startBtn);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStarted) {
                    startBtn.setImageResource(R.drawable.my_main_stop_button);
                    mStartTimerThread = new TimerThread();
                    mStartTimerThread.start();
                } else {
                    startBtn.setImageResource(R.drawable.my_main_start_button);
                    mStartTimerThread.setStop(false);
                    mStartTimerThread.interrupt();
//                    mStartTimerThread = null;
                }
                isStarted = !isStarted;
                System.out.println(isStarted);
            }
        });

        mDrivingTime = findViewById(R.id.drivingTime);


        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.ACCESS_FINE_LOCATION)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, permission.ACCESS_COARSE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            }

        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        TextView locationText = (TextView) findViewById(R.id.locationText);


        // 수동으로 위치 구하기
        String locationProvider = LocationManager.GPS_PROVIDER;
        assert locationManager != null;
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation != null) {
            double latitude = lastKnownLocation.getLatitude();
            double longitude = lastKnownLocation.getLongitude();
            locationText.setText("longtitude=" + longitude + ", latitude=" + latitude);
        }

        locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Log.d(TAG, "longtitude=" + longitude + ", latitude=" + latitude);
                locationText.setText("longtitude=" + longitude + ", latitude=" + latitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
//                locationText.setText("onStatusChanged");

            }

            @Override
            public void onProviderEnabled(String provider) {
//                locationText.setText("onProviderEnabled");

            }

            @Override
            public void onProviderDisabled(String provider) {
//                locationText.setText("onProviderDisabled");

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
//                    break;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSeconds++;
                        int mTempSecond = mSeconds;
                        int hour = mTempSecond / 3600;
                        mTempSecond -= hour * 3600;
                        int minute = mTempSecond / 60;
                        mTempSecond -= minute * 60;
                        int second = mTempSecond;


                        String time = String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second);
                        Log.d("@@", time);
                        mDrivingTime.setText(String.valueOf(time));
                    }
                });
            }
        }
    }
}
