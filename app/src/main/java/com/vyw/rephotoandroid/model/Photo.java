package com.vyw.rephotoandroid.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.net.URL;

public class Photo {
    private int id;
    private String captured_at;
    private int id_file;
    private int id_place;
    private int id_user;
    private User user;
    private String TAG = "Photo";

    public Photo(int id,
                 String captured_at,
                 int id_file,
                 int id_place,
                 int id_user,
                 User user) {
        this.id = id;
        this.captured_at = captured_at;
        this.id_file = id_file;
        this.id_place = id_place;
        this.id_user = id_user;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCaptured_at() {
        return captured_at;
    }

    public void setCaptured_at(String captured_at) {
        this.captured_at = captured_at;
    }

    public int getId_file() {
        return id_file;
    }

    Bitmap tmp_photo = null;
//    https://square.github.io/picasso/#features
//    https://guides.codepath.com/android/Displaying-Images-with-the-Picasso-Library
    public Bitmap getPhoto(ImageView imageView) {
        tmp_photo = null;
        Picasso.get().setLoggingEnabled(true);
        Picasso.get().load(Configuration.baseUrl +  "uploads/" + id_file + ".png").into(imageView);
        return null;
//        Picasso.get().load(Configuration.baseUrl + "uploads/" + id_file + ".png").into(new Target() {
////        Picasso.get().load("https://riptutorial.com/assets/images/android-top10-logo.png").into(new Target() {
//            @Override
//            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                tmp_photo = bitmap;
//            }
//
//            @Override
//            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
//            }
//
//            @Override
//            public void onPrepareLoad(Drawable placeHolderDrawable) {
//            }
//        });
////        TODO null
//        return tmp_photo;
    }

    public void setId_file(int id_file) {
        this.id_file = id_file;
    }

    public int getId_place() {
        return id_place;
    }

    public void setId_place(int id_place) {
        this.id_place = id_place;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPhotoUri() {
        return Configuration.baseUrl + "uploads/" + id_file + ".jpg";
    }
}
