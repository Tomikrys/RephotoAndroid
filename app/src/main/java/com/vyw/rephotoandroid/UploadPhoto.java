package com.vyw.rephotoandroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.internal.utils.ImageUtil;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.vyw.rephotoandroid.model.Configuration;
import com.vyw.rephotoandroid.model.api.LoginResponse;
import com.vyw.rephotoandroid.model.api.OneLoginResponse;
import com.vyw.rephotoandroid.model.api.Status;
import com.vyw.rephotoandroid.model.api.UserLogin;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
    Uri uri_new_image_cropped = null;
    Bitmap bt_ref_frame = null;
    Bitmap bt_new_frame = null;
    private static String TAG = "UploadPhoto";
    private float origX = 0;
    private float origY = 0;

    private float newAlpha = 0.5F;
    private float origAlpha = 0;
    private float newCrop = 0.5F;
    private float origCrop = 0;
    private String source = "";
    private String displayName = "";
    private String image_id;
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
        refImage = findViewById(R.id.refImage);

        Intent intent = getIntent();
        path_ref_image = intent.getStringExtra("PATH_REF_IMAGE");
        path_new_image = intent.getStringExtra("PATH_NEW_IMAGE");
        image_id = intent.getStringExtra("IMAGE_ID");
        source = intent.getStringExtra("SOURCE");
        displayName = intent.getStringExtra("DISPLAY_NAME");
        if (source.equals("ONLINE")) {
            ((FloatingActionButton) findViewById(R.id.retake)).setVisibility(View.VISIBLE);
            ((FloatingActionButton) findViewById(R.id.save_photo)).setVisibility(View.GONE);
            ((FloatingActionButton) findViewById(R.id.upload_photo)).setVisibility(View.VISIBLE);
        } else {
            ((FloatingActionButton) findViewById(R.id.retake)).setVisibility(View.VISIBLE);
            ((FloatingActionButton) findViewById(R.id.save_photo)).setVisibility(View.VISIBLE);
            ((FloatingActionButton) findViewById(R.id.upload_photo)).setVisibility(View.GONE);
        }
//        simpleNavigationIntent = (SimpleNavigation) getIntent().getParcelableExtra("SimpleNavigation");

        if (source.equals("ONLINE")) {
            //      get online ref photo
            Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    bt_ref_frame = bitmap;

                    refImage.setImageBitmap(ImageFunctions.cropAndSetTransparency(newCrop, newAlpha, bt_ref_frame));
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

//        Uri uri_ref_image = Uri.parse(path_ref_image);
//        int rotationDegrees = ImageFunctions.getOrientation(uri_ref_image, this);

            Picasso.get().load(path_ref_image).into(target);
        } else {
            Uri uri_ref_image = Uri.parse(path_ref_image);
            try {
                bt_ref_frame = ImageFunctions.getBitmapFromUri(uri_ref_image, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bt_ref_frame = ImageFunctions.rotateImage(bt_ref_frame, ImageFunctions.getOrientation(uri_ref_image, this));
            refImage.setImageBitmap(ImageFunctions.cropAndSetTransparency(newCrop, newAlpha, bt_ref_frame));


            refImageBitmapCopy = ImageFunctions.deepCopyBitmap(bt_ref_frame);
            refImageBitmap = ImageFunctions.deepCopyBitmap(refImageBitmapCopy);
        }

//      get local new photo
        Uri uri_new_image = Uri.parse(path_new_image);
        try {
            bt_new_frame = ImageFunctions.getBitmapFromUri(uri_new_image, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bt_new_frame = ImageFunctions.rotateImage(bt_new_frame, ImageFunctions.getOrientation(uri_new_image, this));
        bt_new_frame = ImageFunctions.cropToAspectRatio(bt_new_frame, bt_ref_frame.getWidth(), bt_ref_frame.getHeight());

        try {
            uri_new_image_cropped = saveBitmap(this, bt_new_frame, displayName + "_crop");
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    @NonNull
    public Uri saveBitmap(@NonNull final Context context, @NonNull final Bitmap bitmap,
                          @NonNull final String displayName) throws IOException {

        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        final ContentResolver resolver = context.getContentResolver();
        Uri uri = null;

        try {
            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, values);

            if (uri == null)
                throw new IOException("Failed to create new MediaStore record.");

            try (final OutputStream stream = resolver.openOutputStream(uri)) {
                if (stream == null)
                    throw new IOException("Failed to open output stream.");

                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream))
                    throw new IOException("Failed to save bitmap.");
            }

            return uri;
        }
        catch (IOException e) {

            if (uri != null) {
                // Don't leave an orphan entry in the MediaStore
                resolver.delete(uri, null, null);
            }

            throw e;
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
        UploadPhoto();
    }

    public String add_cropToUri(String uri) {
        int index = uri.lastIndexOf(".");
        String uri_crop =uri.substring(0,index) + "_crop" + uri.substring(index,uri.length());
        return uri_crop;
    }

    public void UploadPhoto() {
        if (Configuration.getAccessToken(this) == null || Configuration.getEmail(this) == null) {
            Login();
        }
        Toast.makeText(getApplicationContext(),
                "Begin Upload", Toast.LENGTH_LONG).show();
//        Uri file_uri = Uri.parse(path_new_image_cropped);
        String absolute_path = getPath(uri_new_image_cropped);
        File file = new File(absolute_path);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part multipartBodyPart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<Status> call = apiInterface.addPhoto(Configuration.getAccessToken(this), Integer.parseInt(image_id), multipartBodyPart);

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
                    intent.putExtra("IMAGE_ID", image_id);
                    setResult(Activity.RESULT_OK, intent);
                    copyThis.finish();
                } else if (response.code() == 401 || response.code() == 403) {
                    Login();
                } else {
                    Log.e(TAG, "onResponse: " + response.code());
                    Toast.makeText(
                            copyThis,
                            "Error while uploading, please try again.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(
                        copyThis,
                        "Error while uploading, please try again.",
                        Toast.LENGTH_SHORT
                ).show();
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


    public void Login() {
        final Dialog dialog = new Dialog(this);

        //We have added a title in the custom layout. So let's disable the default title.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //The user will be able to cancel the dialog bu clicking anywhere outside the dialog.
        dialog.setCancelable(true);
        //Mention the name of the layout of your custom dialog.
        dialog.setContentView(R.layout.login_dialog);

        //Initializing the views of the dialog.
        final EditText emailET = dialog.findViewById(R.id.email_et);
        final EditText passwordET = dialog.findViewById(R.id.password_et);
        Button submitButton = dialog.findViewById(R.id.submit_button);


        submitButton.setOnClickListener(v -> {
            String email = emailET.getText().toString();
            String password = passwordET.getText().toString();
            Login(email, password);
            dialog.dismiss();
        });
        dialog.getWindow().setLayout((int) (ViewGroup.LayoutParams.MATCH_PARENT), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

    }


    public void Login(String email, String password) {
        UserLogin userLogin = new UserLogin(email, password);
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<OneLoginResponse> call = apiInterface.login(userLogin);
        AppCompatActivity copyThis = this;
        call.enqueue(new Callback<OneLoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<OneLoginResponse> call, @NonNull Response<OneLoginResponse> response) {
                if (response.code() == 200) {
                    assert response.body() != null;
                    LoginResponse status = response.body().getLoginResponse();

                    SharedPreferences sharedPref = copyThis.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.access_token), status.getAccess_token());
                    editor.putString(getString(R.string.last_name), status.getLast_name());
                    editor.putString(getString(R.string.first_name), status.getFirst_name());
                    editor.putString(getString(R.string.email), status.getEmail());
                    editor.apply();

                    UploadPhoto();

                    Toast.makeText(
                            copyThis,
                            "Logged in as " + Configuration.getFirstName(copyThis) + " " + Configuration.getLastName(copyThis),
                            Toast.LENGTH_SHORT
                    ).show();
                } else if (response.code() == 401) {
                    Log.e(TAG, "onResponse: " + response.code());
                    Toast.makeText(
                            copyThis,
                            "Login failed, wrong email or password.",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Log.e(TAG, "onResponse: " + response.code());
                    Toast.makeText(
                            copyThis,
                            "Login failed, check your internet connection.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OneLoginResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: " + call);
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
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