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
import android.view.ViewTreeObserver;
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
//    ImageView magnifierImageView;

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

        refImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                centerPixelOnImageView(refImageView, pos_x, pos_y, 3.0f);
                refImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        wrapperFirstImage = (LinearLayout) findViewById(R.id.wrapper_first_image);
        firstImageView = (ImageView) findViewById(R.id.first_image);
        firstImageView.setImageBitmap(bt_first_frame);

        firstImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                centerPixelOnImageView(firstImageView, pos_x, pos_y, 3.0f);
                firstImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        draw_point(pos_x, pos_y, bt_first_frame, Color.RED);


//        magnifierImageView = (ImageView) findViewById(R.id.magnifier_image);
//        magnifierImageView.setVisibility(View.INVISIBLE);
//        magnifierImageView.setImageBitmap(bt_first_frame);
//        magnifierImageView.setScaleX(5.0f);
//        magnifierImageView.setScaleY(5.0f);

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
                        firstImageLastTouchX = event.getRawX();
                        firstImageLastTouchY = event.getRawY();
                        firstImageLastTranslationX = firstImageView.getTranslationX();
                        firstImageLastTranslationY = firstImageView.getTranslationY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = (event.getRawX() - firstImageLastTouchX);
                        float deltaY = (event.getRawY() - firstImageLastTouchY);
                        float translationX = firstImageLastTranslationX + deltaX;
                        float translationY = firstImageLastTranslationY + deltaY;
                        firstImageView.setTranslationX(translationX);
                        firstImageView.setTranslationY(translationY);
                        Log.d(TAG, "event: " + event.getX() + " " + event.getY());
//                        Log.d(TAG, "refImageLastTouch: " + firstImageLastTouchX + " " + firstImageLastTouchY);
//                        Log.d(TAG, "delta: " + deltaX + " " + deltaY);
//                        Log.d(TAG, "refImageLastTranslation: " + firstImageLastTranslationX + " " + firstImageLastTranslationY);
//                        Log.d(TAG, "translation: " + translationX + " " + translationY);
                        return true;

//                    case MotionEvent.ACTION_UP:
//                        // Get the x and y coordinates of the touch event
//                        float x = event.getX();
//                        float y = event.getY();
//
//                        // Get the bitmap that is displayed in the ImageView
//                        Bitmap bitmap = ((BitmapDrawable) firstImageView.getDrawable()).getBitmap();
//
//                        // Calculate the corresponding pixel coordinates
//                        int pixelX = (int) (x * bitmap.getWidth() / firstImageView.getWidth());
//                        int pixelY = (int) (y * bitmap.getHeight() / firstImageView.getHeight());
//
//                        // Do something with the pixel coordinates (e.g. display them in a TextView)
//                        textView.setText("Pixel coordinates: (" + pixelX + ", " + pixelY + ")");
//                        break;
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
                        refImageLastTouchX = event.getRawX();
                        refImageLastTouchY = event.getRawY();
                        refImageLastTranslationX = refImageView.getTranslationX();
                        refImageLastTranslationY = refImageView.getTranslationY();
//                        magnifierImageView.setVisibility(View.VISIBLE);
//                        firstImageView.setVisibility(View.INVISIBLE);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = (event.getRawX() - refImageLastTouchX);
                        float deltaY = (event.getRawY() - refImageLastTouchY);
                        float translationX = refImageLastTranslationX + deltaX;
                        float translationY = refImageLastTranslationY + deltaY;
                        refImageView.setTranslationX(translationX);
                        refImageView.setTranslationY(translationY);
//                        Log.d(TAG, "event: " + event.getX() + " " + event.getY());
//                        Log.d(TAG, "rawEvent: " + event.getRawX() + " " + event.getRawY());
//                        Log.d(TAG, "refImageLastTouch: " + refImageLastTouchX + " " + refImageLastTouchY);
//                        Log.d(TAG, "delta: " + deltaX + " " + deltaY);
//                        Log.d(TAG, "refImageLastTranslation: " + refImageLastTranslationX + " " + refImageLastTranslationY);
//                        Log.d(TAG, "translation: " + translationX + " " + translationY);
                        break;
                    case MotionEvent.ACTION_UP:
//                        magnifierImageView.setVisibility(View.INVISIBLE);
//                        firstImageView.setVisibility(View.VISIBLE);
                        break;
                }
                return true;
            }

            ;
        });


//        refImageView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                refImageScaleGestureDetector.onTouchEvent(event);
//                if (refImageScaleGestureDetector.isInProgress()) {
//                    // Scaling is happening, prevent any other positioning
//                    return true;
//                }
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        refImageLastTouchX = event.getX();
//                        refImageLastTouchY = event.getY();
//                        refImageLastTranslationX = refImageView.getTranslationX();
//                        refImageLastTranslationY = refImageView.getTranslationY();
//                        magnifierImageView.setVisibility(View.VISIBLE);
//                        firstImageView.setVisibility(View.INVISIBLE);
//                        break;
//
//                    case MotionEvent.ACTION_MOVE:
//                        float deltaX = event.getX() - refImageLastTouchX;
//                        float deltaY = event.getY() - refImageLastTouchY;
//                        refImageView.setTranslationX(refImageLastTranslationX + deltaX);
//                        refImageView.setTranslationY(refImageLastTranslationY + deltaY);
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        magnifierImageView.setVisibility(View.INVISIBLE);
//                        firstImageView.setVisibility(View.VISIBLE);
//                        break;
//                }
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    double imageViewHeight = refImageView.getHeight();
//                    double imageViewWidth = refImageView.getWidth();
//                    double imageRealHeight = bt_ref_frame.getHeight();
//                    double imageRealWidth = bt_ref_frame.getWidth();
//                    double imageDisplayedHeight;
//                    double imageDisplayedWidth;
////                      https://stackoverflow.com/questions/12463155/get-the-displayed-size-of-an-image-inside-an-imageview
//                    if (imageViewHeight * imageRealWidth <= imageViewWidth * imageRealHeight) {
//                        imageDisplayedWidth = imageRealWidth * imageViewHeight / imageRealHeight;
//                        imageDisplayedHeight = imageViewHeight;
//                    } else {
//                        imageDisplayedHeight = imageRealHeight * imageViewWidth / imageRealWidth;
//                        imageDisplayedWidth = imageViewWidth;
//                    }
//
//                    double x = event.getX();
//                    double y = event.getY();
//                    double realX = ((x - ((imageViewWidth - imageDisplayedWidth) / 2)) / imageDisplayedWidth) * imageRealWidth;
//                    double realY = ((y - ((imageViewHeight - imageDisplayedHeight) / 2)) / imageDisplayedHeight) * imageRealHeight;
//
//                    centerPixelOnImageView(magnifierImageView,
//                            (int) Math.round(realX),
//                            (int) Math.round(realY));

//                }
//                return true;
//            }
//        });
    }


    public void confirmPoint(View view) {
        Bitmap bitmap = ((BitmapDrawable) refImageView.getDrawable()).getBitmap();

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();

        float imageViewWidth = refImageView.getWidth();
        float imageViewHeight = refImageView.getHeight();

        float widthRatio = (imageViewHeight / bitmapHeight);
        float heightRatio = (imageViewWidth / bitmapWidth);

        float bitmapDisplayedWidth = bitmapWidth * widthRatio;
        float bitmapDisplayedHeight = bitmapHeight * heightRatio;

        float cornerX = -(bitmapDisplayedWidth / 2);
        float cornerY = -(bitmapDisplayedHeight / 2);

        float scaleImageView = refImageView.getScaleX();

        float centerBitmapX = bitmapWidth - (((refImageView.getTranslationX() / scaleImageView) - cornerX) / widthRatio);
        float centerBitmapY = bitmapHeight - (((refImageView.getTranslationY() / scaleImageView) - cornerY) / heightRatio);

        draw_point(centerBitmapX, centerBitmapY, bitmap, Color.MAGENTA);
        register_point(centerBitmapX, centerBitmapY);
    }

    private void register_point(float x, float y) {
        //                        OPENCVNATIVECALL
        selected_points_coordinates += "OpenCVNative.registration_register_point(" + x + ", " + y + ");\n";
        float[] points = OpenCVNative.registration_register_point(x, y);
//                        float[] points = {event.getX(), event.getY()}; // smazat
        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));
//                    Utils.matToBitmap(first_frame, bt_first_frame);

        Bitmap first_image_bitmap = ((BitmapDrawable) firstImageView.getDrawable()).getBitmap();
        draw_point(points[0], points[1], first_image_bitmap, Color.RED);
        focusOnCross(points[0], points[1]);
//        float[] touched_point = new float[]{event.getX(), event.getY()};
//
//        Matrix inverse = new Matrix();
//        refImageView.getImageMatrix().invert(inverse);
//        inverse.mapPoints(touched_point);
//
//                        /*float density = getResources().getDisplayMetrics().density;
//                        point[0] /= density;
//                        point[1] /= density;*/
//
//        draw_point(touched_point[0], touched_point[1], bt_ref_frame, Color.BLUE);
//
//        Log.d(TAG, "Touch point to : " + touched_point[0] + " x " + touched_point[1]);
//        refImageView.setImageBitmap(bt_ref_frame);
    }

    public void focusOnCross(float x, float y) {
        centerPixelOnImageView(refImageView, x, y, 4.0f);
        centerPixelOnImageView(firstImageView, x, y, 4.0f);
    }

    public void centerPixelOnImageView(ImageView imageView, float x, float y, float scale) {
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);

        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();

        float imageViewWidth = imageView.getWidth();
        float imageViewHeight = imageView.getHeight();

        float widthRatio = (imageViewHeight / bitmapHeight);
        float heightRatio = (imageViewWidth / bitmapWidth);

        float bitmapDisplayedWidth = bitmapWidth * widthRatio;
        float bitmapDisplayedHeight = bitmapHeight * heightRatio;

        float cornerX = -(bitmapDisplayedWidth / 2);
        float cornerY = -(bitmapDisplayedHeight / 2);

        float positionX = (cornerX + (x * widthRatio)) * scale;
        float positionY = (cornerY + (y * heightRatio)) * scale;

        float centerImageViewX = imageViewWidth / 2;
        float centerImageViewY = imageViewHeight / 2;

        float distanceX = centerImageViewX - (positionX + bitmapDisplayedWidth / 2);
        float distanceY = centerImageViewY - (positionY + bitmapDisplayedHeight / 2);

        imageView.setTranslationX(distanceX);
        imageView.setTranslationY(distanceY);
    }

    private void draw_point(float pos_x, float pos_y, Bitmap bitmap, int color) {
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

        Canvas canvas = new Canvas(bitmap);

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


//        Paint black_paint = new Paint();
//        paint_fill.setColor(Color.BLACK);
//        paint_fill.setStrokeWidth(1);
//        paint_fill.setStyle(Paint.Style.FILL);
//
//        cross_size += 20;
//        center_space = 0;

//        canvas.drawLine(pos_x - cross_size, pos_y, pos_x - center_space, pos_y, black_paint);
//        canvas.drawLine(pos_x + center_space, pos_y, pos_x + cross_size, pos_y, black_paint);
//        canvas.drawLine(pos_x, pos_y - cross_size, pos_x, pos_y - center_space, black_paint);
//        canvas.drawLine(pos_x, pos_y + center_space, pos_x, pos_y + cross_size, black_paint);
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

        finish();
    }

    public void finishRegister(View item) {
//        OPENCVNATIVECALL
        selected_points_coordinates += "ALES %%%%%%%%%%%%%%%%%%%%%%%%\n";

        OpenCVNative.navigation_init();
        Intent intent = new Intent();
        intent.putExtra("NAVIGATION_CAN_START", true);
        setResult(RESULT_OK, intent);

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

        Bitmap first_image_bitmap = ((BitmapDrawable) firstImageView.getDrawable()).getBitmap();
        draw_point(points[0], points[1], first_image_bitmap, Color.RED);
        focusOnCross(points[0], points[1]);
    }

    public void nextPointButton(View view) {
//    OPENCVNATIVECALL
        selected_points_coordinates += "OpenCVNative.registration_next_point();\n";
        float[] points = OpenCVNative.registration_next_point();
//        float[] points = {50, 50}; // smazat
        Log.d(TAG, "Draw point to : " + String.valueOf(points[0]) + " x " + String.valueOf(points[1]));

        Bitmap first_image_bitmap = ((BitmapDrawable) firstImageView.getDrawable()).getBitmap();
        draw_point(points[0], points[1], first_image_bitmap, Color.RED);
        focusOnCross(points[0], points[1]);
    }

    public void resetPosition(View view) {
        refImageView.setTranslationX(0);
        refImageView.setTranslationY(0);
        refImageView.setScaleX(1.0f);
        refImageView.setScaleY(1.0f);
        firstImageView.setTranslationX(0);
        firstImageView.setTranslationY(0);
        firstImageView.setScaleX(1.0f);
        firstImageView.setScaleY(1.0f);
    }
}
