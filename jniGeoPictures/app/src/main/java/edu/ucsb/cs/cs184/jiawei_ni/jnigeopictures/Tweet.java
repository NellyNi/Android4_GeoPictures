package edu.ucsb.cs.cs184.jiawei_ni.jnigeopictures;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

public class Tweet {
    private ArrayList<LatLng> path;
    private String postId;
    private Double title;
    private Double timestamp;
    private Double lon;
    private Double lat;
    private LatLng location;
    private LatLng lastLocation;
    private Marker marker;

    public Tweet(String postId, Double title, Double timestamp, Double lon, Double lat) {
        path = new ArrayList<>();
        this.postId = postId;
        this.title = title;
        this.timestamp = timestamp;
        this.lon = lon;
        this.lat = lat;
        this.location = new LatLng(lat, lon);
        this.lastLocation = location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setLastLocation(LatLng lastLocation) {
        this.lastLocation = lastLocation;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public ArrayList<LatLng> getPath() {
        return path;
    }

    public String getPostId() {
        return postId;
    }
    public Double getLon() {
        return lon;
    }

    public Double getLat() {
        return lat;
    }

    public Double getTitle() {
        return title;
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public LatLng getLocation() {
        return location;
    }

    public LatLng getLastLocation() {
        return lastLocation;
    }

    public Marker getMarker() {
        return marker;
    }
}