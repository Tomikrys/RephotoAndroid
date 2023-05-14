package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import java.lang.Float;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.vyw.rephotoandroid.model.GalleryItem;
import com.vyw.rephotoandroid.model.ListPlace;
import com.vyw.rephotoandroid.model.Photo;
import com.vyw.rephotoandroid.model.Place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by amardeep on 10/25/2017.
 */

public class GalleryUtils {
    public static String TAG = "GalleryUtils";

    //Define bucket name from which you want to take images Example '/DCIM/Camera' for camera images
    public static final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStorageDirectory().toString() + "/Download";

    //method to get id of image bucket from path
    public static String getBucketId(String path) {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    //method to get images
    public static List<GalleryItem> getLocalImages(Context context) {
        final String[] projection = {MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA};
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = {GalleryUtils.getBucketId(CAMERA_IMAGE_BUCKET_NAME)};
        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        ArrayList<GalleryItem> result = new ArrayList<GalleryItem>(cursor.getCount());
        if (cursor.moveToFirst()) {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            final int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            do {
                GalleryItem galleryItem = new GalleryItem(cursor.getString(dataColumn), cursor.getString(nameColumn));
                result.add(galleryItem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public static List<GalleryItem> getImagesFromAPI(Location location) {
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<ListPlace> call = apiInterface.getPlaces();

        try {
            Response<ListPlace> response = call.execute();
            ListPlace listPlace = response.body();
            if (listPlace != null) {
                List<Place> places = listPlace.getPlaces();
                if (places != null) {
                    ArrayList<GalleryItem> results = new ArrayList<GalleryItem>(places.size());
                    for (int i = 0; i < places.size(); i++) {
                        Place place = places.get(i);
                        Log.d(TAG, "onResponse: [" + i + "] name : " + place.getName());
                        Location placeLocation = new Location("providername");
                        placeLocation.setLatitude(Double.parseDouble(place.getLatitude()));
                        placeLocation.setLongitude(Double.parseDouble(place.getLongitude()));
                        try {
                            GalleryItem galleryItem = new GalleryItem(
                                    place.getPhotos().get(0).getPhotoUri(),
                                    place.getName(),
                                    place.getPhotos().get(0).getCaptured_at(),
                                    place,
                                    placeLocation.distanceTo(location)
                            );
                            for (Photo photo : place.getPhotos()) {
                                GalleryItem photoItem = new GalleryItem(
                                        photo.getPhotoUri(),
                                        "photoItem",
                                        photo.getCaptured_at(),
                                        place,
                                        placeLocation.distanceTo(location)
                                );
                                galleryItem.addPlacePhoto(photoItem);
                            }
                            results.add(galleryItem);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                    if (location != null) {
                        ((List) results).sort(new Comparator<GalleryItem>() {
                            @Override
                            public int compare(GalleryItem o1, GalleryItem o2) {
                                return o1.distance.compareTo(o2.distance);
                            }
                        });
                    }
                    return results;
                } else {
                    Log.e(TAG, "onResponse: empty places");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
