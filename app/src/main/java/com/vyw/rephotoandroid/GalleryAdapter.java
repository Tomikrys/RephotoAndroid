package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.content.Context;

import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.vyw.rephotoandroid.model.GalleryItem;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by amardeep on 11/3/2017.
 */

public class GalleryAdapter extends RecyclerView.Adapter {
    //Declare GalleryItems List
    List<GalleryItem> galleryItems;
    Context context;
    //Declare GalleryAdapterCallBacks
    GalleryAdapterCallBacks mAdapterCallBacks;

    public GalleryAdapter(Context context) {
        this.context = context;
        //get GalleryAdapterCallBacks from contex
        this.mAdapterCallBacks = (GalleryAdapterCallBacks) context;
        //Initialize GalleryItem List
        this.galleryItems = new ArrayList<>();
    }

    //This method will take care of adding new Gallery items to RecyclerView
    public void addGalleryItems(List<GalleryItem> galleryItems) {
        if (galleryItems != null) {
            this.galleryItems.removeAll(this.galleryItems);
            this.galleryItems.addAll(galleryItems);
            notifyDataSetChanged();
        }
    }
    //This method will take care of adding new Gallery items to RecyclerView
    public void addOnlyNewGalleryItems(List<GalleryItem> galleryItems) {
        if (galleryItems != null) {
            int previousSize = this.galleryItems.size();
            this.galleryItems.removeAll(this.galleryItems);
            this.galleryItems.addAll(galleryItems);
            notifyItemRangeInserted(previousSize, galleryItems.size());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.gallery_custom_row_gallery_item, parent, false);
        return new GalleryItemHolder(row);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        //get current Gallery Item
        GalleryItem currentItem = galleryItems.get(position);
        //Create file to load with Picasso lib
//        File imageViewThoumb = new File(currentItem.imageUri);
        //cast holder with gallery holder
        GalleryItemHolder galleryItemHolder = (GalleryItemHolder) holder;
        //Load with Picasso
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Picasso.get()
                    .load(currentItem.imageUri)
                    .centerCrop()
                    .resize(GalleryScreenUtils.getScreenWidth(context) / 2, GalleryScreenUtils.getScreenWidth(context) / 2)//Resize image to width half of screen and height 1/3 of screen height
                    .into(galleryItemHolder.imageViewThumbnail);
        } else {
            Picasso.get()
                    .load(currentItem.imageUri)
                    .centerCrop()
                    .resize(GalleryScreenUtils.getScreenWidth(context) / 2, GalleryScreenUtils.getScreenHeight(context) / 3)//Resize image to width half of screen and height 1/3 of screen height
                    .into(galleryItemHolder.imageViewThumbnail);
        }

        //set name of Image
        galleryItemHolder.textViewImageName.setText(currentItem.imageName);
        galleryItemHolder.distance.setText(getDistanceString(currentItem.distance));
        //set on click listener on imageViewThumbnail
        galleryItemHolder.imageViewThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //call onItemSelected method and pass the position and let activity decide what to do when item selected
                mAdapterCallBacks.onItemSelected(position);
            }
        });

    }

    private String getDistanceString(Float distance) {
        if (distance > 10000) {
            return (distance.intValue() / 1000) + " km";
        }
        if (distance > 999) {
            DecimalFormat df = new DecimalFormat("#.##");
            return df.format(distance / 1000)  + " km";
        }
        return distance.intValue() + " m";
    }

    @Override
    public int getItemCount() {
        return galleryItems.size();
    }

    public class GalleryItemHolder extends RecyclerView.ViewHolder {
        ImageView imageViewThumbnail;
        TextView textViewImageName;
        TextView distance;

        public GalleryItemHolder(View itemView) {
            super(itemView);
            imageViewThumbnail = itemView.findViewById(R.id.imageViewThumbnail);
            textViewImageName = itemView.findViewById(R.id.textViewImageName);
            distance = itemView.findViewById(R.id.distance);
        }
    }

    //Interface for communication of Adapter and GalleryMainActivity
    public interface GalleryAdapterCallBacks {
        //call this method to notify about item is clicked
        void onItemSelected(int position);
    }


}