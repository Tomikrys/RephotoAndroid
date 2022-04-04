package com.vyw.rephotoandroid.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ListGalleryItems implements Parcelable {
    List<GalleryItem> galleryItemArrayList;
    public ListGalleryItems(List<GalleryItem> galleryItemArrayList) {
        this.galleryItemArrayList = galleryItemArrayList;
    }

    public List<GalleryItem> getListGalleryItem() {
        return galleryItemArrayList;
    }

    public void setListGalleryItem(List<Place> places) {
        this.galleryItemArrayList = galleryItemArrayList;
    }

    protected ListGalleryItems(Parcel in) {
    }

    public static final Creator<ListGalleryItems> CREATOR = new Creator<ListGalleryItems>() {
        @Override
        public ListGalleryItems createFromParcel(Parcel in) {
            return new ListGalleryItems(in);
        }

        @Override
        public ListGalleryItems[] newArray(int size) {
            return new ListGalleryItems[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
