package com.vyw.rephotoandroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class CameraPreview extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    //    private TextView textView;
    private boolean bProcessing = false;
    public int countFrames = 1;
    private ImageView MyCameraPreview = null;
    private Bitmap bitmap = null;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private static String TAG = "CameraPreview";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        previewView = findViewById(R.id.previewView);
        MyCameraPreview = findViewById(R.id.arrow);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//        textView = findViewById(R.id.orientation2);
        if (!hasCameraPermission()) {
            requestPermission();
        }
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }


    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setTargetResolution(new Size(1280, 960))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                byte[] bytes = new byte[imageProxy.getPlanes()[0].getBuffer().remaining()];
                imageProxy.getPlanes()[0].getBuffer().get(bytes);

                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                Bitmap bitmap = RGBA8888BitesToBitmap(bytes, imageProxy.getWidth(), imageProxy.getHeight(), rotationDegrees);

                if (bitmap != null) {
                    Log.i(TAG, "Run analysis");
                    run(bitmap);
                } else {
                    Log.d(TAG, "imageBitmap is null, cannot run analysis");
                }
//
                imageProxy.close();
            }
        });
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
    }

    private Bitmap RGBA8888BitesToBitmap(byte[] bytes, int width, int height, int rotationDegrees) {
        IntBuffer intBuf =
                ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .asIntBuffer();
        int[] intarray = new int[intBuf.remaining()];
        intBuf.get(intarray);

        for (int i = 0; i < intarray.length; i++) {
            intarray[i] = Integer.rotateRight(intarray[i], 8);
        }

        Bitmap bitmap = Bitmap.createBitmap(intarray, width, height, Bitmap.Config.ARGB_8888);

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }

    private boolean saveBitmapToFile(Bitmap bitmap, String fileName) {
        String filePath = "/storage/emulated/0/Download/" + fileName + ".jpg";
        try {
            File file = new File(filePath);
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
            fOut.flush();
            fOut.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.i(null, "Save file error!");
            return false;
        }
        return true;
    }

    private void run(Bitmap bitmap) {

        bProcessing = true;
        Canvas canvas = new Canvas(bitmap);
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

        Mat currentFrames = new Mat();
        Utils.bitmapToMat(bitmap, currentFrames);

        int direction;

        try {
//            OPENCVNATIVECALL
            countFrames = 1; // for no multithreading
            direction = OpenCVNative.processNavigation(currentFrames.getNativeObjAddr(), countFrames);
            Log.d(TAG, "Value of direction is: " + direction);
        } catch (java.lang.IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
            direction = 1;
        }



        //1 up, 2 down, 3 right, 4 left;
        Path wallpath = new Path();
        wallpath.reset();
        if (direction < 10) {
            switch (direction) {
                case 0:
                    wallpath.moveTo(x, y + height / 8);
                    wallpath.lineTo(x + width / 8, y);
                    wallpath.lineTo(x + width, y + height * 7 / 8);
                    wallpath.lineTo(x + width * 7 / 8, y + height);

                    wallpath.moveTo(x + width, y + height / 8);
                    wallpath.lineTo(x + width*7 / 8, y);
                    wallpath.lineTo(x, y + height * 7 / 8);
                    wallpath.lineTo(x + width / 8, y + height);
                    break;
                case 1:
                    wallpath.moveTo(x + width / 4, y + height / 2);
                    wallpath.lineTo(x + width / 4 + width / 2, y + height / 2);
                    wallpath.lineTo(x + width / 4 + width / 2, y + height);
                    wallpath.lineTo(x + width / 4 - 5f, y + height);
                    wallpath.lineTo(x + width / 4 - 5f, y + height / 2);
                    wallpath.moveTo(x, y + height / 2);
                    wallpath.lineTo(x + width, y + height / 2);
                    wallpath.lineTo(x + width / 2, y);
                    break;
                case 2:
                    wallpath.moveTo(x + width / 4, y);
                    wallpath.lineTo(x + width / 4 + width / 2, y);
                    wallpath.lineTo(x + width / 4 + width / 2, y + height / 2);
                    wallpath.lineTo(x + width / 4 - 5f, y + height / 2);
                    wallpath.lineTo(x + width / 4 - 5f, y);
                    wallpath.moveTo(x, y + height / 2);
                    wallpath.lineTo(x + width, y + height / 2);
                    wallpath.lineTo(x + width / 2, y + height);
                    break;
                case 3:
                    wallpath.moveTo(x, y + height / 4);
                    wallpath.lineTo(x, y + height / 4 + height / 2);
                    wallpath.lineTo(x + width / 2, y + height / 4 + height / 2);
                    wallpath.lineTo(x + width / 2, y + height / 4 - 5f);
                    wallpath.lineTo(x, y + height / 4 - 5f);
                    wallpath.moveTo(x + width / 2, y);
                    wallpath.lineTo(x + width / 2, y + height);
                    wallpath.lineTo(x + width, y + height / 2);
                    break;
                case 4:
                    wallpath.moveTo(x + width / 2, y);
                    wallpath.lineTo(x + width / 2, y + height);
                    wallpath.lineTo(x, y + height / 2);
                    wallpath.moveTo(x + width / 2, y + height / 4);
                    wallpath.lineTo(x + width / 2, y + height / 4 + height / 2);
                    wallpath.lineTo(x + width, y + height / 4 + height / 2);
                    wallpath.lineTo(x + width, y + height / 4 - 5f);
                    wallpath.lineTo(x + width / 2, y + height / 4 - 5f);
                    break;
            }
        } else {
            wallpath.moveTo(x + width / 2, y + height / 8);
            wallpath.lineTo(x + width / 8, y + height / 2);
            wallpath.lineTo(x + width / 2, y + height * 7 / 8);
            wallpath.lineTo(x + width * 7 / 8, y + height / 2);
            wallpath.lineTo(x + width / 2, y + height / 8);
            switch (direction) {
                case 13:
                    wallpath.moveTo(x + width * 1 / 3, y);
                    wallpath.lineTo(x + width, y + height * 2 / 3);
                    wallpath.lineTo(x + width, y);
                    canvas.drawPath(wallpath, mPaint);
                    break;
                case 14:
                    wallpath.moveTo(x + width * 2 / 3, y);
                    wallpath.lineTo(x, y + height * 2 / 3);
                    wallpath.lineTo(x, y);
                    canvas.drawPath(wallpath, mPaint);
                    break;
                case 23:
                    wallpath.moveTo(x + width * 2 / 3, y + height);
                    wallpath.lineTo(x, y + height * 1 / 3);
                    wallpath.lineTo(x, y + height);
                    canvas.drawPath(wallpath, mPaint);
                    break;
                case 24:
                    wallpath.moveTo(x + width * 1 / 3, y + height);
                    wallpath.lineTo(x + width, y + height * 1 / 3);
                    wallpath.lineTo(x + width, y + height);
                    canvas.drawPath(wallpath, mPaint);
                    break;

            }
        }
        canvas.drawPath(wallpath, mPaint);

        MyCameraPreview.setImageBitmap(bitmap);
        bProcessing = false;
        countFrames++;
    }
}