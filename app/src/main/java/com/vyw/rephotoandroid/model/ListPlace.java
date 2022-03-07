package com.vyw.rephotoandroid.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ListPlace {
    @SerializedName("data")
    private List<Place> places;

    public ListPlace(List<Place> places) {
        this.places = places;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }
}
