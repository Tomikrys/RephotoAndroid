#include <jni.h>
#include <opencv2/core/core.hpp>

#ifndef _Included_com_delaroystudios_camera_OpenCVNative
#define _Included_com_delaroystudios_camera_OpenCVNative

extern "C" {

JNIEXPORT void JNICALL Java_com_delaroystudios_camera_OpenCVNative_initReconstruction
  (JNIEnv *, jclass, jlong, jlong, jlong, jfloatArray);

JNIEXPORT jfloatArray
JNICALL Java_com_delaroystudios_camera_OpenCVNative_processReconstruction
        (JNIEnv * , jclass);

JNIEXPORT jfloatArray JNICALL Java_com_delaroystudios_camera_OpenCVNative_nextPoint
  (JNIEnv *, jclass);

JNIEXPORT jfloatArray JNICALL Java_com_delaroystudios_camera_OpenCVNative_registrationPoints
  (JNIEnv *, jclass, jdouble, jdouble);

JNIEXPORT void JNICALL Java_com_delaroystudios_camera_OpenCVNative_initNavigation
  (JNIEnv *, jclass);


JNIEXPORT jint JNICALL Java_com_delaroystudios_camera_OpenCVNative_processNavigation
  (JNIEnv *, jclass, jlong, jint);

}
#endif
