package com.vyw.rephotoandroid.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.vyw.rephotoandroid.R;

public class Configuration {
    // TODO dont forget to change it in res/xml/network_security_config.xml
//    public static String baseUrl = "http://192.168.0.15/";
//    public static final String baseUrl = "http://10.0.2.2/";
    public static final String baseUrl = "https://rephoto.cz/";

    public static String getSharedValue(AppCompatActivity context, String key) {
        SharedPreferences sharedPreferences = context.getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    public static String getAccessToken(AppCompatActivity context) {
        return getSharedValue(context, context.getString(R.string.access_token));
    }

    public static String getFirstName(AppCompatActivity context) {
        return getSharedValue(context, context.getString(R.string.first_name));
    }

    public static String getLastName(AppCompatActivity context) {
        return getSharedValue(context, context.getString(R.string.last_name));
    }

    public static String getEmail(AppCompatActivity context) {
        return getSharedValue(context, context.getString(R.string.email));
    }
}
