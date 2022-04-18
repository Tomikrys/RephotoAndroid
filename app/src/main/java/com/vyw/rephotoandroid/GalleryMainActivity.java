package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.vyw.rephotoandroid.model.Configuration;
import com.vyw.rephotoandroid.model.GalleryItem;
import com.vyw.rephotoandroid.model.api.LoginResponse;
import com.vyw.rephotoandroid.model.api.OneLoginResponse;
import com.vyw.rephotoandroid.model.api.Status;
import com.vyw.rephotoandroid.model.api.UserLogin;
import com.vyw.rephotoandroid.model.api.UserLogout;

import net.steamcrafted.materialiconlib.MaterialMenuInflater;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//Remember to implement  GalleryAdapter.GalleryAdapterCallBacks to activity  for communication of Activity and Gallery Adapter
public class GalleryMainActivity extends AppCompatActivity implements GalleryAdapter.GalleryAdapterCallBacks {
    private static final String TAG = "GalleryMainActivity";
    public List<GalleryItem> galleryItems;
    //Read storage permission request code
    private static final int RC_READ_STORAGE = 5;
    GalleryAdapter mGalleryAdapter;
    private GalleryItem selectedPicture = null;
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private static final int REQUEST_CHOOSER = 1234;
    private int id_entered_button;
    private String path_ref_image = "";
    private ImageView loading_image_view = null;
    private Button choose_photo_button = null;
    private Bundle savedInstanceState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private GallerySlideShowFragment slideShowFragment = null;
    private int last_position = 0;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity_main);
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.my_drawer_layout);
        loading_image_view = findViewById(R.id.loading);
        choose_photo_button = findViewById(R.id.choose_photo_gallery);
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

        //setup RecyclerView
        RecyclerView recyclerViewGallery = (RecyclerView) findViewById(R.id.recyclerViewGallery);
        recyclerViewGallery.setLayoutManager(new GridLayoutManager(this, 2));
        //Create RecyclerView Adapter
        mGalleryAdapter = new GalleryAdapter(this);
        //set adapter to RecyclerView
        recyclerViewGallery.setAdapter(mGalleryAdapter);
        //check for read storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //Get images
            loadImagesFromAPIAsynchronously();
        } else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RC_READ_STORAGE);
        }

        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        initializeRefreshListener();
    }

    public void initializeRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                TODO refresh images
                loadImagesFromAPIAsynchronously();

                // This method gets called when user pull for refresh,
                // You can make your API call here,
                // We are using adding a delay for the moment
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                }, 3000);
            }
        });
    }

    public void Logout() {
        UserLogout userLogout = new UserLogout(Configuration.getAccessToken(this));
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<Status> call = apiInterface.logout(userLogout);
        AppCompatActivity copyThis = this;
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

    public void Login() {
        final Dialog dialog = new Dialog(GalleryMainActivity.this);

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

    private void testAccessTokenAndSetDrawer(String access_token) {
        UserLogout accesToken = new UserLogout(access_token);
        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
        Call<OneLoginResponse> call = apiInterface.checkLogin(accesToken);
        AppCompatActivity copyThis = this;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && data.getBooleanExtra("REFRESH", false)) {
            loadImagesFromAPIAsynchronously();
            onItemSelected(last_position);
        } else if (resultCode == RESULT_OK) {
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

//    public void UploadPhoto(View view) {
//        Toast.makeText(getApplicationContext(),
//                "Begin Upload", Toast.LENGTH_LONG).show();
////        File path = Environment.getExternalStoragePublicDirectory(
////                Environment.DIRECTORY_PICTURES);
////        File file = new File(path, "DemoPicture.jpg");
//        String path_new_image = "content://media/external/images/media/67";
//        Uri file_uri = Uri.parse(path_new_image);
//        String absolute_path = getPath(file_uri);
//        File file = new File(absolute_path);
//        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
//        MultipartBody.Part multipartBodyPart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
//
//        ApiInterface apiInterface = RetrofitClient.getRetrofitInstance().create(ApiInterface.class);
//        Call<Status> call = apiInterface.addPhoto(Configuration.getAccessToken(this), 6, multipartBodyPart);
//
//        AppCompatActivity copyThis = this;
//        call.enqueue(new Callback<Status>() {
//            @Override
//            public void onResponse(Call<Status> call, Response<Status> response) {
////                LoginResponse status = response.body().getLoginResponse();
//                if (response.code() == 200) {
//                    Toast.makeText(
//                            copyThis,
//                            "Uploaded",
//                            Toast.LENGTH_SHORT
//                    ).show();
//                } else {
//                    Log.e(TAG, "onResponse: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<Status> call, Throwable t) {
//                Log.e(TAG, "onFailure: " + call.toString());
//                Log.e(TAG, "onFailure: " + t.getMessage());
//            }
//        });
//    }

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

    public void Login(MenuItem item) {
        Login();
    }

    public void Logout(MenuItem item) {
        Logout();
    }

    public void exitApplication(MenuItem item) {
        finish();
    }

    public void choosePhoto(View view) {
        showFileDialog(view);
    }

    public void choosePhoto(MenuItem menu) {
        showFileDialog(null);
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

    public void openSettings(MenuItem item) {
        // opening a new intent to open settings activity.
        Intent i = new Intent(GalleryMainActivity.this, PreferenceActivity.class);
        startActivity(i);
    }

    public void openTermsAndConditions(MenuItem item) {
        // opening a new intent to open settings activity.
        Intent i = new Intent(GalleryMainActivity.this, TermsAndConditions.class);
        startActivity(i);
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


    private void loadImagesFromAPIAsynchronously() {
        // do
        GalleryMainActivity thisCopy = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Run async
//                TODO local
//                galleryItems = GalleryUtils.getLocalImages(thisCopy);
                galleryItems = GalleryUtils.getImagesFromAPI();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // do after
                        // add images to gallery recyclerview using adapter
                        if (galleryItems == null) {
                            Toast.makeText(thisCopy, "Images cannot be fetched, check your internet connection.", Toast.LENGTH_LONG).show();
                            connectionLost();
                        } else {
                            loading_image_view.setVisibility(View.INVISIBLE);
                        }
                        mGalleryAdapter.addOnlyNewGalleryItems(galleryItems);
                    }
                });
            }
        }).start();
    }


    // Network Check
    public void registerNetworkCallback() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            connectivityManager.registerDefaultNetworkCallback(
                    new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            connectionLost();
                        }

                        @Override
                        public void onLost(Network network) {
                            connectionGained();
                        }
                    }
            );
            connectionLost();
        } catch (Exception e) {
            connectionLost();
        }
    }

    public void connectionLost() {
        loading_image_view.setVisibility(View.VISIBLE);
        loading_image_view.setImageResource(R.drawable.baseline_sync_problem_black_24dp);
        choose_photo_button.setVisibility(View.VISIBLE);
    }

    public void connectionGained() {
        loading_image_view.setImageResource(R.drawable.baseline_cloud_sync_black_24dp);
        choose_photo_button.setVisibility(View.INVISIBLE);
    }

    public void takeRephoto(View view) {
        Log.d(TAG, "takeRephoto");
        Intent intent = new Intent(this, SimpleNavigation.class);
        intent.putExtra("PATH_REF_IMAGE", selectedPicture.imageUri);
        int id = selectedPicture.place.getId();
        intent.putExtra("IMAGE_ID", String.valueOf(id));
        intent.putExtra("SOURCE", "ONLINE");
        SimpleNavigationActivityResultLauncher.launch(intent);
//        startActivity(intent);
    }

    ActivityResultLauncher<Intent> SimpleNavigationActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        loadImagesFromAPIAsynchronously();
//                    }
                }
            });

    public void openNavigation(View view) {
        Log.d(TAG, "getDirection");
        Uri geoLocation = Uri.parse("geo:0,0?q=" + selectedPicture.place.getLatitude() + "," + selectedPicture.place.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        startActivity(intent);
    }

    @Override
    public void onItemSelected(int position) {
        int orientation = getResources().getConfiguration().orientation;
        if (slideShowFragment != null) {
            slideShowFragment.dismiss();
        }
        last_position = position;
        //create fullscreen GallerySlideShowFragment dialog
        slideShowFragment = GallerySlideShowFragment.newInstance(position, this, orientation);
        //setUp style for slide show fragment
        slideShowFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFragmentTheme);
        //finally show dialogue
        slideShowFragment.show(getSupportFragmentManager(), null);
        if (slideShowFragment.getGallerySlideShowPagerAdapter() != null) {
            selectedPicture = slideShowFragment.getGallerySlideShowPagerAdapter().getPicture(position);
        } else {
            // will be set with setSelectedPictureUri from Create function in slideShowFragment
        }
    }

    public void setSelectedPicture(GalleryItem picture, int index) {
        selectedPicture = picture;
        onItemSelected(index);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImagesFromAPIAsynchronously();
            } else {
                Toast.makeText(this, "Storage Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
