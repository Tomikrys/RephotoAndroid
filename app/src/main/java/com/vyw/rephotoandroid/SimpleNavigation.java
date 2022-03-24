package com.vyw.rephotoandroid;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class SimpleNavigation extends AppCompatActivity {
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
    String path_ref_image = "";
    Bitmap bt_ref_frame = null;
    private static String TAG = "SimpleNavigation";
    private float origX = 0;
    private float origY = 0;

    private float newAlpha = 1;
    private float origAlpha = 1;
    private float newCrop = 0;
    private float origCrop = 0;
    private ImageCapture imageCapture;

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

    boolean onlyOnce = true;

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
        if (intent.getStringExtra("SOURCE").equals("ONLINE")) {
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    bt_ref_frame = bitmap;

                    refImage = findViewById(R.id.refImage);
                    refImage.setImageBitmap(bt_ref_frame);

                    refImageBitmapCopy = deepCopyBitmap(bt_ref_frame);
                    refImageBitmap = deepCopyBitmap(refImageBitmapCopy);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {}

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}
            };

            Picasso.get().load(path_ref_image).into(target);

        } else {
            Uri uri_ref_image = Uri.parse(path_ref_image);
            try {
                bt_ref_frame = getBitmapFromUri(uri_ref_image);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bt_ref_frame = rotateImage(bt_ref_frame, getOrientation(uri_ref_image));

            refImage = findViewById(R.id.refImage);
            refImage.setImageBitmap(bt_ref_frame);

            refImageBitmapCopy = deepCopyBitmap(bt_ref_frame);
            refImageBitmap = deepCopyBitmap(refImageBitmapCopy);
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
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

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

                refImage.setImageBitmap(cropAndSetTransparency(newCrop, newAlpha));
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

    private static Bitmap makeBlackTransparent(Bitmap image) {
        // convert image to matrix
        Mat src = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(image, src);

        // init new matrices
        Mat dst = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Mat tmp = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Mat alpha = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);

        // convert image to grayscale
        Imgproc.cvtColor(src, tmp, Imgproc.COLOR_BGR2GRAY);

        // threshold the image to create alpha channel with complete transparency in black background region and zero transparency in foreground object region.
        Imgproc.threshold(tmp, alpha, 100, 255, Imgproc.THRESH_BINARY);

        // split the original image into three single channel.
        List<Mat> rgb = new ArrayList<Mat>(3);
        Core.split(src, rgb);

        // Create the final result by merging three single channel and alpha(BGRA order)
        List<Mat> rgba = new ArrayList<Mat>(4);
        rgba.add(rgb.get(0));
        rgba.add(rgb.get(1));
        rgba.add(rgb.get(2));
        rgba.add(alpha);
        Core.merge(rgba, dst);

        // convert matrix to output bitmap
        Bitmap output = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, output);
        return output;
    }

    private Bitmap RefImageEdges(Bitmap origImage) {
        Bitmap image = deepCopyBitmap(origImage);

        Mat rgbMat = new Mat();
        Utils.bitmapToMat(image, rgbMat);

        Mat grayMat = new Mat();
        Mat bwMat = new Mat();

        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(grayMat, grayMat);

//        Imgproc.adaptiveThreshold(grayMat, grayMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
        Imgproc.Canny(grayMat, bwMat, 50, 200, 3, false);

        Bitmap edges = image;
        Utils.matToBitmap(bwMat, edges);

        Bitmap transparentBlack = makeBlackTransparent(edges);
        return invertImage(transparentBlack);
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


    public static Bitmap cropAndSetTransparency(double newCrop, double newAlpha) {
        assert (0 <= newAlpha) && (newAlpha <= 1);
        assert (0 <= newCrop) && (newCrop <= 1);

        int width = refImageBitmap.getWidth();
        int height = refImageBitmap.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];
        Bitmap tmpBitmap = deepCopyBitmap(refImageBitmap);
        tmpBitmap.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),
                myBitmap.getHeight());

        int transparency = (int) Math.round(newAlpha * 255);
        int numberOfPixels = (int) Math.round(newCrop * width);

        for (int i = 0; i < myBitmap.getHeight() * myBitmap.getWidth(); i++) {
            boolean shouldBeCropped = width - numberOfPixels <= (i % myBitmap.getWidth());
            boolean isAlreadyTransparent = allpixels[i] == Color.alpha(Color.TRANSPARENT);
            if (shouldBeCropped) {
                allpixels[i] = Color.alpha(Color.TRANSPARENT);
            } else if (!isAlreadyTransparent) {
                allpixels[i] = setAplha(allpixels[i], transparency);
            }
        }

        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),
                myBitmap.getHeight());
        return myBitmap;
    }

    public Bitmap invertImage(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];
        image.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < myBitmap.getHeight() * myBitmap.getWidth(); i++) {
            allpixels[i] = (0xFFFFFF - (allpixels[i] & 0x00ffffff)) + (allpixels[i] & 0xff000000);
        }

        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        return myBitmap;
    }

    public static int setAplha(int pixel, int transparency) {
        return (pixel & 0x00ffffff) + (transparency << 24);
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    boolean isEdges = false;

    public void toggleEdges(View view) {
        Log.d(TAG, "toggle");
        if (isEdges) {
            bt_ref_frame = deepCopyBitmap(refImageBitmapCopy);
            refImageBitmap = deepCopyBitmap(refImageBitmapCopy);
        } else {
            Bitmap edges = RefImageEdges(refImageBitmapCopy);
            bt_ref_frame = edges;
            refImageBitmap = edges;
        }
        refImage.setImageBitmap(cropAndSetTransparency(newCrop, newAlpha));
        isEdges = !isEdges;
    }

    public void capturePhoto(View view) {
        Log.d(TAG, "capture");

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Rephoto" + System.currentTimeMillis());
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues).build();
        imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                	outputFileResults.getSavedUri()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Image Saved successfully", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    public static Bitmap deepCopyBitmap(Bitmap b) {
        return b.copy(b.getConfig(), true);
    }

    public int getOrientation(Uri photoUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            ExifInterface exif = new ExifInterface(inputStream);
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 0;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            Log.e("WallOfLightApp", e.getMessage());
            return 0;
        }
    }

    public Bitmap rotateImage(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }
}