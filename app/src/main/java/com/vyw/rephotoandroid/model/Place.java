package com.vyw.rephotoandroid.model;

public class Place {
    private int id;
    private String name;
    private String description;
    private String latitude;
    private String longtitude;
    private Photo oldestPhoto;
    private Photo newestPhoto;

    public Place(int id,
                 String name,
                 String description,
                 String latitude,
                 String longtitude,
                 Photo oldestPhoto,
                 Photo newestPhoto) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.oldestPhoto = oldestPhoto;
        this.newestPhoto = newestPhoto;
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

    public Photo getOldestPhoto() {
        return oldestPhoto;
    }

    public void setOldestPhoto(Photo oldestPhoto) {
        this.oldestPhoto = oldestPhoto;
    }

    public Photo getNewestPhoto() {
        return newestPhoto;
    }

    public void setNewestPhoto(Photo newestPhoto) {
        this.newestPhoto = newestPhoto;
    }
}
