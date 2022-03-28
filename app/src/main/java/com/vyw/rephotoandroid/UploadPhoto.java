package com.vyw.rephotoandroid;

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
    private SimpleNavigation simpleNavigationIntent = null;


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

    public String getPath(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){;
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
        Call<Status> call = apiInterface.addPhoto(Configuration.access_token, 6, multipartBodyPart);

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
                    copyThis.finish();
                    simpleNavigationIntent.finish();
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
        simpleNavigationIntent = (SimpleNavigation) getIntent().getParcelableExtra("SimpleNavigation");

//      get online ref photo
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

//      get local new photo
        Uri uri_ref_image = Uri.parse(path_new_image);
        try {
            bt_new_frame = getBitmapFromUri(uri_ref_image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bt_new_frame = rotateImage(bt_new_frame, getOrientation(uri_ref_image));

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

    public static Bitmap deepCopyBitmap(Bitmap b) {
        return b.copy(b.getConfig(), true);
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