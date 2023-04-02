package com.vyw.rephotoandroid.smartNavigation;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.vyw.rephotoandroid.GalleryScreenUtils;
import com.vyw.rephotoandroid.ImageFunctions;
import com.vyw.rephotoandroid.OpenCVNative;
import com.vyw.rephotoandroid.R;
import com.vyw.rephotoandroid.UploadPhoto;
import com.vyw.rephotoandroid.testCpp.RegisterPoints;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class SmartNavigation extends AppCompatActivity implements Parcelable {
    private Executor executor = Executors.newSingleThreadExecutor();
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private boolean isOCVSetUp = false;
    private PreviewView previewView;
    private ImageView refImage;
    private static Bitmap refImageBitmap;
    private static Bitmap refImageBitmapCopy;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageView cameraPreview = null;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private String path_ref_image = "";
    private Bitmap bt_ref_frame = null;
    private static String TAG = "SimpleNavigation";
    private float origX = 0;
    private float origY = 0;
    Bitmap refImageForAnalysis;

    private float newAlpha = 1;
    private float origAlpha = 1;
    private float newCrop = 0;
    private float origCrop = 0;
    private ImageCapture imageCapture;
    private String source = "";
    private String image_id;

    private int max_width_for_analysis = 1280;
    private int max_height_for_analysis = 960;

    private int widthForAnalysis = 1280;
    private int heightForAnalysis = 960;

    private Boolean navigation_started = false;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.smart_navigation);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (!isOCVSetUp) { // if OCV hasn't been setup yet, init it
            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
                Log.i(TAG, "Cannot load OpenCV");
            } else {
                isOCVSetUp = true;
                mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }

        previewView = findViewById(R.id.previewView);
        Intent intent = getIntent();
        path_ref_image = intent.getStringExtra("PATH_REF_IMAGE");
        image_id = intent.getStringExtra("IMAGE_ID");
        source = intent.getStringExtra("SOURCE");
        if (source.equals("ONLINE")) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    bt_ref_frame = bitmap;
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            };

            Picasso.get().load(path_ref_image).into(target);

        } else {
            Uri uri_ref_image = Uri.parse(path_ref_image);
            try {
                bt_ref_frame = ImageFunctions.getBitmapFromUri(uri_ref_image, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bt_ref_frame = ImageFunctions.rotateImage(bt_ref_frame, ImageFunctions.getOrientation(uri_ref_image, this));
        }
        refImage = findViewById(R.id.refImage);
        refImage.setImageBitmap(bt_ref_frame);

        refImageForAnalysis = ImageFunctions.scaleImage(bt_ref_frame, max_width_for_analysis, max_height_for_analysis);
        refImageBitmap = ImageFunctions.deepCopyBitmap(bt_ref_frame);
        refImageBitmapCopy = ImageFunctions.deepCopyBitmap(bt_ref_frame);

        toggleEdges(refImage);

        widthForAnalysis = refImageForAnalysis.getWidth();
        heightForAnalysis = refImageForAnalysis.getHeight();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        if (!hasCameraPermission()) {
            requestPermission();
        }
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreviewAndAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));


        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        ImageCapture.Builder builder = new ImageCapture.Builder();

        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        Rational aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                        .setTargetResolution(new Size(widthForAnalysis, heightForAnalysis))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (navigation_started) {
                    byte[] bytes = new byte[imageProxy.getPlanes()[0].getBuffer().remaining()];
                    imageProxy.getPlanes()[0].getBuffer().get(bytes);
                    int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                    Bitmap bitmap = RGBA8888BitesToBitmap(bytes, imageProxy.getWidth(), imageProxy.getHeight(), rotationDegrees);

                    if (bitmap != null) {
                        Bitmap scaledBitmap = ImageFunctions.cropAndScaleImage(bitmap, widthForAnalysis, heightForAnalysis);
                        Log.i(TAG, "Run analysis");
                        get_dimensions(scaledBitmap);
                        process_navigation(scaledBitmap);
                    } else {
                        Log.d(TAG, "imageBitmap is null, cannot run analysis");
                    }
                }
                imageProxy.close();
            }
        });

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture, imageAnalysis);
    }

    public int countFrames = 1;

    public void process_navigation(Bitmap bitmap) {
//      add countFrames to bitmap
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawBitmap(bitmap, 100, 200, null);
//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setTextSize(70);

        // draw the text on the canvas
//        canvas.drawText(String.valueOf(countFrames), 10, 10, paint);

        Log.d(TAG, "count frames: " + countFrames);
        Mat mat_current_frame = new Mat();
        Utils.bitmapToMat(bitmap, mat_current_frame);

        int direction;
        Mat position;
        double x = 0;
        double y = 0;
        double z = 0;
        try {
//            OPENCVNATIVECALL
            long position_addr = OpenCVNative.process_navigation(mat_current_frame.getNativeObjAddr(), countFrames);
            position = new Mat(position_addr);
            x = position.get(0, 3)[0];
            y = position.get(1, 3)[0];
            z = position.get(2, 3)[0];
            direction = getDirection(x, y);
            Log.d(TAG, "Value of direction is: " + direction + " x=" + x + " y=" + y);
        } catch (java.lang.IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
            direction = -1;
        }
        countFrames += 1;

        //1 up, 2 down, 3 right, 4 left;
        View arrow = findViewById(R.id.navigation_arrow);
        TextView distance_xy = findViewById(R.id.distance_xy);
        TextView distance_z = findViewById(R.id.distance_z);
        arrow.setVisibility(View.VISIBLE);

        double angle = Math.atan2(y, x) * 180 / Math.PI; // angle in degrees
        angle += 0;
        if (angle < 0) {
            angle += 360; // ensure angle is in range [0, 359]
        } else if (angle >= 360) {
            angle -= 360; // ensure angle is in range [0, 359]
        }
        arrow.setRotation(Math.round(angle));
        distance_xy.setText(String.format("%.2f", Math.sqrt(x * x + y * y)));
        distance_z.setText(String.format("%.2f", z));

        if (x == 0 && y == 0) {
            arrow.setVisibility(View.INVISIBLE);
        }


        View z_arrow = findViewById(R.id.z_arrow);
        z_arrow.setVisibility(View.VISIBLE);
        if (z > 0) {
            z_arrow.setRotation(-90);
        } else if (z < 0) {
            z_arrow.setRotation(90);
        } else {
            z_arrow.setVisibility(View.INVISIBLE);
        }
//        if (direction < 10) {
//            switch (direction) {
//                case -1:
//                    arrow.setRotation(15);
//                    break;
//                case 0:
//                    // optimal position
//                    arrow.setVisibility(View.INVISIBLE);
//                    break;
//                case 1:
//                    // up
//                    arrow.setRotation(90);
//                    break;
//                case 2:
//                    // down
//                    arrow.setRotation(270);
//                    break;
//                case 3:
//                    // right
//                    arrow.setRotation(180);
//                    break;
//                case 4:
//                    // left
//                    arrow.setRotation(0);
//                    break;
//            }
//        } else {
//            switch (direction) {
//                case 13:
//                    // up right
//                    arrow.setRotation(135);
//                    break;
//                case 14:
//                    // up left
//                    arrow.setRotation(45);
//                    break;
//                case 23:
//                    // down right
//                    arrow.setRotation(220);
//                    break;
//                case 24:
//                    // down left
//                    arrow.setRotation(315);
//                    break;
//            }
//        }
    }

    public int getDirection(double x, double y) {
        int direction = 0;
        if (x == 0) {
            if (y > 0) {
                direction = 1;
            } else if (y < 0) {
                direction = 2;
            }
        } else if (y == 0) {
            if (x > 0) {
                direction = 3;
            } else if (x < 0) {
                direction = 4;
            }
        } else if (x < 0 && y < 0) {
            direction = 14;
        } else if (x > 0 && y < 0) {
            direction = 13;
        } else if (x < 0 && y > 0) {
            direction = 24;
        } else if (x > 0 && y > 0) {
            direction = 23;
        }
        return direction;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        final int action = motionEvent.getActionMasked();

        float x = motionEvent.getX();
        float y = motionEvent.getY();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float sensitivity = (float) 0.4;
        int screenSmallerSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        float referenceSize = screenSmallerSize * sensitivity;

        switch (action) {
            case (MotionEvent.ACTION_DOWN):
                Log.d(TAG, "Action was DOWN");
                origX = x;
                origY = y;
                origAlpha = newAlpha;
                origCrop = newCrop;
                return true;
            case (MotionEvent.ACTION_MOVE):
                float xDiff = (origX - x);
                float yDiff = (origY - y);
                Log.d(TAG, "Action was MOVE " + xDiff + " " + yDiff);
                newAlpha = origAlpha + (yDiff / referenceSize);
                newAlpha = newAlpha > 1 ? 1 : newAlpha < 0 ? 0 : newAlpha;
                Log.d(TAG, "newAlpha " + newAlpha);

                newCrop = origCrop + (xDiff / referenceSize);
                newCrop = newCrop > 1 ? 1 : newCrop < 0 ? 0 : newCrop;
                Log.d(TAG, "newCrop " + newCrop);

                refImage.setImageBitmap(ImageFunctions.cropAndSetTransparency(newCrop, newAlpha, refImageBitmap));
                return true;
            case (MotionEvent.ACTION_UP):
                Log.d(TAG, "Action was UP");
                origAlpha = newAlpha;
                origCrop = newCrop;
                return true;
            case (MotionEvent.ACTION_CANCEL):
                Log.d(TAG, "Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE):
                Log.d(TAG, "Movement occurred outside bounds " +
                        "of current screen element");
                return true;
            default:
                return super.onTouchEvent(motionEvent);
        }
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

    boolean isEdges = false;

    public void toggleEdges(View view) {
        Log.d(TAG, "toggle");
        if (isEdges) {
            bt_ref_frame = ImageFunctions.deepCopyBitmap(refImageBitmapCopy);
            refImageBitmap = ImageFunctions.deepCopyBitmap(refImageBitmapCopy);
        } else {
            Bitmap edges = ImageFunctions.RefImageEdges(refImageBitmapCopy);
            bt_ref_frame = edges;
            refImageBitmap = edges;
        }
        refImage.setImageBitmap(ImageFunctions.cropAndSetTransparency(newCrop, newAlpha, refImageBitmap));
        isEdges = !isEdges;
    }


    public void takeFirstPhoto(View view) {
        Log.d(TAG, "first photo capture");

        String displayName = "Rephoto_" + System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();
        SmartNavigation thisCopy = this;
        Rational aspect_ratio;
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
            imageCapture.setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation());
        } else {
            aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
        }
        imageCapture.setCropAspectRatio(aspect_ratio);
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Image Saved successfully", Toast.LENGTH_LONG).show();
                        String savedImage = outputFileResults.getSavedUri().toString();

                        thisCopy.loadImageFoTriangulation(savedImage, true);
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }


    public void takeSecondPhoto(View view) {
        Log.d(TAG, "second photo capture");

        String displayName = "Rephoto_" + System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();
        SmartNavigation thisCopy = this;
        Rational aspect_ratio;
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
            imageCapture.setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation());
        } else {
            aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
        }
        imageCapture.setCropAspectRatio(aspect_ratio);
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Image Saved successfully", Toast.LENGTH_LONG).show();
                        String savedImage = outputFileResults.getSavedUri().toString();

                        thisCopy.loadImageFoTriangulation(savedImage, false);
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    Bitmap bt_first_image = null;
    Bitmap bt_second_image = null;

    public void loadImageFoTriangulation(String path_image, Boolean isFirst) {
        Uri uri_new_image = Uri.parse(path_image);
        Bitmap bt_image = null;
        try {
            bt_image = ImageFunctions.getBitmapFromUri(uri_new_image, this);
            bt_image = ImageFunctions.rotateImage(bt_image, ImageFunctions.getOrientation(uri_new_image, this));
            bt_image = ImageFunctions.scaleImage(bt_image, max_width_for_analysis, max_height_for_analysis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert bt_image != null;

        File file = new File(path_image);
        file.delete();
        if (file.exists()) {
            try {
                file.getCanonicalFile().delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file.exists()) {
                getApplicationContext().deleteFile(file.getName());
            }
        }

        if (isFirst) {
            bt_first_image = ImageFunctions.deepCopyBitmap(bt_image);
            findViewById(R.id.take_first_image).setVisibility(View.INVISIBLE);
            findViewById(R.id.take_second_image).setVisibility(View.VISIBLE);
        } else {
            bt_second_image = ImageFunctions.deepCopyBitmap(bt_image);

            Mat mat_first_image = new Mat();
            Mat mat_second_image = new Mat();
            Mat mat_ref_image = new Mat();
            Utils.bitmapToMat(bt_first_image, mat_first_image);
            Utils.bitmapToMat(bt_second_image, mat_second_image);
            Utils.bitmapToMat(refImageForAnalysis, mat_ref_image);

            int automatic_registration = OpenCVNative.triangulation(mat_first_image.getNativeObjAddr(), mat_second_image.getNativeObjAddr(), mat_ref_image.getNativeObjAddr());
//            TODO REISTRATION DEVELOPMENT UNCOMMENT IMIDIETLY OMG!!!!
//            if (automatic_registration != 1) {
            if (true) {
                Intent intent = new Intent(this, RegisterPoints.class);

                File first_image_file = new File(this.getFilesDir(), "first_image.jpg");
                FileOutputStream first_image_outputStream = null;
                try {
                    first_image_outputStream = new FileOutputStream(first_image_file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                bt_first_image.compress(Bitmap.CompressFormat.PNG, 100, first_image_outputStream);
                try {
                    first_image_outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long mat_first_address = mat_first_image.getNativeObjAddr();
                intent.putExtra("first_image", first_image_file.getAbsolutePath());


                File ref_image_file = new File(this.getFilesDir(), "ref_image.jpg");
                FileOutputStream ref_image_outputStream = null;
                try {
                    ref_image_outputStream = new FileOutputStream(ref_image_file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                refImageForAnalysis.compress(Bitmap.CompressFormat.PNG, 100, ref_image_outputStream);
                try {
                    ref_image_outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                long mat_ref_address = mat_ref_image.getNativeObjAddr();
                intent.putExtra("ref_image", ref_image_file.getAbsolutePath());
//                startActivity(intent);
                UploadPhotoActivityResultLauncher.launch(intent);
            } else {
                OpenCVNative.navigation_init();
                findViewById(R.id.take_second_image).setVisibility(View.INVISIBLE);
                findViewById(R.id.take_rephoto).setVisibility(View.VISIBLE);

                navigation_started = true;
            }
        }
    }

    public void capturePhoto(View view) {
        Log.d(TAG, "capture");

        String displayName = "Rephoto_" + System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();
        SmartNavigation thisCopy = this;
        Rational aspect_ratio;
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
            imageCapture.setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation());
        } else {
            aspect_ratio = new Rational(GalleryScreenUtils.getScreenWidth(this), GalleryScreenUtils.getScreenHeight(this));
        }
        imageCapture.setCropAspectRatio(aspect_ratio);
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Image Saved successfully", Toast.LENGTH_LONG).show();
                        String savedImage = outputFileResults.getSavedUri().toString();
                        Intent intent = new Intent(thisCopy, UploadPhoto.class);
                        intent.putExtra("PATH_REF_IMAGE", path_ref_image);
                        intent.putExtra("IMAGE_ID", image_id);
                        intent.putExtra("PATH_NEW_IMAGE", savedImage);
                        intent.putExtra("SOURCE", source);
                        intent.putExtra("DISPLAY_NAME", displayName);

//                        intent.putExtra("SimpleNavigation", (Parcelable) thisCopy);
                        UploadPhotoActivityResultLauncher.launch(intent);
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> UploadPhotoActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("CLOSE", false)) {
                            Intent intent = new Intent();
                            intent.putExtra("IMAGE_ID", image_id);
                            intent.putExtra("REFRESH", true);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } else if (data != null && data.getBooleanExtra("NAVIGATION_CAN_START", false)) {
                            OpenCVNative.navigation_init();
                            findViewById(R.id.take_second_image).setVisibility(View.INVISIBLE);
                            findViewById(R.id.take_rephoto).setVisibility(View.VISIBLE);

                            navigation_started = true;
                        }
                    }
                }


            });


    public static final Creator<SmartNavigation> CREATOR = new Creator<SmartNavigation>() {
        @Override
        public SmartNavigation createFromParcel(Parcel in) {
            return new SmartNavigation(in);
        }

        @Override
        public SmartNavigation[] newArray(int size) {
            return new SmartNavigation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public SmartNavigation() {
    }


    protected SmartNavigation(Parcel in) {
        isOCVSetUp = in.readByte() != 0;
        path_ref_image = in.readString();
        bt_ref_frame = in.readParcelable(Bitmap.class.getClassLoader());
        origX = in.readFloat();
        origY = in.readFloat();
        newAlpha = in.readFloat();
        origAlpha = in.readFloat();
        newCrop = in.readFloat();
        origCrop = in.readFloat();
        isEdges = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isOCVSetUp ? 1 : 0));
        dest.writeString(path_ref_image);
        dest.writeParcelable(bt_ref_frame, flags);
        dest.writeFloat(origX);
        dest.writeFloat(origY);
        dest.writeFloat(newAlpha);
        dest.writeFloat(origAlpha);
        dest.writeFloat(newCrop);
        dest.writeFloat(origCrop);
        dest.writeByte((byte) (isEdges ? 1 : 0));
    }


    public void get_dimensions(Bitmap bitmap) {
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            Integer width = bitmap.getWidth();
            Integer height = bitmap.getHeight();
            Float focal_length = getFocalLengths(cameraProvider)[0];
// F(mm) = F(pixels) * SensorWidth(mm) / ImageWidth (pixel)
// https://answers.opencv.org/question/17076/conversion-focal-distance-from-mm-to-pixels/
            Integer focal_f_x = Math.round(width * focal_length);
            Integer focal_f_y = Math.round(width * focal_length);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @androidx.annotation.OptIn(markerClass = ExperimentalCamera2Interop.class)
    float[] getFocalLengths(ProcessCameraProvider cameraProvider) {
        List<CameraInfo> filteredCameraInfos = CameraSelector.DEFAULT_BACK_CAMERA
                .filter(cameraProvider.getAvailableCameraInfos());
        if (!filteredCameraInfos.isEmpty()) {
            return Camera2CameraInfo.from(filteredCameraInfos.get(0)).getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        }
        return null;
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
}