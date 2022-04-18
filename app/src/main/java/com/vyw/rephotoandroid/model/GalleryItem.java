package com.vyw.rephotoandroid.model;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.icu.text.SimpleDateFormat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by amardeep on 11/3/2017.
 */
//This class represents single gallery item
public class GalleryItem implements Comparable<GalleryItem> {
    public String imageUri;
    public List<GalleryItem> placePhotos;
    public String imageName;
    public String year = null;
//    public String latitude = null;
//    public String longtitude = null;
//    public String distance = null;
    public boolean isSelected = false;
    public String TAG = "GalleryItem";
    public Place place;

    public GalleryItem(String imageUri, String imageName) {
        this.imageUri = imageUri;
        this.imageName = imageName;
    }

    public GalleryItem(String imageUri, String imageName, String strDate, Place place) {
        this.imageUri = imageUri;
        this.imageName = imageName;
        String year = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = format.parse(strDate);
            SimpleDateFormat df = new SimpleDateFormat("yyyy");
            year = df.format(date);
        }
        catch(Exception e) {
            Log.e(TAG, "GalleryItem: " + e.toString() );
        }
        this.year = year;
        this.placePhotos = new ArrayList<>();
        this.place = place;
    }

    public void addPlacePhoto(GalleryItem photo) {
        this.placePhotos.add(photo);
    }

    public List<GalleryItem> getPlacePhotos() {
        return this.placePhotos;
    }

    public GalleryItem getPlacePhoto(int id) {
        return this.placePhotos.get(id);
    }

    @Override
    public int compareTo(GalleryItem o) {
//        return distance(this.latitude, this.longtitude, o.latitude, o.longtitude);
        return 0;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}
