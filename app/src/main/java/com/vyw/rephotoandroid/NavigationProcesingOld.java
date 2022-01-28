//package com.delaroystudios.camera;
package com.vyw.rephotoandroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.vyw.rephotoandroid.CameraPreview;
import com.vyw.rephotoandroid.R;

/**
 * Created by acervenka2 on 24.04.2017.
 */

public class NavigationProcesingOld extends Activity
{
    private CameraPreview camPreview;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Set this APK Full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Set this APK no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        ImageView myCameraPreview = new ImageView(this);

        SurfaceView camView = new SurfaceView(this);
        SurfaceHolder camHolder = camView.getHolder();
        int previewSizeWidth = 640;
        int previewSizeHeight = 480;
//        camPreview = new CameraPreview(previewSizeWidth, previewSizeHeight, myCameraPreview);

//        camHolder.addCallback(camPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        FrameLayout mainLayout = (FrameLayout) findViewById(R.id.frameLayout1);
        mainLayout.addView(camView, new WindowManager.LayoutParams(previewSizeWidth, previewSizeHeight));
        mainLayout.addView(myCameraPreview, new WindowManager.LayoutParams(previewSizeWidth, previewSizeHeight));
    }
    protected void onPause()
    {
        if ( camPreview != null)
//            camPreview.onPause();
        super.onPause();
    }
}