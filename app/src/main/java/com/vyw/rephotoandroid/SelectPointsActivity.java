package com.vyw.rephotoandroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by acervenka2 on 20.03.2017.
 */

public class SelectPointsActivity extends AppCompatActivity {

    public static String TAG = "SelectPointsActivity";

    ImageView imageView;

    Bitmap bit_first_frame;
    Bitmap bit_ref_frame;

    Mat first_frame;
    Mat ref_frame;

    boolean isRefImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_points);

        isRefImage = true;

        long firstFrameAddress = getIntent().getLongExtra("first_image", 0);
        long refFrameAddress = getIntent().getLongExtra("ref_image", 0);
        float pos_x = getIntent().getFloatExtra("x", 0);
        float pos_y = getIntent().getFloatExtra("y", 0);

        first_frame = new Mat(firstFrameAddress);
        ref_frame = new Mat(refFrameAddress);

        Bitmap.Config con_first_frame = Bitmap.Config.ARGB_4444;
        Bitmap.Config con_ref_frame = Bitmap.Config.ARGB_4444;

        bit_first_frame = Bitmap.createBitmap(first_frame.width(), first_frame.height(), con_first_frame);
        Utils.matToBitmap(first_frame, bit_first_frame);

        bit_ref_frame = Bitmap.createBitmap(ref_frame.width(), ref_frame.height(), con_ref_frame);
        Utils.matToBitmap(ref_frame, bit_ref_frame);

        imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setImageBitmap(bit_ref_frame);


        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bit_first_frame);
        canvas.drawPoint(pos_x, pos_y, paint);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isRefImage) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        float[] points = OpenCVNative.registrationPoints(event.getX(), event.getY());
                        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));
                        Utils.matToBitmap(first_frame, bit_first_frame);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStrokeWidth(15);
                        paint.setStyle(Paint.Style.FILL);
                        Canvas canvas = new Canvas(bit_first_frame);
                        canvas.drawPoint(points[0], points[1], paint);

                        paint.setColor(Color.BLUE);
                        Canvas canvas1 = new Canvas(bit_ref_frame);
                        //canvas1.drawPoint(event.getX(), event.getY(), paint);

                        float[] point = new float[] {event.getX(), event.getY()};

                        Matrix inverse = new Matrix();
                        imageView.getImageMatrix().invert(inverse);
                        inverse.mapPoints(point);

                        /*float density = getResources().getDisplayMetrics().density;
                        point[0] /= density;
                        point[1] /= density;*/

                        canvas1.drawPoint( point[0], point[1], paint);
                        Log.d(TAG, "Touch point to : " +point[0] + " x " + point[1]);
                        imageView.setImageBitmap(bit_ref_frame);
                    }
                }
                return true;
            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.change_view, menu);
        return true;
    }

    public void changeView(MenuItem item) {
        if (!isRefImage) {
            imageView.setImageBitmap(bit_ref_frame);
            isRefImage = true;
        } else {
            imageView.setImageBitmap(bit_first_frame);
            isRefImage = false;
        }
    }

    public void finishRegister(MenuItem item) {
        OpenCVNative.initNavigation();
        ActivityCompat.finishAffinity(this);
        Intent play = new Intent(this, NavigationProcesing.class);
        startActivity(play);
        finish();
    }

    public void exitApplication(MenuItem item) {
        finish();
    }

    public void nextPoint(MenuItem item) {
        float[] points = OpenCVNative.nextPoint();
        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));

        Utils.matToBitmap(first_frame, bit_first_frame);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bit_first_frame);
        canvas.drawPoint(points[0], points[1], paint);

    }
}
