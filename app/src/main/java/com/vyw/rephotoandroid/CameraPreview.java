package com.vyw.rephotoandroid;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;

public class CameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static String TAG = "CameraPreview";

    private Camera mCamera = null;
    private ImageView MyCameraPreview = null;
    private Bitmap bitmap = null;

    private int imageFormat;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private boolean bProcessing = false;

    public int countFrames = 1;

    Handler mHandler = new Handler(Looper.getMainLooper());

    public CameraPreview(int PreviewlayoutWidth, int PreviewlayoutHeight,
                         ImageView CameraPreview) {
        PreviewSizeWidth = PreviewlayoutWidth;
        PreviewSizeHeight = PreviewlayoutHeight;
        MyCameraPreview = CameraPreview;
        bitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onPreviewFrame(byte[] arg0, Camera arg1) {
        // At preview mode, the frame data will push to here.
        if (imageFormat == ImageFormat.NV21) {
            //We only accept the NV21(YUV420) format.
            if (!bProcessing) {
                mHandler.post(DoImageProcessing);
            }
        }
    }

    public void onPause() {
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Camera.Parameters parameters;

        parameters = mCamera.getParameters();
        // Set the camera preview size
        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);

        imageFormat = parameters.getPreviewFormat();

        mCamera.setParameters(parameters);
        if (this.MyCameraPreview.getHeight() > this.MyCameraPreview.getWidth()) {
            mCamera.setDisplayOrientation(270);
        }

        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        // todo nefunguje camera
        mCamera = Camera.open();
        try {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    static {
        System.loadLibrary("native-lib");
    }

    private Runnable DoImageProcessing = new Runnable() {
        public void run() {

            bProcessing = true;
            Canvas canvas = new Canvas(bitmap);
            Paint mPaint = new Paint();
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.WHITE);
            mPaint.setStrokeWidth(4);
            float width = 150f;
            float height = 150f;
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
                direction = OpenCVNative.processNavigation(currentFrames.getNativeObjAddr(), 1);
                Log.d(TAG, "Value of direction is: " + direction);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getMessage());
                direction = 1;
            }


            //1 up, 2, down, 3 right, 4 left;
            Path wallpath = new Path();
            wallpath.reset();
            if (direction < 10) {
                switch (direction) {
                    case 0:
                        wallpath.moveTo(x, y + height / 8);
                        wallpath.lineTo(x + width / 8, y);
                        wallpath.lineTo(x + width, y + height * 7 / 8);
                        wallpath.lineTo(x + width * 7 / 8, y + height);

                        wallpath.moveTo(x+width, y + height / 8);
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
    };


}