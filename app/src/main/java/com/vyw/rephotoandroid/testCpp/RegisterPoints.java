package com.vyw.rephotoandroid.testCpp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vyw.rephotoandroid.OpenCVNative;
import com.vyw.rephotoandroid.R;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.w3c.dom.Text;

public class RegisterPoints extends AppCompatActivity {

    public static String TAG = "RegisterPoints";

    ImageView refImageView;
    ImageView firstImageView;
    LinearLayout wrapperRefImage;
    LinearLayout wrapperFirstImage;
    ImageView magnifierImageView;

    Bitmap bt_first_frame;
    Bitmap bt_ref_frame;

    Mat first_frame;
    Mat ref_frame;

    String selected_points_coordinates = "";

    private ScaleGestureDetector refImageScaleGestureDetector;
    private float refImageLastTouchX;
    private float refImageLastTouchY;
    private float refImageLastTranslationX;
    private float refImageLastTranslationY;
    private ScaleGestureDetector firstImageScaleGestureDetector;
    private float firstImageLastTouchX;
    private float firstImageLastTouchY;
    private float firstImageLastTranslationX;
    private float firstImageLastTranslationY;
    private TextView textView;
    float firstImageScaleFactor = 1.0f;
    float refImageScaleFactor = 1.0f;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_points);

//        long firstFrameAddress = getIntent().getLongExtra("first_image", 0);
//        long refFrameAddress = getIntent().getLongExtra("ref_image", 0);

        float[] points = OpenCVNative.registration_init();
        float pos_x = points[0];
        float pos_y = points[1];

//        first_frame = new Mat(firstFrameAddress);
//        ref_frame = new Mat(refFrameAddress);
//
//        Bitmap.Config con_first_frame = Bitmap.Config.ARGB_4444;
//        Bitmap.Config con_ref_frame = Bitmap.Config.ARGB_4444;
//
//        bt_first_frame = Bitmap.createBitmap(first_frame.width(), first_frame.height(), con_first_frame);
//        Utils.matToBitmap(first_frame, bt_first_frame);
//
//        bt_ref_frame = Bitmap.createBitmap(ref_frame.width(), ref_frame.height(), con_ref_frame);
//        Utils.matToBitmap(ref_frame, bt_ref_frame);

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        String first_frame_filePath = getIntent().getStringExtra("first_image");
        Bitmap bt_first_frame = BitmapFactory.decodeFile(first_frame_filePath, options);
        String ref_frame_filePath = getIntent().getStringExtra("ref_image");
        Bitmap bt_ref_frame = BitmapFactory.decodeFile(ref_frame_filePath, options);

        wrapperRefImage = (LinearLayout) findViewById(R.id.wrapper_ref_image);
        refImageView = (ImageView) findViewById(R.id.ref_image);
        refImageView.setImageBitmap(bt_ref_frame);

        wrapperFirstImage = (LinearLayout) findViewById(R.id.wrapper_first_image);
        firstImageView = (ImageView) findViewById(R.id.first_image);
        firstImageView.setImageBitmap(bt_first_frame);

        magnifierImageView = (ImageView) findViewById(R.id.magnifier_image);
        magnifierImageView.setVisibility(View.INVISIBLE);
        magnifierImageView.setImageBitmap(bt_first_frame);
        magnifierImageView.setScaleX(5.0f);
        magnifierImageView.setScaleY(5.0f);

        draw_point(pos_x, pos_y, bt_first_frame, Color.RED);

        textView = (TextView) findViewById(R.id.textView);

        refImageScaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                refImageScaleFactor *= detector.getScaleFactor();
                refImageScaleFactor = Math.max(1.0f, Math.min(refImageScaleFactor, 10.0f));

                refImageView.setScaleX(refImageScaleFactor);
                refImageView.setScaleY(refImageScaleFactor);

                return true;
            }
        });
        firstImageScaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                firstImageScaleFactor *= detector.getScaleFactor();
                firstImageScaleFactor = Math.max(1.0f, Math.min(firstImageScaleFactor, 10.0f));

                firstImageView.setScaleX(firstImageScaleFactor);
                firstImageView.setScaleY(firstImageScaleFactor);

                return true;
            }
        });
        firstImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                firstImageScaleGestureDetector.onTouchEvent(event);
                if (firstImageScaleGestureDetector.isInProgress()) {
                    // Scaling is happening, prevent any other positioning
                    return true;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        firstImageLastTouchX = event.getX();
                        firstImageLastTouchY = event.getY();
                        firstImageLastTranslationX = firstImageView.getTranslationX();
                        firstImageLastTranslationY = firstImageView.getTranslationY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = (event.getX() - firstImageLastTouchX) * firstImageScaleFactor;
                        float deltaY = (event.getY() - firstImageLastTouchY) * firstImageScaleFactor;
                        firstImageView.setTranslationX(firstImageLastTranslationX + deltaX);
                        firstImageView.setTranslationY(firstImageLastTranslationY + deltaY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Get the x and y coordinates of the touch event
                        float x = event.getX();
                        float y = event.getY();

                        // Get the bitmap that is displayed in the ImageView
                        Bitmap bitmap = ((BitmapDrawable) firstImageView.getDrawable()).getBitmap();

                        // Calculate the corresponding pixel coordinates
                        int pixelX = (int) (x * bitmap.getWidth() / firstImageView.getWidth());
                        int pixelY = (int) (y * bitmap.getHeight() / firstImageView.getHeight());

                        // Do something with the pixel coordinates (e.g. display them in a TextView)
                        textView.setText("Pixel coordinates: (" + pixelX + ", " + pixelY + ")");
                        break;
                }
                return true;
            }
        });


        refImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                refImageScaleGestureDetector.onTouchEvent(event);
                if (refImageScaleGestureDetector.isInProgress()) {
                    // Scaling is happening, prevent any other positioning
                    return true;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        refImageLastTouchX = event.getX();
                        refImageLastTouchY = event.getY();
                        refImageLastTranslationX = refImageView.getTranslationX();
                        refImageLastTranslationY = refImageView.getTranslationY();
                        magnifierImageView.setVisibility(View.VISIBLE);
                        firstImageView.setVisibility(View.INVISIBLE);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getX() - refImageLastTouchX;
                        float deltaY = event.getY() - refImageLastTouchY;
                        refImageView.setTranslationX(refImageLastTranslationX + deltaX);
                        refImageView.setTranslationY(refImageLastTranslationY + deltaY);
                        break;
                    case MotionEvent.ACTION_UP:
                        magnifierImageView.setVisibility(View.INVISIBLE);
                        firstImageView.setVisibility(View.VISIBLE);
                        break;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    double imageViewHeight = refImageView.getHeight();
                    double imageViewWidth = refImageView.getWidth();
                    double imageRealHeight = bt_ref_frame.getHeight();
                    double imageRealWidth = bt_ref_frame.getWidth();
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

                    centerPixelOnImageView(magnifierImageView,
                            (int) Math.round(realX),
                            (int) Math.round(realY));

//                        OPENCVNATIVECALL
                    selected_points_coordinates += "OpenCVNative.registration_register_point(" + realX + ", " + realY + ");\n";
                    float[] points = OpenCVNative.registration_register_point(realX, realY);
//                        float[] points = {event.getX(), event.getY()}; // smazat
                    Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));
//                    Utils.matToBitmap(first_frame, bt_first_frame);

                    draw_point(points[0], points[1], bt_first_frame, Color.RED);

                    float[] touched_point = new float[]{event.getX(), event.getY()};

                    Matrix inverse = new Matrix();
                    refImageView.getImageMatrix().invert(inverse);
                    inverse.mapPoints(touched_point);

                        /*float density = getResources().getDisplayMetrics().density;
                        point[0] /= density;
                        point[1] /= density;*/

                    draw_point(touched_point[0], touched_point[1], bt_ref_frame, Color.BLUE);

                    Log.d(TAG, "Touch point to : " + touched_point[0] + " x " + touched_point[1]);
                    refImageView.setImageBitmap(bt_ref_frame);
                }
                return true;
            }
        });
    }

    private void draw_point(float pos_x, float pos_y, Bitmap bt_first_frame, int color) {
        int sw = 1; //stroke_weight
        int fw = 1; //filling_weight / 2
        int fwsw = fw + sw;
        int cross_size = 25;
        int center_space = 7;

        Paint paint_stroke = new Paint();
        paint_stroke.setColor(0x77000000);
        paint_stroke.setStrokeWidth(0);
        paint_stroke.setStyle(Paint.Style.FILL);

        Paint paint_fill = new Paint();
        paint_fill.setColor(color);
        paint_fill.setStrokeWidth(0);
        paint_fill.setStyle(Paint.Style.FILL);

        Canvas canvas = new Canvas(bt_first_frame);

        // LEFT
        // stroke
        canvas.drawRect(pos_x - cross_size,
                pos_y - fwsw,
                pos_x - center_space,
                pos_y + fwsw,
                paint_stroke);

        // filling
        canvas.drawRect(pos_x - cross_size + sw,
                pos_y - fw,
                pos_x - center_space - sw,
                pos_y + fw,
                paint_fill);

        // RIGHT
        // stroke
        canvas.drawRect(pos_x + cross_size,
                pos_y - fwsw,
                pos_x + center_space,
                pos_y + fwsw,
                paint_stroke);

        // filling
        canvas.drawRect(pos_x + cross_size - sw,
                pos_y - fw,
                pos_x + center_space + sw,
                pos_y + fw,
                paint_fill);

        // DOWN
        // stroke
        canvas.drawRect(pos_x - fwsw,
                pos_y + cross_size,
                pos_x + fwsw,
                pos_y + center_space,
                paint_stroke);

        // filling
        canvas.drawRect(pos_x - fw,
                pos_y + cross_size - sw,
                pos_x + fw,
                pos_y + center_space + sw,
                paint_fill);

        // UP
        // stroke
        canvas.drawRect(pos_x - fwsw,
                pos_y - cross_size,
                pos_x + fwsw,
                pos_y - center_space,
                paint_stroke);

        // filling
        canvas.drawRect(pos_x - fw,
                pos_y - center_space - sw,
                pos_x + fw,
                pos_y - cross_size + sw,
                paint_fill);

//        canvas.drawLine(pos_x - cross_size, pos_y, pos_x - center_space, pos_y, paint);
//        canvas.drawLine(pos_x + center_space, pos_y, pos_x + cross_size, pos_y, paint_fill);
//        canvas.drawLine(pos_x, pos_y - cross_size, pos_x, pos_y - center_space, paint_fill);
//        canvas.drawLine(pos_x, pos_y + center_space, pos_x, pos_y + cross_size, paint_fill);
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
//        if (!isRefImage) {
//            refImageView.setImageBitmap(bt_ref_frame);
//            isRefImage = true;
//        } else {
//            firstImageView.setImageBitmap(bt_first_frame);
//            isRefImage = false;
//        }
    }

    public void finishRegister(MenuItem item) {
//        OPENCVNATIVECALL
        selected_points_coordinates += "ALES %%%%%%%%%%%%%%%%%%%%%%%%\n";

        OpenCVNative.navigation_init();
        Intent intent = new Intent();
        intent.putExtra("NAVIGATION_CAN_START", true);
        setResult(RESULT_OK, intent);

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

//        Utils.matToBitmap(first_frame, bt_first_frame);
        draw_point(points[0], points[1], bt_first_frame, Color.RED);
    }

    public void nextPointButton(View view) {
//    OPENCVNATIVECALL
        selected_points_coordinates += "OpenCVNative.registration_next_point();\n";
        float[] points = OpenCVNative.registration_next_point();
//        float[] points = {50, 50}; // smazat
        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));

//        Utils.matToBitmap(first_frame, bt_first_frame);
        draw_point(points[0], points[1], bt_first_frame, Color.RED);
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

    public void centerPixelOnImageView(ImageView imageView, int x, int y) {
        // Get the bitmap from the ImageView
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        // Get the width and height of the ImageView
        int imageViewWidth = imageView.getWidth();
        int imageViewHeight = imageView.getHeight();
        Log.d("RegisterPoints", "imageViewWidth" + imageViewWidth);
        Log.d("RegisterPoints", "imageViewHeight" + imageViewHeight);

        // Get the width and height of the bitmap
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        Log.d("RegisterPoints", "bitmapWidth" + bitmapWidth);
        Log.d("RegisterPoints", "bitmapHeight" + bitmapHeight);

        // Get the current scale factor of the ImageView
        float scaleX = imageView.getScaleX();
        float scaleY = imageView.getScaleY();

        // Calculate the center x and y coordinates of the ImageView
        int centerX = (int) (imageViewWidth / 2.0f);
        int centerY = (int) (imageViewHeight / 2.0f);
        Log.d("RegisterPoints", "centerY" + centerY);
        Log.d("RegisterPoints", "centerY" + centerY);

        // Calculate the x and y offsets to center the pixel
        int xOffset = centerX - x;
        int yOffset = centerY - y;

        // Calculate the new top-left coordinates of the bitmap inside the ImageView
        int newLeft = (int) (xOffset * scaleX);
        int newTop = (int) (yOffset * scaleY);

        // Check if the bitmap is smaller than the ImageView
        if (bitmapWidth * scaleX < imageViewWidth) {
            newLeft = (int) ((imageViewWidth / 2.0f) - (bitmapWidth / 2.0f * scaleX));
        }
        if (bitmapHeight * scaleY < imageViewHeight) {
            newTop = (int) ((imageViewHeight / 2.0f) - (bitmapHeight / 2.0f * scaleY));
        }

        // Set the new translation coordinates of the bitmap inside the ImageView
        imageView.setTranslationX(newLeft);
        imageView.setTranslationY(newTop);
        Log.d("RegisterPoints", "newLeft" + newLeft);
        Log.d("RegisterPoints", "newTop" + newTop);
    }
}
