package com.vyw.rephotoandroid.testCpp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.vyw.rephotoandroid.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class CameraTest extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    //    private TextView textView;
    private static String TAG = "CameraPreview";
    TextView text;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_test);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        text = (TextView) findViewById(R.id.textView5);
        if (!hasCameraPermission()) {
            requestPermission();
        }
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
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

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setTargetResolution(new Size(1280, 960))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                byte[] bytes = new byte[imageProxy.getPlanes()[0].getBuffer().remaining()];
                imageProxy.getPlanes()[0].getBuffer().get(bytes);

                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                Bitmap bitmap = RGBA8888BitesToBitmap(bytes, imageProxy.getWidth(), imageProxy.getHeight(), rotationDegrees);

                if (bitmap != null) {
                    Log.i(TAG, "Run analysis");
                    get_dimensions(bitmap);
                } else {
                    Log.d(TAG, "imageBitmap is null, cannot run analysis");
                }
                imageProxy.close();
            }
        });
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
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

    public void getProperities(View view) {
        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            String focal_lengths = "Focal lengths:";
            for (float number : getFocalLengths(cameraProvider)) {
                focal_lengths += (" " + number);
            }
            text.setText(focal_lengths);
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
}
