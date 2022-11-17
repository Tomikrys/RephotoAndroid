package com.vyw.rephotoandroid;


/**
 * Created by acervenka2 on 21.04.2017.
 */

public class OpenCVNative {

    static {
        System.loadLibrary("native-lib");
    }

    public static native void initReconstruction(long first_frame, long second_frame, long reference_frame, float[] arg);

    public static native float[] processReconstruction();

    public static native int fakemain(long bt_first_frame, long bt_second_frame, long bt_ref_frame);

    public static native float[] nextPoint();

    public static native float[] registrationPoints(double x, double y);

    public static native void initNavigation();

    public static native int processNavigation(long current_frame, int count_frames);
}
