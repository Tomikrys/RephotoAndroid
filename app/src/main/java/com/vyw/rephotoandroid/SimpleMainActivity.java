package com.vyw.rephotoandroid;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;


public class SimpleMainActivity extends AppCompatActivity {

    private static String TAG = "SimpleMainActivity";

    static {
        System.loadLibrary("native-lib");
    }

    Dialog dialog;
    int simple_navigation_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_ref_choose);
        simple_navigation_button = findViewById(R.id.simple_navigation).getId();

//  TODO debug memory mode
//        if(BuildConfig.DEBUG)
//            StrictMode.enableDefaults();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void simpleNavigation(View view) {
//        ActivityCompat.finishAffinity(this);
        showFileDialog(view);
//        finish();
    }

    private static final int REQUEST_CHOOSER = 1234;
    int id_entered_button;
    String path_ref_image = "";

    public void showFileDialog(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "Choose file");

        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    public void RephotoApiButton(View view) {
        Intent intent = new Intent(this, Rephotos_API.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri uri = data.getData();
            String path = uri.toString();

            if (path != null) {
                path_ref_image = path;
                Intent intent = new Intent(this, SimpleNavigation.class);
                intent.putExtra("PATH_REF_IMAGE", path_ref_image);
                startActivity(intent);

            }
            Log.d(TAG, path);
            Log.d(TAG, String.valueOf(id_entered_button));
        }
    }

    public void exitApplication(MenuItem item) {
        finish();
    }
}
