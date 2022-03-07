package com.vyw.rephotoandroid;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vyw.rephotoandroid.model.ListPlace;
import com.vyw.rephotoandroid.model.Place;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Rephotos_API extends AppCompatActivity {
    public static String TAG = "Rephotos_API";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rephotos_api);
    }

    public void getPhotos(View view) {
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<ListPlace> call = apiInterface.getPlaces();

        call.enqueue(new Callback<ListPlace>() {
            @Override
            public void onResponse(Call<ListPlace> call, Response<ListPlace> response) {
                Log.e(TAG, "onResponse: " + response.code());
                ListPlace listPlace = response.body();
                if (listPlace != null) {
                    List<Place> places = listPlace.getPlaces();
                    if (places != null) {
                        for (int i = 0; i < places.size(); i++) {
                            Place place = places.get(i);
                            Log.e(TAG, "onResponse: [" + i + "] name : " + place.getName());
                        }
                    } else {
                        Log.e(TAG, "onResponse: empty places");
                    }
                } else {
                    Log.e(TAG, "onResponse: empty response");
                }
            }

            @Override
            public void onFailure(Call<ListPlace> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }


}
