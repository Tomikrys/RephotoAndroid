package com.vyw.rephotoandroid;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.vyw.rephotoandroid.model.Configuration;
import com.vyw.rephotoandroid.model.api.LoginResponse;
import com.vyw.rephotoandroid.model.api.OneLoginResponse;
import com.vyw.rephotoandroid.model.api.Status;
import com.vyw.rephotoandroid.model.api.UserLogin;
import com.vyw.rephotoandroid.model.api.UserLogout;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Callback;
import retrofit2.Call;
import retrofit2.Response;


public class SimpleMainActivity extends AppCompatActivity {

    private static String TAG = "SimpleMainActivity";

    static {
        System.loadLibrary("native-lib");
    }

    Dialog dialog;
    int simple_navigation_button;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    Menu menu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_ref_choose);
        simple_navigation_button = findViewById(R.id.simple_navigation).getId();



        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

//  TODO debug memory mode
//        if(BuildConfig.DEBUG)
//            StrictMode.enableDefaults();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    public void simpleNavigation(View view) {
//        ActivityCompat.finishAffinity(this);
        showFileDialog(view);
//        finish();
    }

    private static final int REQUEST_CHOOSER = 1234;
    int id_entered_button;
    String path_ref_image = "";

    public void showFileDialog(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "Choose file");

        startActivityForResult(intent, REQUEST_CHOOSER);
    }

    public void RephotoApiButton(View view) {
        Intent intent = new Intent(this, Rephotos_API.class);
        startActivity(intent);
    }

    public void GalleryButton(View view) {
        Intent intent = new Intent(this, GalleryMainActivity.class);
        startActivity(intent);
    }

    public void Login(View view) {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

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
//        File path = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES);
//        File file = new File(path, "DemoPicture.jpg");
        String path_new_image = "content://media/external/images/media/67";
        Uri file_uri = Uri.parse(path_new_image);
        String absolute_path = getPath(file_uri);
        File file = new File(absolute_path);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part multipartBodyPart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<Status> call = apiInterface.addPhoto(Configuration.access_token, 6, multipartBodyPart);

        SimpleMainActivity copyThis = this;
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
//                LoginResponse status = response.body().getLoginResponse();
                if (response.code() == 200) {
//                    Configuration.access_token = status.getAccess_token();
//                    Configuration.email = status.getEmail();
//                    Configuration.first_name = status.getFirst_name();
//                    Configuration.last_name = status.getLast_name();
//                    Configuration.logged = true;
                    Toast.makeText(
                            copyThis,
                            "Uploaded",
                            Toast.LENGTH_SHORT
                    ).show();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri uri = data.getData();
            String path = uri.toString();

            if (path != null) {
                path_ref_image = path;
                Intent intent = new Intent(this, SimpleNavigation.class);
                intent.putExtra("PATH_REF_IMAGE", path_ref_image);
                startActivity(intent);

            }
            Log.d(TAG, path);
            Log.d(TAG, String.valueOf(id_entered_button));
        }
    }

    public void exitApplication(MenuItem item) {
        finish();
    }


    // override the onOptionsItemSelected()
    // function to implement
    // the item click listener callback
    // to open and close the navigation
    // drawer when the icon is clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.login:
                if (Configuration.logged) {
                    Logout(null);
                } else {
                    Login(null);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void Logout(View view) {
        UserLogout userLogout = new UserLogout(Configuration.access_token);
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<Status> call = apiInterface.logout(userLogout);
        SimpleMainActivity copyThis = this;
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                if (response.code() == 200) {
                    Configuration.access_token = null;
                    Configuration.logged = false;
                    Configuration.last_name = null;
                    Configuration.first_name = null;
                    Configuration.email = null;
//                    TODO update menu
                    Toast.makeText(
                            copyThis,
                            "logged out",
                            Toast.LENGTH_SHORT
                    ).show();
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
}
