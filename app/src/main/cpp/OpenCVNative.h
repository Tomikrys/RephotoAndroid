#ifndef _Included_com_vyw_rephotoandroid_OpenCVNative
#define _Included_com_vyw_rephotoandroid_OpenCVNative

#include <jni.h>
#include <opencv2/core/core.hpp>

extern "C" {
JNIEXPORT jfloatArray
Java_com_vyw_rephotoandroid_OpenCVNative_processReconstruction
        (JNIEnv *env, jclass);

JNIEXPORT jfloatArray
Java_com_vyw_rephotoandroid_OpenCVNative_nextPoint
        (JNIEnv *env, jclass);

JNIEXPORT jfloatArray
Java_com_vyw_rephotoandroid_OpenCVNative_registrationPoints
        (JNIEnv *env, jclass, jdouble x, jdouble y);

JNIEXPORT void JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_initNavigation
        (JNIEnv *, jclass);

JNIEXPORT jint JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_processNavigation
        (JNIEnv *, jclass, jlong currentFrame, jint count_frames);

JNIEXPORT void JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_initReconstruction
        (JNIEnv *env, jclass, jlong image1, jlong image2, jlong refImage, jfloatArray arr);
};
#endif
