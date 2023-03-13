package com.vyw.rephotoandroid.testCpp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;

import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.vyw.rephotoandroid.ImageFunctions;
import com.vyw.rephotoandroid.OpenCVNative;
import com.vyw.rephotoandroid.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class NavigationTest extends AppCompatActivity {
    private ImageView imageView;
    private boolean bProcessing = false;
    public int countFrames = 1;
    private Bitmap bitmap = null;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private static String TAG = "CameraPreview";
    private static final int REQUEST_CHOOSER = 1234;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_matcher);
        imageView = findViewById(R.id.image);


    }


    public void chooseImage(View view) {
        showFileDialog(view);
    }

    private void run(Bitmap bitmap) {
        bProcessing = true;
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(4);
        float width = 150f;
        float height = 150f;
        PreviewSizeWidth = 720;
        PreviewSizeHeight = 1280;
        float x = PreviewSizeWidth / 2 - width / 2;
        float y = PreviewSizeHeight / 10;

        canvas.drawRect(x, y, x + width, y + height, mPaint);
        mPaint.setColor(Color.BLACK);

        width = width - 10f;
        height = height - 10f;
        x = x + 5f;
        y = y + 5f;
        canvas.drawRect(x, y, x + width, y + height, mPaint);

        mPaint.setColor(Color.YELLOW);
        width = width - 10f;
        height = height - 10f;
        x = x + 5f;
        y = y + 5f;

        Mat mat_current_frame = new Mat();
        Utils.bitmapToMat(bitmap, mat_current_frame);

        int direction;

        try {
//            OPENCVNATIVECALL
//            direction = OpenCVNative.process_navigation(mat_current_frame.getNativeObjAddr(), countFrames);
//            Log.d(TAG, "Value of direction is: " + direction);
        } catch (java.lang.IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
            direction = 1;
        }


        //1 up, 2 down, 3 right, 4 left;
        Path wallpath = new Path();
        wallpath.reset();
//        if (direction < 10) {
//            switch (direction) {
//                case 0:
//                    wallpath.moveTo(x, y + height / 8);
//                    wallpath.lineTo(x + width / 8, y);
//                    wallpath.lineTo(x + width, y + height * 7 / 8);
//                    wallpath.lineTo(x + width * 7 / 8, y + height);
//
//                    wallpath.moveTo(x + width, y + height / 8);
//                    wallpath.lineTo(x + width * 7 / 8, y);
//                    wallpath.lineTo(x, y + height * 7 / 8);
//                    wallpath.lineTo(x + width / 8, y + height);
//                    break;
//                case 1:
//                    wallpath.moveTo(x + width / 4, y + height / 2);
//                    wallpath.lineTo(x + width / 4 + width / 2, y + height / 2);
//                    wallpath.lineTo(x + width / 4 + width / 2, y + height);
//                    wallpath.lineTo(x + width / 4 - 5f, y + height);
//                    wallpath.lineTo(x + width / 4 - 5f, y + height / 2);
//                    wallpath.moveTo(x, y + height / 2);
//                    wallpath.lineTo(x + width, y + height / 2);
//                    wallpath.lineTo(x + width / 2, y);
//                    break;
//                case 2:
//                    wallpath.moveTo(x + width / 4, y);
//                    wallpath.lineTo(x + width / 4 + width / 2, y);
//                    wallpath.lineTo(x + width / 4 + width / 2, y + height / 2);
//                    wallpath.lineTo(x + width / 4 - 5f, y + height / 2);
//                    wallpath.lineTo(x + width / 4 - 5f, y);
//                    wallpath.moveTo(x, y + height / 2);
//                    wallpath.lineTo(x + width, y + height / 2);
//                    wallpath.lineTo(x + width / 2, y + height);
//                    break;
//                case 3:
//                    wallpath.moveTo(x, y + height / 4);
//                    wallpath.lineTo(x, y + height / 4 + height / 2);
//                    wallpath.lineTo(x + width / 2, y + height / 4 + height / 2);
//                    wallpath.lineTo(x + width / 2, y + height / 4 - 5f);
//                    wallpath.lineTo(x, y + height / 4 - 5f);
//                    wallpath.moveTo(x + width / 2, y);
//                    wallpath.lineTo(x + width / 2, y + height);
//                    wallpath.lineTo(x + width, y + height / 2);
//                    break;
//                case 4:
//                    wallpath.moveTo(x + width / 2, y);
//                    wallpath.lineTo(x + width / 2, y + height);
//                    wallpath.lineTo(x, y + height / 2);
//                    wallpath.moveTo(x + width / 2, y + height / 4);
//                    wallpath.lineTo(x + width / 2, y + height / 4 + height / 2);
//                    wallpath.lineTo(x + width, y + height / 4 + height / 2);
//                    wallpath.lineTo(x + width, y + height / 4 - 5f);
//                    wallpath.lineTo(x + width / 2, y + height / 4 - 5f);
//                    break;
//            }
//        } else {
//            wallpath.moveTo(x + width / 2, y + height / 8);
//            wallpath.lineTo(x + width / 8, y + height / 2);
//            wallpath.lineTo(x + width / 2, y + height * 7 / 8);
//            wallpath.lineTo(x + width * 7 / 8, y + height / 2);
//            wallpath.lineTo(x + width / 2, y + height / 8);
//            switch (direction) {
//                case 13:
//                    wallpath.moveTo(x + width * 1 / 3, y);
//                    wallpath.lineTo(x + width, y + height * 2 / 3);
//                    wallpath.lineTo(x + width, y);
//                    canvas.drawPath(wallpath, mPaint);
//                    break;
//                case 14:
//                    wallpath.moveTo(x + width * 2 / 3, y);
//                    wallpath.lineTo(x, y + height * 2 / 3);
//                    wallpath.lineTo(x, y);
//                    canvas.drawPath(wallpath, mPaint);
//                    break;
//                case 23:
//                    wallpath.moveTo(x + width * 2 / 3, y + height);
//                    wallpath.lineTo(x, y + height * 1 / 3);
//                    wallpath.lineTo(x, y + height);
//                    canvas.drawPath(wallpath, mPaint);
//                    break;
//                case 24:
//                    wallpath.moveTo(x + width * 1 / 3, y + height);
//                    wallpath.lineTo(x + width, y + height * 1 / 3);
//                    wallpath.lineTo(x + width, y + height);
//                    canvas.drawPath(wallpath, mPaint);
//                    break;
//
//            }
//        }
//        canvas.drawPath(wallpath, mPaint);

        imageView.setImageBitmap(mutableBitmap);
        bProcessing = false;
        countFrames++;
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

            Uri uri_current_image = Uri.parse(path);
            Bitmap bt_current_frame = null;
            try {
                bt_current_frame = ImageFunctions.getBitmapFromUri(uri_current_image, this);
                imageView.setImageBitmap(bt_current_frame);
                run(bt_current_frame);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}