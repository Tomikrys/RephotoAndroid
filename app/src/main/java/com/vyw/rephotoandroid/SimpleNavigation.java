package com.vyw.rephotoandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class SimpleNavigation extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private static Bitmap refImageCopy;
    private PreviewView previewView;
    private ImageView refImage;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageView cameraPreview = null;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private static String TAG = "SimpleNavigation";
    private float origX = 0;
    private float origY = 0;

    private float newAlpha = 1;
    private float origAlpha = 1;
    private float newCrop = 0;
    private float origCrop = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.simple_navigation);
        getSupportActionBar().hide();
        previewView = findViewById(R.id.previewView);
        refImage = findViewById(R.id.refImage);
        BitmapDrawable drawable = (BitmapDrawable) refImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        refImageCopy = bitmap;
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
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

        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
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
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                imageProxy.close();
            }
        });
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
//                textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
    }


    // This example shows an Activity, but you would use the same approach if
// you were subclassing a View.
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        final int action = motionEvent.getActionMasked();

        float x = motionEvent.getX();
        float y = motionEvent.getY();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float sensitivity = (float) 0.4;
        int screenSmallerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        float referenceSize = screenSmallerSize * sensitivity;

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
                Log.d(TAG,"Action was DOWN");
                origX = x;
                origY = y;
                origAlpha = newAlpha;
                origCrop = newCrop;
                return true;
            case (MotionEvent.ACTION_MOVE) :
                float xDiff = (origX-x);
                float yDiff = (origY-y);
                Log.d(TAG,"Action was MOVE " + xDiff + " " + yDiff);
                newAlpha = origAlpha + (yDiff / referenceSize);
                newAlpha = newAlpha>1 ? 1 : newAlpha<0 ? 0 : newAlpha;
                Log.d(TAG,"newAlpha " + newAlpha);

                newCrop = origCrop + (xDiff / referenceSize);
                newCrop = newCrop>1 ? 1 : newCrop<0 ? 0 : newCrop;
                Log.d(TAG,"newCrop " + newCrop);

                refImage.setImageBitmap(cropAndSetTransparency(newCrop, newAlpha));
                return true;
            case (MotionEvent.ACTION_UP) :
                Log.d(TAG,"Action was UP");
                origAlpha = newAlpha;
                origCrop = newCrop;
                return true;
            case (MotionEvent.ACTION_CANCEL) :
                Log.d(TAG,"Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                Log.d(TAG,"Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default :
                return super.onTouchEvent(motionEvent);
        }
    }

    public static Bitmap cropAndSetTransparency(double newCrop, double newAlpha) {
        int width =  refImageCopy.getWidth();
        int height = refImageCopy.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int [] allpixels = new int [myBitmap.getHeight()*myBitmap.getWidth()];
        refImageCopy.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),myBitmap.getHeight());
        myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height);

        int transparency = (int) Math.round(newAlpha * 255);
        int numberOfPixels = (int) Math.round(newCrop * width);

        for(int i = 0; i < myBitmap.getHeight()*myBitmap.getWidth(); i++){
            boolean shouldBeCropped = width-numberOfPixels <= (i % myBitmap.getWidth());
            allpixels[i] = shouldBeCropped ? Color.alpha(Color.TRANSPARENT) : setAplha(allpixels[i], transparency);
        }

        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        return myBitmap;
    }

    public static int setAplha(int pixel, int transparency) {
        return (pixel & 0x00ffffff) | transparency << 24;
    }
}