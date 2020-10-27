package com.example.traveldriving.model;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.Toast;

import com.example.traveldriving.activity.MainActivity;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;

public class DrivingLog extends RealmObject implements Serializable {
    private long id;

    private double startLatitude;
    private double startLongitude;
    private double stopLatitude;
    private double stopLongitude;
    private Date startDate;
    private Date stopDate;
    private RealmList<MapPoint> mapPoints;

    public DrivingLog() {
    }

    public double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public double getStopLatitude() {
        return stopLatitude;
    }

    public void setStopLatitude(double stopLatitude) {
        this.stopLatitude = stopLatitude;
    }

    public double getStopLongitude() {
        return stopLongitude;
    }

    public void setStopLongitude(double stopLongitude) {
        this.stopLongitude = stopLongitude;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReadableLocation(Context context, boolean isStart){
        String result = "찾을수없음";
        Geocoder geocoder = new Geocoder(context);
        List<Address> list = new ArrayList<Address>();
        double latitude, longitude;

        if(isStart){
            latitude = this.getStartLatitude();
            longitude = this.getStartLongitude();
        } else {
            latitude = this.getStopLatitude();
            longitude = this.getStopLongitude();
        }

        try {
            list = geocoder.getFromLocation(latitude, longitude, 2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (list.size() > 0) {
            result = list.get(0).getAddressLine(0).toString();
        }

        return result;
    }

    public long getId() {
        return id;
    }

    public RealmList<MapPoint> getMapPoints() {
        return mapPoints;
    }

    public void setMapPoints(RealmList<MapPoint> mapPoints) {
        this.mapPoints = mapPoints;
    }
}
