package com.vyw.rephotoandroid.model;

import java.util.List;

public class Place {
    private int id;
    private String name;
    private String description;
    private String latitude;
    private String longtitude;
    private List<Photo> photos;

    public Place(int id,
                 String name,
                 String description,
                 String latitude,
                 String longtitude, List<Photo> photos) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.photos = photos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(String longtitude) {
        this.longtitude = longtitude;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }
}
