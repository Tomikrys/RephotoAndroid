package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/


import static android.provider.MediaStore.MediaColumns.ORIENTATION;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.vyw.rephotoandroid.model.GalleryItem;
import com.vyw.rephotoandroid.model.ListGalleryItems;

import java.util.ArrayList;
import java.util.List;


//Remember to implement  GalleryStripAdapter.GalleryStripCallBacks to fragment  for communication of fragment and GalleryStripAdapter
public class GallerySlideShowFragment extends DialogFragment implements GalleryStripAdapter.GalleryStripCallBacks {
    //declare static variable which will serve as key of current position argument
    private static final String ARG_CURRENT_POSITION = "position";
    private static final String GALLERY_ITEMS = "activity";
    //Declare list of GalleryItems
    List<GalleryItem> galleryItems;
    //Deceleration of  Gallery Strip Adapter
    GalleryStripAdapter mGalleryStripAdapter;
    // //Deceleration of  Slide show View Pager Adapter
    GallerySlideShowPagerAdapter mSlideShowPagerAdapter;
    //Deceleration of viewPager
    ViewPager mViewPagerGallery;
    TextView textViewImageName;
    TextView textViewImageTitle;
    RecyclerView recyclerViewGalleryStrip;
    Context context;
    int orientation;

    private int mCurrentPosition;
    //set bottom to visible of first load
    boolean isBottomBarVisible = true;

    public GallerySlideShowPagerAdapter getGallerySlideShowPagerAdapter() {
        return mSlideShowPagerAdapter;
    }

    public GallerySlideShowFragment() {
        // Required empty public constructor
    }

    //This method will create new instance of GallerySlideShowFragment
    public static GallerySlideShowFragment newInstance(int position, GalleryMainActivity galleryMainActivity, int orientation) {
        GallerySlideShowFragment fragment = new GallerySlideShowFragment();
        //Create bundle
        Bundle args = new Bundle();
        //put Current Position in the bundle
        args.putInt(ARG_CURRENT_POSITION, position);
        ListGalleryItems listGalleryItems = new ListGalleryItems(galleryMainActivity.galleryItems);
        args.putParcelable(GALLERY_ITEMS, (Parcelable) listGalleryItems);
        args.putInt(ORIENTATION, orientation);
        //set arguments of GallerySlideShowFragment
        fragment.setArguments(args);
        //return fragment instance
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialise GalleryItems List
        galleryItems = new ArrayList<>();
        if (getArguments() != null) {
            //get Current selected position from arguments
            mCurrentPosition = getArguments().getInt(ARG_CURRENT_POSITION);
            orientation = getArguments().getInt(ORIENTATION);
            //get GalleryItems from activity
            galleryItems = ((ListGalleryItems) getArguments().getParcelable(GALLERY_ITEMS)).getListGalleryItem();
            if (galleryItems != null) {
                GalleryItem galleryPhotos = galleryItems.get(mCurrentPosition);

                //Initialise View Pager Adapter
                mSlideShowPagerAdapter = new GallerySlideShowPagerAdapter(getContext(), galleryPhotos.placePhotos);
                int firstIndex = 0;
                ((GalleryMainActivity) getActivity()).setSelectedPicture(mSlideShowPagerAdapter.getPicture(firstIndex), firstIndex);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.gallery_fragment_slide_show, container, false);
        mViewPagerGallery = view.findViewById(R.id.viewPagerGallery);
        // set On Touch Listener on mViewPagerGallery to hide show bottom bar
        mViewPagerGallery.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (isBottomBarVisible) {
                        //bottom bar is visible make it invisible
                        FadeOutBottomBar();
                    } else {
                        //bottom bar is invisible make it visible
                        FadeInBottomBar();
                    }
                }
                return false;
            }

        });
        textViewImageName = view.findViewById(R.id.textViewImageName);
        textViewImageTitle = view.findViewById(R.id.textViewImageTitle);
        //set adapter to Viewpager
        mViewPagerGallery.setAdapter(mSlideShowPagerAdapter);
        recyclerViewGalleryStrip = view.findViewById(R.id.recyclerViewGalleryStrip);
        //Create GalleryStripRecyclerView's Layout manager
        final RecyclerView.LayoutManager mGalleryStripLayoutManger = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        //set layout manager of GalleryStripRecyclerView
        recyclerViewGalleryStrip.setLayoutManager(mGalleryStripLayoutManger);
        //Create GalleryStripRecyclerView's Adapter
        if (galleryItems != null) {
            mGalleryStripAdapter = new GalleryStripAdapter(galleryItems, getContext(), this, mCurrentPosition);
            //set Adapter of GalleryStripRecyclerView
            recyclerViewGalleryStrip.setAdapter(mGalleryStripAdapter);
            //tell viewpager to open currently selected item and pass position of current item
            mViewPagerGallery.setCurrentItem(mCurrentPosition);
            //set image name textview's text according to position
            textViewImageName.setText(galleryItems.get(mCurrentPosition).year);
            textViewImageTitle.setText(galleryItems.get(mCurrentPosition).imageName);
            //Add OnPageChangeListener to viewpager to handle page changes
            mViewPagerGallery.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    // mCurrentPosition is place id
                    // position is photo id
                    //set image name textview's text on any page selected
                    textViewImageName.setText(galleryItems.get(mCurrentPosition).getPlacePhoto(position).year);
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    //first check When Page is scrolled and gets stable
                    if (state == ViewPager.SCROLL_STATE_IDLE) {
                        //get current  item on view pager
                        int currentSelected = mViewPagerGallery.getCurrentItem();
                        //scroll strip smoothly to current  position of viewpager
                        mGalleryStripLayoutManger.smoothScrollToPosition(recyclerViewGalleryStrip, null, currentSelected);
                        //select current item of viewpager on gallery strip at bottom
                        mGalleryStripAdapter.setSelected(currentSelected);
                    }
                }
            });
        }
        return view;
    }

    //Overridden method by GalleryStripAdapter.GalleryStripCallBacks for communication on gallery strip item selected
    @Override
    public void onGalleryStripItemSelected(int position) {
        //set current item of viewpager
        mViewPagerGallery.setCurrentItem(position);
        ((GalleryMainActivity) getActivity()).setSelectedPicture(mSlideShowPagerAdapter.getPicture(position), position);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //remove selection on destroy
        if (mGalleryStripAdapter != null) {
            mGalleryStripAdapter.removeSelection();
        }
    }

    //Method to fadeIn bottom bar which is image textview name
    public void FadeInBottomBar() {
        //define alpha animation
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        //set duration
        fadeIn.setDuration(100);
        //set animation listener
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //set textview visible on animation ends
                textViewImageName.setVisibility(View.VISIBLE);
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textViewImageTitle.setVisibility(View.VISIBLE);
//                    recyclerViewGalleryStrip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        //start animation
        textViewImageName.startAnimation(fadeIn);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textViewImageTitle.startAnimation(fadeIn);
//            recyclerViewGalleryStrip.startAnimation(fadeIn);
        }
        isBottomBarVisible = true;
    }

    public void FadeOutBottomBar() {
        //define alpha animation
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        //set duration
        fadeOut.setDuration(100);
        //set animation listener
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //set textview Visibility gone on animation ends
                textViewImageName.setVisibility(View.GONE);

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textViewImageTitle.setVisibility(View.GONE);
//                    recyclerViewGalleryStrip.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        //start animation
        textViewImageName.startAnimation(fadeOut);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textViewImageTitle.startAnimation(fadeOut);
//            recyclerViewGalleryStrip.startAnimation(fadeOut);
        }
        isBottomBarVisible = false;
    }
}
