package com.example.traveldriving.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.traveldriving.model.MapPoint;

import java.util.Date;

public class MyService extends Service {
    private static final String TAG = "MyService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private Location mPreviousLocation;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mPreviousLocation == null) {
                    mPreviousLocation = location;
                }
                int distance = Math.round(location.distanceTo(mPreviousLocation));
                mPreviousLocation = location;
                Log.d(TAG, String.valueOf(distance));

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Date date = new Date(location.getTime());
                Intent intent = new Intent("location_update");
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                intent.putExtra("date", date);
                intent.putExtra("distance", distance);
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
            return;
        }
        mLocationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }
}
