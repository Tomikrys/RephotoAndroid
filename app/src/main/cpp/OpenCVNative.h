#include <jni.h>
#include <opencv2/core/core.hpp>

#ifndef _Included_com_vyw_rephotoandroid_OpenCVNative
#define _Included_com_vyw_rephotoandroid_OpenCVNative

extern "C"
JNIEXPORT void JNICALL Java_com_vyw_rephotoandroid_OpenCVNative_initReconstruction
        (JNIEnv *, jclass, jlong, jlong, jlong, jfloatArray);

extern "C"
JNIEXPORT jfloatArray
JNICALL Java_com_vyw_rephotoandroid_OpenCVNative_processReconstruction
        (JNIEnv *, jclass);

extern "C"
JNIEXPORT jfloatArray JNICALL Java_com_vyw_rephotoandroid_OpenCVNative_nextPoint
        (JNIEnv *, jclass);

extern "C"
JNIEXPORT jfloatArray JNICALL Java_com_vyw_rephotoandroid_OpenCVNative_registrationPoints
        (JNIEnv *, jclass, jdouble, jdouble);

extern "C"
JNIEXPORT void JNICALL Java_com_vyw_rephotoandroid_OpenCVNative_initNavigation
        (JNIEnv *, jclass);

extern "C"
JNIEXPORT jint JNICALL Java_com_vyw_rephotoandroid_OpenCVNative_processNavigation
        (JNIEnv *, jclass, jlong, jint);
#endif
