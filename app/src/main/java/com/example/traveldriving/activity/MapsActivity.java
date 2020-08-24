package com.example.traveldriving.activity;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.traveldriving.R;
import com.example.traveldriving.model.DrivingLog;
import com.example.traveldriving.model.MapPoint;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;

    private RealmList<MapPoint> mMapPoints;
    private DrivingLog mDrivingLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        long drivingLogId = intent.getLongExtra("drivingLogId", 0);

        Realm.init(this);
        Realm realm = Realm.getDefaultInstance();
        mDrivingLog = realm.where(DrivingLog.class).equalTo("id", drivingLogId).findFirst();
        Log.d(TAG, String.valueOf(mDrivingLog.getMapPoints().size())) ;
        mMapPoints = mDrivingLog.getMapPoints();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng startLatLng = new LatLng(mDrivingLog.getStartLatitude(), mDrivingLog.getStartLongitude());
        mMap.addMarker(new MarkerOptions().position(startLatLng).title("시작"));

        for (int i = 0; i < mMapPoints.size(); i++) {
            MapPoint mapPoint = mMapPoints.get(i);
            LatLng latLng = new LatLng(mapPoint.getLatitude(), mapPoint.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title(i + "번째"));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13.5f), 1000, null);


        // Add a marker in Sydney and move the camera
        LatLng stopLatLng = new LatLng(mDrivingLog.getStopLatitude(), mDrivingLog.getStopLongitude());
        mMap.addMarker(new MarkerOptions().position(stopLatLng).title("종료"));
    }
}