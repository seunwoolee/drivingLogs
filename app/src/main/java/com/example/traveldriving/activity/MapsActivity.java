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

    private double mStartLatitude;
    private double mStartLongitude;
    private double mStopLatitude;
    private double mStopLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        long drivingLogId = intent.getLongExtra("drivingLogId", 0);

        Realm.init(this);
        Realm realm = Realm.getDefaultInstance();
        DrivingLog drivingLog = realm.where(DrivingLog.class).equalTo("id", drivingLogId).findFirst();
        Log.d(TAG, String.valueOf(drivingLog.getMapPoints().size())) ;
        mMapPoints = drivingLog.getMapPoints();


//        mStartLatitude = intent.getDoubleExtra("startLatitude", 0);
//        mStartLongitude = intent.getDoubleExtra("startLongitude", 0);
//        mStopLatitude = intent.getDoubleExtra("stopLatitude", 0);
//        mStopLongitude = intent.getDoubleExtra("stopLongitude", 0);

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
        LatLng startLatLng = new LatLng(mMapPoints.get(0).getLatitude(), mMapPoints.get(0).getLongitude());

        for (int i = 0; i < mMapPoints.size(); i++) {
            MapPoint mapPoint = mMapPoints.get(i);
            LatLng latLng = new LatLng(mapPoint.getLatitude(), mapPoint.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title(i + "번째"));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));

//        LatLng startLocation = new LatLng(mStartLatitude, mStartLongitude);
//        LatLng stopLocation = new LatLng(mStopLatitude, mStopLongitude);
//        mMap.addMarker(new MarkerOptions().position(startLocation).title("시작위치"));
//        mMap.addMarker(new MarkerOptions().position(stopLocation).title("종료위치"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLocation));
    }
}