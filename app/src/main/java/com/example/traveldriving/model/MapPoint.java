package com.example.traveldriving.model;

import java.util.Date;

import io.realm.RealmObject;

public class MapPoint extends RealmObject {
    private DrivingLog drivingLog;
    private double latitude;
    private double longitude;
    private Date currentDate;

    public MapPoint() {
    }

    public DrivingLog getDrivingLog() {
        return drivingLog;
    }

    public void setDrivingLog(DrivingLog drivingLog) {
        this.drivingLog = drivingLog;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(Date currentDate) {
        this.currentDate = currentDate;
    }

    @Override
    public String toString() {
        return "MapPoint{" +
                "drivingLog=" + drivingLog +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", currentDate=" + currentDate +
                '}';
    }
}
