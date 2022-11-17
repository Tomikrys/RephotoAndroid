package com.vyw.rephotoandroid.testCpp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vyw.rephotoandroid.GalleryMainActivity;
import com.vyw.rephotoandroid.ImageFunctions;
import com.vyw.rephotoandroid.OpenCVNative;
import com.vyw.rephotoandroid.R;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Objects;

public class TestCpp extends AppCompatActivity {

    private static final int RC_READ_STORAGE = 7;
    private static final int REQUEST_CHOOSER = 1234;
    private static String TAG = "testCpp";
    private String path_first_image = "";
    private String path_second_image = "";
    private String path_ref_image = "";
    private Boolean isOCVSetUp = false;


    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: { Log.i(TAG, "OpenCV loaded successfully"); }
                break;
                default: { super.onManagerConnected(status); }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_cpp);

        // to make the Navigation drawer icon always appear on the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //check for read storage permission
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            //Get images
//        } else {
//            //request permission
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RC_READ_STORAGE);
//        }

        if (!isOCVSetUp) { // if OCV hasn't been setup yet, init it
            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
                Log.i(TAG, "Cannot load OpenCV");
            } else {
                isOCVSetUp = true;
                mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }

    }

    public void initializeCpp(View view) {
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 8);
//        }
        showFileDialog(view);
        showFileDialog(view);
        showFileDialog(view);
    }

    public void cameraTest(View view) {
        showFileDialog(view);
        Intent intent = new Intent(this, CameraTest.class);
        startActivity(intent);
    }

    public void startCpp() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 8);
        }

        Uri uri_first_image = Uri.parse(path_first_image);
        Uri uri_second_image = Uri.parse(path_second_image);
        Uri uri_ref_image = Uri.parse(path_ref_image);
        Bitmap bt_first_frame = null;
        Bitmap bt_second_frame = null;
        Bitmap bt_ref_frame = null;
        try {
            bt_first_frame = ImageFunctions.getBitmapFromUri(uri_first_image, this);
            bt_second_frame = ImageFunctions.getBitmapFromUri(uri_second_image, this);
            bt_ref_frame = ImageFunctions.getBitmapFromUri(uri_ref_image, this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Mat mat_first_frame = new Mat();
        Mat mat_second_frame = new Mat();
        Mat mat_ref_frame = new Mat();

        Utils.bitmapToMat(bt_first_frame, mat_first_frame);
        Utils.bitmapToMat(bt_second_frame, mat_second_frame);
        Utils.bitmapToMat(bt_ref_frame, mat_ref_frame);

        OpenCVNative.fakemain(mat_first_frame.getNativeObjAddr(), mat_second_frame.getNativeObjAddr(), mat_ref_frame.getNativeObjAddr());
    }

    public void showFileDialog(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "Choose file");

        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri uri = data.getData();
            String path = uri.toString();

            if (path != null) {
                if (Objects.equals(path_first_image, "")) {
                    path_first_image = path;
                } else if (Objects.equals(path_second_image, "")) {
                    path_second_image = path;
                } else if (Objects.equals(path_ref_image, "")) {
                    path_ref_image = path;
                    this.startCpp();
                }
            }
            Log.d(TAG, path);
        }
    }
}
