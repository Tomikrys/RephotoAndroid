package com.vyw.rephotoandroid.testCpp;

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
import android.widget.LinearLayout;

import com.vyw.rephotoandroid.OpenCVNative;
import com.vyw.rephotoandroid.R;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public class RegisterPoints extends AppCompatActivity {

    public static String TAG = "RegisterPoints";

    ImageView refImageView;
    ImageView firstImageView;
    LinearLayout wrapperRefImage;
    LinearLayout wrapperFirstImage;

    Bitmap bit_first_frame;
    Bitmap bit_ref_frame;

    Mat first_frame;
    Mat ref_frame;

    boolean isRefImage;

    String selected_points_coordinates = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_points);

        isRefImage = true;

        long firstFrameAddress = getIntent().getLongExtra("first_image", 0);
        long refFrameAddress = getIntent().getLongExtra("ref_image", 0);

        float[] points = OpenCVNative.registration_init();
        float pos_x = points[0];
        float pos_y = points[1];

        first_frame = new Mat(firstFrameAddress);
        ref_frame = new Mat(refFrameAddress);

        Bitmap.Config con_first_frame = Bitmap.Config.ARGB_4444;
        Bitmap.Config con_ref_frame = Bitmap.Config.ARGB_4444;

        bit_first_frame = Bitmap.createBitmap(first_frame.width(), first_frame.height(), con_first_frame);
        Utils.matToBitmap(first_frame, bit_first_frame);

        bit_ref_frame = Bitmap.createBitmap(ref_frame.width(), ref_frame.height(), con_ref_frame);
        Utils.matToBitmap(ref_frame, bit_ref_frame);

        wrapperRefImage = (LinearLayout) findViewById(R.id.wrapper_ref_image);
        refImageView = (ImageView) findViewById(R.id.ref_image);
        refImageView.setImageBitmap(bit_ref_frame);

        wrapperFirstImage = (LinearLayout) findViewById(R.id.wrapper_first_image);
        firstImageView = (ImageView) findViewById(R.id.first_image);
        firstImageView.setImageBitmap(bit_first_frame);


        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bit_first_frame);
        canvas.drawPoint(pos_x, pos_y, paint);


        refImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isRefImage) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        double imageViewHeight = refImageView.getHeight();
                        double imageViewWidth = refImageView.getWidth();
                        double imageRealHeight = bit_ref_frame.getHeight();
                        double imageRealWidth = bit_ref_frame.getWidth();
                        double imageDisplayedHeight;
                        double imageDisplayedWidth;
//                      https://stackoverflow.com/questions/12463155/get-the-displayed-size-of-an-image-inside-an-imageview
                        if (imageViewHeight * imageRealWidth <= imageViewWidth * imageRealHeight) {
                            imageDisplayedWidth = imageRealWidth * imageViewHeight / imageRealHeight;
                            imageDisplayedHeight = imageViewHeight;
                        } else {
                            imageDisplayedHeight = imageRealHeight * imageViewWidth / imageRealWidth;
                            imageDisplayedWidth = imageViewWidth;
                        }

                        double x = event.getX();
                        double y = event.getY();
                        double realX = ((x - ((imageViewWidth - imageDisplayedWidth) / 2)) / imageDisplayedWidth) * imageRealWidth;
                        double realY = ((y - ((imageViewHeight - imageDisplayedHeight) / 2)) / imageDisplayedHeight) * imageRealHeight;

//                        OPENCVNATIVECALL
                        selected_points_coordinates += "OpenCVNative.registration_register_point(" + realX + ", " + realY + ");\n";
                        float[] points = OpenCVNative.registration_register_point(realX, realY);
//                        float[] points = {event.getX(), event.getY()}; // smazat
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
//                        canvas1.drawPoint(event.getX(), event.getY(), paint);

                        float[] point = new float[]{event.getX(), event.getY()};

                        Matrix inverse = new Matrix();
                        refImageView.getImageMatrix().invert(inverse);
                        inverse.mapPoints(point);

                        /*float density = getResources().getDisplayMetrics().density;
                        point[0] /= density;
                        point[1] /= density;*/

                        canvas1.drawPoint(point[0], point[1], paint);
                        Log.d(TAG, "Touch point to : " + point[0] + " x " + point[1]);
                        refImageView.setImageBitmap(bit_ref_frame);
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
            refImageView.setImageBitmap(bit_ref_frame);
            isRefImage = true;
        } else {
            firstImageView.setImageBitmap(bit_first_frame);
            isRefImage = false;
        }
    }

    public void finishRegister(MenuItem item) {
//        OPENCVNATIVECALL
        selected_points_coordinates += "ALES %%%%%%%%%%%%%%%%%%%%%%%%\n";

        OpenCVNative.navigation_init();

//        ActivityCompat.finishAffinity(this);
//        Intent play = new Intent(this, CameraPreview.class);
//        startActivity(play);
        finish();
    }

    public void exitApplication(MenuItem item) {
        finish();
    }

    public void nextPoint(MenuItem item) {
//    OPENCVNATIVECALL
        float[] points = OpenCVNative.registration_next_point();
//        float[] points = {50, 50}; // smazat
        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));

        Utils.matToBitmap(first_frame, bit_first_frame);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bit_first_frame);
        canvas.drawPoint(points[0], points[1], paint);
    }

    public void nextPointButton(View view) {
//    OPENCVNATIVECALL
        selected_points_coordinates += "OpenCVNative.registration_next_point();\n";
        float[] points = OpenCVNative.registration_next_point();
//        float[] points = {50, 50}; // smazat
        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));

        Utils.matToBitmap(first_frame, bit_first_frame);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.FILL);
        Canvas canvas = new Canvas(bit_first_frame);
        canvas.drawPoint(points[0], points[1], paint);
    }

    public void quickInput(View view) {
//        ######### BYT ##################
        OpenCVNative.registration_register_point(1068.62, 1256.12);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(1053.58, 1082.94);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(184.48, 48.16);
        OpenCVNative.registration_register_point(211.59, 329.82);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(214.62, 920.27);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(999.39, 1090.49);
        OpenCVNative.registration_register_point(268.83, 875.06);
        OpenCVNative.registration_register_point(261.29, 337.37);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(308.00, 789.24);
        for (int i = 0; i < 6; i++) {
            OpenCVNative.registration_next_point();
        }
        OpenCVNative.registration_register_point(315.50, 870.57);
        for (int i = 0; i < 9; i++) {
            OpenCVNative.registration_next_point();
        }
        OpenCVNative.registration_register_point(365.25, 766.59);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(357.70, 240.93);
        OpenCVNative.registration_register_point(362.22, 319.20);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(407.39, 349.41);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(930.06, 1117.53);
        OpenCVNative.registration_next_point();
        OpenCVNative.registration_register_point(952.65, 346.35);

//      ######### POSTA ##################

        OpenCVNative.navigation_init();
        finish();
    }
}
