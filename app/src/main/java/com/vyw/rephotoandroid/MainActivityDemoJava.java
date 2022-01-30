package com.vyw.rephotoandroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Size;
import android.view.OrientationEventListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.concurrent.ExecutionException;


// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5

public class MainActivityDemoJava extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    //    private TextView textView;
    private TextView brightness;
    private ImageView cameraPreview = null;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_demo_java);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//        textView = findViewById(R.id.orientation);
        brightness = findViewById(R.id.brightness);
        cameraPreview = findViewById(R.id.cameraPreview);
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

        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }


    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                byte[] bytes = new byte[imageProxy.getPlanes()[0].getBuffer().remaining()];
                imageProxy.getPlanes()[0].getBuffer().get(bytes);
                int total = 0;
                for (byte value : bytes) {
                    total += value & 0xFF;
                }
                if (bytes.length != 0) {
                    final int luminance = total / bytes.length;
                    brightness.setText(Integer.toString(luminance));
                    // luminance is the value you need.
                }

                int rotation = imageProxy.getImageInfo().getRotationDegrees();
                Bitmap bitmap = RGBA8888BitesToBitmap(bytes, imageProxy.getWidth(), imageProxy.getHeight(), rotation);

                cameraPreview.setImageBitmap(bitmap);

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
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
    }

    private Bitmap RGBA8888BitesToBitmap(byte[] bytes, int width, int height, int rotation) {
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
        matrix.postRotate(rotation);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }
}