package com.vyw.rephotoandroid;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * Created by acervenka2 on 29.04.2017.
 */

public class SettingActivity extends AppCompatActivity {


    private static final int REQUEST_CHOOSER = 1234;

    private static final String TAG = "SettingActivity";

    String path_ref_image = "";
    String path_first_image = "";
    String path_second_image = "";

    int id_entered_button = 0;

    int id_ref_button;
    int id_first_button = 0;
    int id_second_button = 0;

    String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seting_layout);

        id_ref_button = findViewById(R.id.button).getId();
        id_first_button = findViewById(R.id.button2).getId();
        id_second_button = findViewById(R.id.button3).getId();

        Intent intent = getIntent();
        message = intent.getStringExtra(EXTRA_MESSAGE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHOOSER:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    String path = uri.toString();

                    if (path != null) {
                        if (Integer.compare(id_entered_button, id_ref_button) == 0) {
                            path_ref_image = path;
                            TextView textView = (TextView) findViewById(R.id.ref_frame_label);
                            textView.setText(path_ref_image);
                        } else if (Integer.compare(id_entered_button, id_first_button) == 0) {
                            path_first_image = path;
                            TextView textView = (TextView) findViewById(R.id.fist_frame_label);
                            textView.setText(path_first_image);
                        } else if (Integer.compare(id_entered_button, id_second_button) == 0) {
                            path_second_image = path;
                            TextView textView = (TextView) findViewById(R.id.second_frame_label);
                            textView.setText(path_second_image);

                        }
                        Log.d(TAG, path);
                        Log.d(TAG, String.valueOf(id_entered_button));
                    }
                }
                break;
        }
    }

    public void showFileDialog(View view) {
        id_entered_button = view.getId();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "Choose file");

        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    public void setEnableButton(View view) {
        CheckBox checkBox = (CheckBox) view;
        Button button2 = (Button) findViewById(R.id.button2);
        Button button3 = (Button) findViewById(R.id.button3);
        button2.setEnabled(checkBox.isChecked());
        button3.setEnabled(checkBox.isChecked());
    }

    public void nextPage(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        Log.d(TAG, message);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra("PATH_REF_IMAGE", path_ref_image);
        intent.putExtra("PATH_FIRST_IMAGE", path_first_image);
        intent.putExtra("PATH_SECOND_IMAGE", path_second_image);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
        finish();
    }

    public void exitApplication(MenuItem item) {
        finish();
    }
}
