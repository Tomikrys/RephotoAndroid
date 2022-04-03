package com.vyw.rephotoandroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationView;
import com.vyw.rephotoandroid.model.Configuration;
import com.vyw.rephotoandroid.model.ListPlace;
import com.vyw.rephotoandroid.model.Place;
import com.vyw.rephotoandroid.model.api.LoginResponse;
import com.vyw.rephotoandroid.model.api.OneLoginResponse;
import com.vyw.rephotoandroid.model.api.Status;
import com.vyw.rephotoandroid.model.api.UserLogin;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class Login extends AppCompatActivity {

    private static String TAG = "Login";

    static {
        System.loadLibrary("native-lib");
    }

    Dialog dialog;
    int login_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

//  TODO debug memory mode
//        if(BuildConfig.DEBUG)
//            StrictMode.enableDefaults();
    }


    public void Login(View view) {
        Login();
    }

    public void Login() {
        EditText emailInput = (EditText) findViewById(R.id.email);
        EditText passwordInput = (EditText) findViewById(R.id.password);
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        Toast.makeText(
                this,
                "logging in " + email + "/" + password,
                Toast.LENGTH_SHORT
        ).show();

        UserLogin userLogin = new UserLogin(email, password);
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<OneLoginResponse> call = apiInterface.login(userLogin);
        Login copyThis = this;
        call.enqueue(new Callback<OneLoginResponse>() {
            @Override
            public void onResponse(Call<OneLoginResponse> call, Response<OneLoginResponse> response) {
                LoginResponse status = response.body().getLoginResponse();
                if (response.code() == 200) {
//                    Configuration.access_token = status.getAccess_token();
//                    Configuration.email = status.getEmail();
//                    Configuration.first_name = status.getFirst_name();
//                    Configuration.last_name = status.getLast_name();
//                    Configuration.logged = true;
//                    Toast.makeText(
//                            copyThis,
//                            "Logged in as " + Configuration.first_name + " " + Configuration.last_name,
//                            Toast.LENGTH_SHORT
//                    ).show();

                    NavigationView navigationView = findViewById(R.id.drawer_navigation);
                    Menu navigation_menu = navigationView.getMenu();
                    navigation_menu.findItem(R.id.logout).setVisible(true);
                    navigation_menu.findItem(R.id.login).setVisible(false);
                    TextView header_title = findViewById(R.id.header_title);
//                    header_title.setText("Logged in as " + Configuration.first_name + " " + Configuration.last_name);

//                    TODO update menu
                    copyThis.finish();
                } else {
                    Log.e(TAG, "onResponse: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<OneLoginResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    public void exitApplication(MenuItem item) {
        finish();
    }

}
