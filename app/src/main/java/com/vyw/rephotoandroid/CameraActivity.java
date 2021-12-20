package com.vyw.rephotoandroid;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by acervenka2 on 20.03.2017.
 */

public class CameraActivity extends Activity {

    private static String TAG = "CameraActivity";

    private ImageView capturedImage;

    private ArrayList<Bitmap> images;

    private Mat firstFrame;
    private Mat secondFrame;
    private Mat refFrame;

    Bitmap bt_ref_frame;

    String path_ref_image;
    String path_first_image;
    String path_second_image;

    private float[] calibrate_params;

    Intent intent1;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    refFrame = new Mat();
                    firstFrame = new Mat();
                    secondFrame = new Mat();
                    calc();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVCallBack)) {
            Log.e("TEST", "Cannot connect to OpenCV Manager");
        }


        setContentView(R.layout.activity_main);

        Typeface font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        Button btnCamera = (Button) findViewById(R.id.btnCamera);

        capturedImage = (ImageView) findViewById(R.id.capturedImage);

        btnCamera.setTypeface(font);

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        Intent intent = getIntent();
        String message = intent.getStringExtra(EXTRA_MESSAGE);


        String delims = "[;]";
        String[] value = message.split(delims);

        calibrate_params = new float[8];
        for (int i = 0; i < calibrate_params.length; i++) {
            calibrate_params[i] = Float.parseFloat(value[i]);
        }

        images = new ArrayList<>();

        intent1 = new Intent(this, SelectPointsActivity.class);

        path_ref_image = intent.getStringExtra("PATH_REF_IMAGE");
        path_first_image = intent.getStringExtra("PATH_FIRST_IMAGE");
        path_second_image = intent.getStringExtra("PATH_SECOND_IMAGE");


        File file0 = new File(path_ref_image);
        bt_ref_frame = BitmapFactory.decodeFile(file0.getAbsolutePath());

    }

    private void calc() {
        Utils.bitmapToMat(bt_ref_frame, refFrame);
        if (!"".equals(path_first_image) && !"".equals(path_second_image)) {

            File file1 = new File(path_first_image);
            File file2 = new File(path_second_image);


            Bitmap myBitmap1 = BitmapFactory.decodeFile(file1.getAbsolutePath());
            Bitmap myBitmap2 = BitmapFactory.decodeFile(file2.getAbsolutePath());

            Utils.bitmapToMat(myBitmap1, firstFrame);
            Utils.bitmapToMat(myBitmap2, secondFrame);
            Utils.bitmapToMat(bt_ref_frame, refFrame);

            OpenCVNative.initReconstruction(firstFrame.getNativeObjAddr(), secondFrame.getNativeObjAddr(), refFrame.getNativeObjAddr(), calibrate_params);
            float[] points = OpenCVNative.processReconstruction();
            //Log.i(TAG, "Desc: " + out.dump());

            intent1.putExtra("first_image", firstFrame.getNativeObjAddr());
            intent1.putExtra("ref_image", refFrame.getNativeObjAddr());
            intent1.putExtra("x", points[0]);
            intent1.putExtra("y", points[1]);
            startActivity(intent1);
            finish();
        }
    }


    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 0);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap bp = (Bitmap) data.getExtras().get("data");
            capturedImage.setImageBitmap(bp);
            images.add(bp);
            if (images.size() == 2) {

                Utils.bitmapToMat(images.get(0), firstFrame);
                Utils.bitmapToMat(images.get(1), secondFrame);

                OpenCVNative.initReconstruction(firstFrame.getNativeObjAddr(), secondFrame.getNativeObjAddr(), refFrame.getNativeObjAddr(), calibrate_params);

                ActivityCompat.finishAffinity(this);
                Intent intent = new Intent(this, SelectPointsActivity.class);
                intent.putExtra("first_image", firstFrame.getNativeObjAddr());
                intent.putExtra("ref_image", refFrame.getNativeObjAddr());
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);

    }

    public void exitApplication(MenuItem item) {
        System.exit(0);
    }

}
