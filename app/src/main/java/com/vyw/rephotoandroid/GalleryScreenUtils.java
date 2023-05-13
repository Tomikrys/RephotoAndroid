package com.vyw.rephotoandroid;
// Code inpired by https://www.loopwiki.com/application/create-gallery-android-application/

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Insets;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

public class GalleryScreenUtils {

    //static method to get screen width
    public static int getScreenWidth(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics displayMetrics = ((Activity) context).getWindowManager().getCurrentWindowMetrics();
            WindowInsets windowInsets = displayMetrics.getWindowInsets();
            Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()
                    | WindowInsets.Type.displayCutout());
            int insetsWidth = insets.right + insets.left;
            int insetsHeight = insets.top + insets.bottom;

            // Legacy size that Display#getSize reports
            final Rect bounds = displayMetrics.getBounds();
            return bounds.width() - insetsWidth;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    //static method to get screen height
    public static int getScreenHeight(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics displayMetrics = ((Activity) context).getWindowManager().getCurrentWindowMetrics();
            WindowInsets windowInsets = displayMetrics.getWindowInsets();
            Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()
                    | WindowInsets.Type.displayCutout());
            int insetsWidth = insets.right + insets.left;
            int insetsHeight = insets.top + insets.bottom;

            // Legacy size that Display#getSize reports
            final Rect bounds = displayMetrics.getBounds();
            return bounds.height() - insetsHeight;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }
}
