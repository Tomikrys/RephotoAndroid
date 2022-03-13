package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

/**
 * Created by amardeep on 11/3/2017.
 */

public class GallerySlideShowPagerAdapter extends PagerAdapter {

    Context mContext;
    //Layout inflater
    LayoutInflater mLayoutInflater;
    //list of Gallery Items
    List<GalleryItem> galleryItems;

    public GallerySlideShowPagerAdapter(Context context, List<GalleryItem> galleryItems) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //set galleryItems
        this.galleryItems = galleryItems;
    }

    @Override
    public int getCount() {
        return galleryItems.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((ImageView) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.gallery_pager_item, container, false);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.imageViewThumbnail);
        //load current image in viewpager
        Picasso.with(mContext).load(new File(galleryItems.get(position).imageUri)).fit().into(imageView);
        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ImageView) object);
    }

}