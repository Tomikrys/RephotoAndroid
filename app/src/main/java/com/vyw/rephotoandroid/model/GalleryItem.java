package com.vyw.rephotoandroid.model;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.icu.text.SimpleDateFormat;
import android.util.Log;

import java.util.Date;


/**
 * Created by amardeep on 11/3/2017.
 */
//This class represents single gallery item
public class GalleryItem {
    public String imageUri;
    public String imageName;
    public String year = null;
    public boolean isSelected = false;
    public String TAG = "GalleryItem";

    public GalleryItem(String imageUri, String imageName) {
        this.imageUri = imageUri;
        this.imageName = imageName;
    }

    public GalleryItem(String imageUri, String imageName, String strDate) {
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
    }
}
