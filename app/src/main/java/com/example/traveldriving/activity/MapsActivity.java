package com.example.traveldriving.activity;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.traveldriving.R;
import com.example.traveldriving.model.MapPoint;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double mStartLatitude;
    private double mStartLongitude;
    private double mStopLatitude;
    private double mStopLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        mStartLatitude = intent.getDoubleExtra("startLatitude", 0);
        mStartLongitude = intent.getDoubleExtra("startLongitude", 0);
        mStopLatitude = intent.getDoubleExtra("stopLatitude", 0);
        mStopLongitude = intent.getDoubleExtra("stopLongitude", 0);
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
        LatLng startLocation = new LatLng(mStartLatitude, mStartLongitude);
        LatLng stopLocation = new LatLng(mStopLatitude, mStopLongitude);
        mMap.addMarker(new MarkerOptions().position(startLocation).title("시작위치"));
        mMap.addMarker(new MarkerOptions().position(stopLocation).title("종료위치"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startLocation));
    }
}