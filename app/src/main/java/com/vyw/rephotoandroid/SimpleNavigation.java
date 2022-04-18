package com.vyw.rephotoandroid;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
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
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class SimpleNavigation extends AppCompatActivity implements Parcelable {
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

    private float newAlpha = 1;
    private float origAlpha = 1;
    private float newCrop = 0;
    private float origCrop = 0;
    private ImageCapture imageCapture;
    private String source = "";
    private String image_id;

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
        setContentView(R.layout.simple_navigation);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
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

                    refImage = findViewById(R.id.refImage);
                    refImage.setImageBitmap(bt_ref_frame);

                    refImageBitmapCopy = ImageFunctions.deepCopyBitmap(bt_ref_frame);
                    refImageBitmap = ImageFunctions.deepCopyBitmap(refImageBitmapCopy);
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

            refImage = findViewById(R.id.refImage);
            refImage.setImageBitmap(bt_ref_frame);

            refImageBitmapCopy = ImageFunctions.deepCopyBitmap(bt_ref_frame);
            refImageBitmap = ImageFunctions.deepCopyBitmap(refImageBitmapCopy);
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

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        if (!hasCameraPermission()) {
            requestPermission();
        }
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        ImageCapture.Builder builder = new ImageCapture.Builder();

        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
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
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
//        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);
//
//        // Query if extension is available (optional).
//        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
//            // Enable the extension if available.
//            hdrImageCaptureExtender.enableExtension(cameraSelector);
//        }

        imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
//        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview, imageCapture);
    }

//z

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
        SimpleNavigation thisCopy = this;
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
                        }
                    }
                }
            });

    public static final Creator<SimpleNavigation> CREATOR = new Creator<SimpleNavigation>() {
        @Override
        public SimpleNavigation createFromParcel(Parcel in) {
            return new SimpleNavigation(in);
        }

        @Override
        public SimpleNavigation[] newArray(int size) {
            return new SimpleNavigation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public SimpleNavigation() {
    }


    protected SimpleNavigation(Parcel in) {
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
}