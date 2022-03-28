package com.vyw.rephotoandroid;

import com.vyw.rephotoandroid.model.ListPlace;
import com.vyw.rephotoandroid.model.Place;
import com.vyw.rephotoandroid.model.api.LoginResponse;
import com.vyw.rephotoandroid.model.api.OneLoginResponse;
import com.vyw.rephotoandroid.model.api.Status;
import com.vyw.rephotoandroid.model.api.UploadFile;
import com.vyw.rephotoandroid.model.api.UserLogin;
import com.vyw.rephotoandroid.model.api.UserLogout;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiInterface {

    @GET("places")
    Call<ListPlace> getPlaces();

//    @POST("places")
//    Call<Place> getPlaces(
//            @Field("name") String name,
//            @Field("description") String description,
//            @Field("latitude") float latitude,
//            @Field("longitude") float longitude,
//            @Field("id_category") int id_category,
//            @Field("file") File file);

    @Multipart
    @POST("places/{id}/photo")
    Call<Status> addPhoto(
            @Header("access_token") String authHeader,
            @Path("id") int id,
            @Part MultipartBody.Part photo
    );

    @POST("user/login")
    Call<OneLoginResponse> login(@Body UserLogin userLogin );

    @POST("user/logout")
    Call<Status> logout(@Body UserLogout userLogout );
}
