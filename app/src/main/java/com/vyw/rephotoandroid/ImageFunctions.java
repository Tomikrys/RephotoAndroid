package com.vyw.rephotoandroid;

import static org.opencv.core.Core.meanStdDev;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageFunctions {

    public static Bitmap cropAndSetTransparency(double newCrop, double newAlpha, Bitmap refImageBitmap) {
        assert (0 <= newAlpha) && (newAlpha <= 1);
        assert (0 <= newCrop) && (newCrop <= 1);

        int width = refImageBitmap.getWidth();
        int height = refImageBitmap.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];
        Bitmap tmpBitmap = deepCopyBitmap(refImageBitmap);
        tmpBitmap.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),
                myBitmap.getHeight());

        int transparency = (int) Math.round(newAlpha * 255);
        int numberOfPixels = (int) Math.round(newCrop * width);

        for (int i = 0; i < myBitmap.getHeight() * myBitmap.getWidth(); i++) {
            boolean shouldBeCropped = width - numberOfPixels <= (i % myBitmap.getWidth());
            boolean isAlreadyTransparent = allpixels[i] == Color.alpha(Color.TRANSPARENT);
            if (shouldBeCropped) {
                allpixels[i] = Color.alpha(Color.TRANSPARENT);
            } else if (!isAlreadyTransparent) {
                allpixels[i] = setAplha(allpixels[i], transparency);
            }
        }

        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(),
                myBitmap.getHeight());
        return myBitmap;
    }

    public static Bitmap rotateImage(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }


    public static Bitmap makeBlackTransparent(Bitmap image) {
        // convert image to matrix
        Mat src = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(image, src);

        // init new matrices
        Mat dst = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Mat tmp = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Mat alpha = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);

        // convert image to grayscale
        Imgproc.cvtColor(src, tmp, Imgproc.COLOR_BGR2GRAY);

        // threshold the image to create alpha channel with complete transparency in black background region and zero transparency in foreground object region.
        Imgproc.threshold(tmp, alpha, 100, 255, Imgproc.THRESH_BINARY);

        // split the original image into three single channel.
        List<Mat> rgb = new ArrayList<Mat>(3);
        Core.split(src, rgb);

        // Create the final result by merging three single channel and alpha(BGRA order)
        List<Mat> rgba = new ArrayList<Mat>(4);
        rgba.add(rgb.get(0));
        rgba.add(rgb.get(1));
        rgba.add(rgb.get(2));
        rgba.add(alpha);
        Core.merge(rgba, dst);

        // convert matrix to output bitmap
        Bitmap output = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, output);
        return output;
    }


    public static Bitmap invertImage(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];
        image.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < myBitmap.getHeight() * myBitmap.getWidth(); i++) {
            allpixels[i] = (0xFFFFFF - (allpixels[i] & 0x00ffffff)) + (allpixels[i] & 0xff000000);
        }

        myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
        return myBitmap;
    }


    public static Bitmap RefImageEdges(Bitmap origImage) {
        Bitmap image = deepCopyBitmap(origImage);

        Mat rgbMat = new Mat();
        Utils.bitmapToMat(image, rgbMat);

        // apply gaussian blur
        Imgproc.GaussianBlur(rgbMat, rgbMat, new Size(3, 3), 0, 0);

        Mat grayMat = new Mat();
        Mat bwMat = new Mat();

        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(grayMat, grayMat);


        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(grayMat, mean, std);
        double[] means = mean.get(0, 0);
        double[] stds = std.get(0, 0);

//        Imgproc.adaptiveThreshold(grayMat, bwMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
//        Imgproc.Canny(grayMat, bwMat, 50, 200, 3, false);
        Imgproc.Canny(grayMat, bwMat, means[0] - stds[0], means[0] + stds[0], 3, false);

        Bitmap edges = image;
        Utils.matToBitmap(bwMat, edges);

        Bitmap transparentBlack = makeBlackTransparent(edges);
        return invertImage(transparentBlack);
    }


    public static int getOrientation(Uri photoUri, AppCompatActivity context) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(photoUri);
            ExifInterface exif = new ExifInterface(inputStream);
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 0;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            Log.e("WallOfLightApp", e.getMessage());
            return 0;
        }
    }


    public static Bitmap getBitmapFromUri(Uri uri, AppCompatActivity context) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public static Boolean cropAndTrasparencyOnTouch(MotionEvent motionEvent,
                                                    ImageView refImage,
                                                    Bitmap refImageBitmap,
                                                    float origX,
                                                    float origY,
                                                    float origAlpha,
                                                    float origCrop,
                                                    float newAlpha,
                                                    float newCrop,
                                                    String TAG,
                                                    AppCompatActivity context) {
        final int action = motionEvent.getActionMasked();

        float x = motionEvent.getX();
        float y = motionEvent.getY();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
                return false;
        }
    }

    public static Bitmap cropToAspectRatio(Bitmap srcBmp, int refWidth, int refHeight) {
        float refAspectRatio = (float) refWidth / refHeight;
        int srcWidth = srcBmp.getWidth();
        int srcHeight = srcBmp.getHeight();
        float srcAspectRatio = (float) srcWidth / srcHeight;
        float refCoefRatio = (float) Math.min(refWidth, refHeight) / Math.max(refWidth, refHeight);
        if ((srcAspectRatio > 0 && refAspectRatio > 0 && srcAspectRatio < refAspectRatio) ||
            (srcAspectRatio > 0 && refAspectRatio < 0 && srcAspectRatio > refAspectRatio) ||
            (srcAspectRatio < 0 && refAspectRatio > 0 && srcAspectRatio < refAspectRatio) ||
            (srcAspectRatio < 0 && refAspectRatio < 0 && srcAspectRatio > refAspectRatio)
        ) {
//            crop height
            int newHeight = Math.round(srcWidth * refCoefRatio);
            return Bitmap.createBitmap(
                    srcBmp,
                    0,
                    Math.round((srcHeight - newHeight) / 2),
                    srcWidth,
                    newHeight
            );

        } else if ((srcAspectRatio > 0 && refAspectRatio > 0 && srcAspectRatio > refAspectRatio) ||
                   (srcAspectRatio > 0 && refAspectRatio < 0 && srcAspectRatio < refAspectRatio) ||
                   (srcAspectRatio < 0 && refAspectRatio > 0 && srcAspectRatio > refAspectRatio) ||
                   (srcAspectRatio < 0 && refAspectRatio < 0 && srcAspectRatio < refAspectRatio)
        ) {
//            crop width
            int newWidth = Math.round(srcHeight * refAspectRatio);
            return Bitmap.createBitmap(
                    srcBmp,
                    Math.round((srcWidth - newWidth) / 2),
                    0,
                    newWidth,
                    srcHeight
            );
        }
        return srcBmp;
    }


    public static Bitmap deepCopyBitmap(Bitmap b) {
        return b.copy(b.getConfig(), true);
    }

    public static int setAplha(int pixel, int transparency) {
        return (pixel & 0x00ffffff) + (transparency << 24);
    }
}
