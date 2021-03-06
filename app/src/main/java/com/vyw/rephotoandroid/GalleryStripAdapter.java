package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.vyw.rephotoandroid.layout.SquareLayout;
import com.vyw.rephotoandroid.model.GalleryItem;

import java.util.List;


public class GalleryStripAdapter extends RecyclerView.Adapter {
    private static final String TAG = "GalleryStripAdapter";
    //Declare list of GalleryItems
    List<GalleryItem> galleryItems;
    Context context;
    GalleryStripCallBacks mStripCallBacks;
    GalleryItem mCurrentSelected;

    public GalleryStripAdapter(List<GalleryItem> galleryItems, Context context, GalleryStripCallBacks StripCallBacks, int CurrentPosition) {
        //set galleryItems
        this.galleryItems = galleryItems.get(CurrentPosition).placePhotos;
        this.context = context;
        //set stripcallbacks
        this.mStripCallBacks = StripCallBacks;
        //set current selected
        mCurrentSelected = galleryItems.get(CurrentPosition);
        //set current selected item as selected
        mCurrentSelected.isSelected = true;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.gallery_custom_row_gallery_strip_item, parent, false);
        SquareLayout squareLayout = row.findViewById(R.id.squareLayout);
        return new GalleryStripItemHolder(squareLayout);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        //get Curent Gallery Item
        GalleryItem mCurrentItem = galleryItems.get(position);
        //get thumb square size 1/6 of screen width
        int orientation = context.getResources().getConfiguration().orientation;
        final int thumbSize = GalleryScreenUtils.getScreenWidth(context) / (orientation == Configuration.ORIENTATION_LANDSCAPE ? 12 : 6);
        //cast holder to galleryStripItemHolder
        GalleryStripItemHolder galleryStripItemHolder = (GalleryStripItemHolder) holder;
        //get thumb size bitmap by using ThumbnailUtils
//        TODO local
        Picasso.get()
            .load(mCurrentItem.imageUri)
            .centerCrop()
            .resize(thumbSize, thumbSize)//Resize image to width half of screen and height 1/3 of screen height
            .into(galleryStripItemHolder.imageViewThumbnail);
//        Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(mCurrentItem.imageUri),
//                thumbSize, thumbSize);
//        //set thumbnail
//        galleryStripItemHolder.imageViewThumbnail.setImageBitmap(ThumbImage);
        //set current selected
        if (mCurrentItem.isSelected) {
            galleryStripItemHolder.imageViewThumbnail.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            //value 0 removes any background color
            galleryStripItemHolder.imageViewThumbnail.setBackgroundColor(0);
        }
        galleryStripItemHolder.imageViewThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //call onGalleryStripItemSelected on click and pass position
                mStripCallBacks.onGalleryStripItemSelected(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return galleryItems.size();
    }

    public class GalleryStripItemHolder extends RecyclerView.ViewHolder {
        ImageView imageViewThumbnail;

        public GalleryStripItemHolder(View itemView) {
            super(itemView);
            imageViewThumbnail = itemView.findViewById(R.id.imageViewThumbnail);
        }
    }

    //interface for communication on gallery strip interactions
    public interface GalleryStripCallBacks {
        void onGalleryStripItemSelected(int position);
    }

    //Method to highlight  selected item on gallery strip
    public void setSelected(int position) {
        //remove current selection
        mCurrentSelected.isSelected = false;
        //notify recyclerview that we changed  item to update its view
        notifyItemChanged(galleryItems.indexOf(mCurrentSelected));
        //select gallery item
        galleryItems.get(position).isSelected = true;
        //notify recyclerview that we changed  item to update its view
        notifyItemChanged(position);
        //set current selected
        mCurrentSelected = galleryItems.get(position);

    }

    //method to remove selection
    public void removeSelection() {
        mCurrentSelected.isSelected = false;
    }


}
