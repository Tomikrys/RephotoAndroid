package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vyw.rephotoandroid.model.GalleryItem;

import java.util.List;

//Remember to implement  GalleryAdapter.GalleryAdapterCallBacks to activity  for communication of Activity and Gallery Adapter
public class GalleryMainActivity extends AppCompatActivity implements GalleryAdapter.GalleryAdapterCallBacks {
    private static final String TAG = "GalleryMainActivity";
    //Deceleration of list of  GalleryItems
    public List<GalleryItem> galleryItems;
    //Read storage permission request code
    private static final int RC_READ_STORAGE = 5;
    GalleryAdapter mGalleryAdapter;
    private GalleryItem selectedPicture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity_main);
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
                        }
                        mGalleryAdapter.addGalleryItems(galleryItems);
                    }
                });
            }
        }).start();
    }

    public void takeRephoto(View view) {
        Log.d(TAG, "takeRephoto");
        Intent intent = new Intent(this, SimpleNavigation.class);
        intent.putExtra("PATH_REF_IMAGE", selectedPicture.imageUri);
        intent.putExtra("SOURCE", "ONLINE");
        startActivity(intent);
//        Toast.makeText(this, "Images cannot be fetched, check your internet connection.", Toast.LENGTH_LONG).show();
    }

    public void openNavigation(View view) {
        Log.d(TAG, "getDirection");
        Uri geoLocation = Uri.parse("geo:0,0?q=" + selectedPicture.place.getLatitude() + "," + selectedPicture.place.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        startActivity(intent);
    }


    @Override
    public void onItemSelected(int position) {
        //create fullscreen GallerySlideShowFragment dialog
        GallerySlideShowFragment slideShowFragment = GallerySlideShowFragment.newInstance(position);
        //setUp style for slide show fragment
        slideShowFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogFragmentTheme);
        //finally show dialogue
        slideShowFragment.show(getSupportFragmentManager(), null);
        if (slideShowFragment.getGallerySlideShowPagerAdapter() != null) {
            selectedPicture = slideShowFragment.getGallerySlideShowPagerAdapter().getPicture(position);
        } else {
//            will be set with setSelectedPictureUri from Create function in slideShowFragment
        }
    }

    public void setSelectedPicture(GalleryItem picture) { selectedPicture = picture; }

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
