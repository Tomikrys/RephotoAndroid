package com.vyw.rephotoandroid;

import com.vyw.rephotoandroid.model.ListPlace;
import com.vyw.rephotoandroid.model.Photo;
import com.vyw.rephotoandroid.model.Place;
import com.vyw.rephotoandroid.model.Status;
import com.vyw.rephotoandroid.model.UploadFile;

import java.io.File;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

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

    @Multipart
    @POST("uploadAttachment")
    Call<Status> uploadAttachment(@Part MultipartBody.Part filePart);
    // You can add other parameters too

    @POST("place/1/photo")
    Call<Status> addPhoto( @Body UploadFile uploadFile );
}
