package com.vyw.rephotoandroid;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.vyw.rephotoandroid.model.Configuration;
import com.vyw.rephotoandroid.model.api.LoginResponse;
import com.vyw.rephotoandroid.model.api.OneLoginResponse;
import com.vyw.rephotoandroid.model.api.Status;
import com.vyw.rephotoandroid.model.api.UserLogin;
import com.vyw.rephotoandroid.model.api.UserLogout;

import net.steamcrafted.materialiconlib.MaterialMenuInflater;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private static final int REQUEST_CHOOSER = 1234;
    int id_entered_button;
    String path_ref_image = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_ref_choose);

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

        String access_token = Configuration.getAccessToken(this);
        if (access_token == null) {
            setDrawerNotLogged();
        } else {
            testAccessTokenAndSetDrawer(access_token);
        }
    }

    public void checkLogin(View view) {
        testAccessTokenAndSetDrawer(Configuration.getAccessToken(this));
    }

    private void testAccessTokenAndSetDrawer(String access_token) {
        UserLogout accesToken = new UserLogout(access_token);
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<OneLoginResponse> call = apiInterface.checkLogin(accesToken);
        SimpleMainActivity copyThis = this;
        call.enqueue(new Callback<OneLoginResponse>() {
            @Override
            public void onResponse(Call<OneLoginResponse> call, Response<OneLoginResponse> response) {
                if (response.code() == 200) {
                    assert response.body() != null;
                    LoginResponse status = response.body().getLoginResponse();

                    SharedPreferences sharedPref = copyThis.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.access_token), status.getAccess_token());
                    editor.apply();

                    setDrawerLogged();
                } else {
                    setDrawerNotLogged();
                    setVariablesNotLogged(copyThis);
                }
            }

            @Override
            public void onFailure(Call<OneLoginResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MaterialMenuInflater
                .with(this) // Provide the activity context
                // Set the fall-back color for all the icons. Colors set inside the XML will always have higher priority
                .setDefaultColor(Color.RED)
                // Inflate the menu
                .inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri uri = data.getData();
            String path = uri.toString();

            path_ref_image = path;
            Intent intent = new Intent(this, SimpleNavigation.class);
            intent.putExtra("PATH_REF_IMAGE", path_ref_image);
            intent.putExtra("SOURCE", "LOCAL");
            startActivity(intent);

            Log.d(TAG, path);
            Log.d(TAG, String.valueOf(id_entered_button));
        }
    }

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
        Call<Status> call = apiInterface.addPhoto(Configuration.getAccessToken(this), 6, multipartBodyPart);

        SimpleMainActivity copyThis = this;
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
//                LoginResponse status = response.body().getLoginResponse();
                if (response.code() == 200) {
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
        return super.onOptionsItemSelected(item);
    }

    public void Logout(View view) {
        Logout();
    }

    public void Logout() {
        UserLogout userLogout = new UserLogout(Configuration.getAccessToken(this));
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<Status> call = apiInterface.logout(userLogout);
        SimpleMainActivity copyThis = this;
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                if (response.code() == 200) {
                    Toast.makeText(
                            copyThis,
                            "logged out",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {
                    Log.e(TAG, "onResponse: " + response.code());
                }
                setVariablesNotLogged(copyThis);
                setDrawerNotLogged();
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    public void setVariablesNotLogged(AppCompatActivity view) {
        SharedPreferences sharedPref = view.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.access_token), null);
        editor.putString(getString(R.string.last_name), null);
        editor.putString(getString(R.string.first_name), null);
        editor.putString(getString(R.string.email), null);
        editor.apply();
    }

    private void setDrawerNotLogged() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.drawer_navigation);
        if (navigationView != null) {
            Menu navigation_menu = navigationView.getMenu();
            navigation_menu.findItem(R.id.logout).setVisible(false);
            navigation_menu.findItem(R.id.login).setVisible(true);
            TextView header_title = navigationView.getHeaderView(0).findViewById(R.id.header_title);
            if (header_title != null) {
                header_title.setText("Not logged in");
            }
        }
    }

    private void setDrawerLogged() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.drawer_navigation);
        if (navigationView != null) {
            Menu navigation_menu = navigationView.getMenu();
            navigation_menu.findItem(R.id.logout).setVisible(true);
            navigation_menu.findItem(R.id.login).setVisible(false);
            TextView header_title = navigationView.getHeaderView(0).findViewById(R.id.header_title);
            if (header_title != null) {
                header_title.setText("Logged in as " + Configuration.getFirstName(this) + " " + Configuration.getLastName(this));
            }
        }
    }

    public void Logout(MenuItem item) {
        Logout();
    }

    public void Login(MenuItem item) {
        Login();
    }

    public void Login(View view) {
        Login();
    }


    public void choosePhoto(View view) {
//        ActivityCompat.finishAffinity(this);
        showFileDialog(view);
//        finish();
    }

    public void choosePhoto(MenuItem menu) {
//        ActivityCompat.finishAffinity(this);
        showFileDialog(null);
//        finish();
    }

    public void Login() {
        final Dialog dialog = new Dialog(SimpleMainActivity.this);

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


        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                Login(email, password);
                dialog.dismiss();
            }
        });
        if (dialog != null) {
            dialog.getWindow().setLayout((int) (ViewGroup.LayoutParams.MATCH_PARENT), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

    }

    public void Login(String email, String password) {
        UserLogin userLogin = new UserLogin(email, password);
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<OneLoginResponse> call = apiInterface.login(userLogin);
        SimpleMainActivity copyThis = this;
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

                    Toast.makeText(
                            copyThis,
                            "Logged in as " + Configuration.getFirstName(copyThis) + " " + Configuration.getLastName(copyThis),
                            Toast.LENGTH_SHORT
                    ).show();

                    setDrawerLogged();

//                    TODO update menu
                } else {
                    Log.e(TAG, "onResponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<OneLoginResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailure: " + call.toString());
                Log.e(TAG, "onFailure: " + t.getMessage());
            }
        });
    }

    public void openSettings(MenuItem item) {
        // opening a new intent to open settings activity.
        Intent i = new Intent(SimpleMainActivity.this, PreferenceActivity.class);
        startActivity(i);
    }
}

