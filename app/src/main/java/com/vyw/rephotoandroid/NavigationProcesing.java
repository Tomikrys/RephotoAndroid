package com.vyw.rephotoandroid;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;

/**
 * Created by acervenka2 on 24.04.2017.
 */

public class NavigationProcesing extends Activity
{
    private CameraPreview camPreview;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Set this APK Full screen
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        //Set this APK no title
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        setContentView(R.layout.main);

        ImageView myCameraPreview = new ImageView(this);

        SurfaceView camView = new SurfaceView(this);
        SurfaceHolder camHolder = camView.getHolder();
        int previewSizeWidth = 640;
        int previewSizeHeight = 480;
//        camPreview = new CameraPreview(previewSizeWidth, previewSizeHeight, myCameraPreview);


//        ActivityCompat.finishAffinity(this);
        Intent intent = new Intent(this, MainActivityDemoJava.class);
        startActivity(intent);
//        finish();
//        camHolder.addCallback((SurfaceHolder.Callback) camPreview);
//
//        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.frameLayout1);
//        mainLayout.addView(camView, new WindowManager.LayoutParams(previewSizeWidth, previewSizeHeight));
//        mainLayout.addView(myCameraPreview, new WindowManager.LayoutParams(previewSizeWidth, previewSizeHeight));
    }
    protected void onPause()
    {
        if ( camPreview != null);
        super.onPause();
    }
}