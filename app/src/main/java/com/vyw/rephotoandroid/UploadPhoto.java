package com.vyw.rephotoandroid;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.vyw.rephotoandroid.model.Configuration;
import com.vyw.rephotoandroid.model.api.Status;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadPhoto extends AppCompatActivity {
    private boolean isOCVSetUp = false;
    private ImageView refImage;
    private ImageView newImage;
    private static Bitmap refImageBitmap;
    private static Bitmap refImageBitmapCopy;
    private ImageView cameraPreview = null;
    String path_ref_image = "";
    String path_new_image = "";
    Bitmap bt_ref_frame = null;
    Bitmap bt_new_frame = null;
    private static String TAG = "UploadPhoto";
    private float origX = 0;
    private float origY = 0;

    private float newAlpha = 1;
    private float origAlpha = 1;
    private float newCrop = 0;
    private float origCrop = 0;
    private String source = "";
//    private SimpleNavigation simpleNavigationIntent = null;


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
        setContentView(R.layout.upload_photo);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        path_ref_image = getIntent().getStringExtra("PATH_REF_IMAGE");
        path_new_image = getIntent().getStringExtra("PATH_NEW_IMAGE");
        source = getIntent().getStringExtra("SOURCE");
        if (source.equals("ONLINE")) {
            ((FloatingActionButton) findViewById(R.id.retake)).setVisibility(View.GONE);
            ((FloatingActionButton) findViewById(R.id.save_photo)).setVisibility(View.GONE);
            ((FloatingActionButton) findViewById(R.id.upload_photo)).setVisibility(View.VISIBLE);
        } else {
            ((FloatingActionButton) findViewById(R.id.retake)).setVisibility(View.VISIBLE);
            ((FloatingActionButton) findViewById(R.id.save_photo)).setVisibility(View.VISIBLE);
            ((FloatingActionButton) findViewById(R.id.upload_photo)).setVisibility(View.GONE);
        }
//        simpleNavigationIntent = (SimpleNavigation) getIntent().getParcelableExtra("SimpleNavigation");

//      get online ref photo
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

//      get local new photo
        Uri uri_ref_image = Uri.parse(path_new_image);
        try {
            bt_new_frame = ImageFunctions.getBitmapFromUri(uri_ref_image, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bt_new_frame = ImageFunctions.rotateImage(bt_new_frame, ImageFunctions.getOrientation(uri_ref_image, this));

        newImage = findViewById(R.id.newImage);
        newImage.setImageBitmap(bt_new_frame);


        if (!isOCVSetUp) { // if OCV hasn't been setup yet, init it
            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mOpenCVCallBack);
                Log.i(TAG, "Cannot load OpenCV");
            } else {
                isOCVSetUp = true;
                mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    public String getPath(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            ;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    public void UploadPhoto(View view) {
        Toast.makeText(getApplicationContext(),
                "Begin Upload", Toast.LENGTH_LONG).show();
        Uri file_uri = Uri.parse(path_new_image);
        String absolute_path = getPath(file_uri);
        File file = new File(absolute_path);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part multipartBodyPart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
//        TODO get id
        Call<Status> call = apiInterface.addPhoto(Configuration.getAccessToken(this), 6, multipartBodyPart);

        UploadPhoto copyThis = this;
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                if (response.code() == 200) {
                    Toast.makeText(
                            copyThis,
                            "Uploaded",
                            Toast.LENGTH_SHORT
                    ).show();
                    Intent intent = new Intent();
                    intent.putExtra("CLOSE", true);
                    setResult(Activity.RESULT_OK, intent);
                    copyThis.finish();
//                    simpleNavigationIntent.finish();
                } else {
                    Log.e(TAG, "onResponse: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
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

    public void SavePhoto(View view) {
        Intent intent = new Intent();
        intent.putExtra("CLOSE", true);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void Retake(View view) {
//        remove file
        File file = new File(path_new_image);
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
        finish();
    }
}