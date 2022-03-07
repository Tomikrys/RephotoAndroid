package com.vyw.rephotoandroid;

import com.vyw.rephotoandroid.model.ListPlace;
import com.vyw.rephotoandroid.model.Place;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiInterface {

    @GET("places")
    Call<ListPlace> getPlaces();

    @POST("places")
    Call<Place> getPlaces(
            @Field("name") String name,
            @Field("description") String description,
            @Field("latitude") float latitude,
            @Field("longitude") float longitude,
            @Field("id_category") int id_category,
            @Field("file") File file);
}
