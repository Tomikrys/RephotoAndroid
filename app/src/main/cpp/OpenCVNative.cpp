#include <OpenCVNative.h>

#include "Main.h"

Main process = Main();

void JNICALL Java_com_delaroystudios_camera_OpenCVNative_initReconstruction
  (JNIEnv *env, jclass, jlong image1, jlong image2, jlong refImage, jfloatArray arr){

    cv::Mat& first_frame = *(cv::Mat*) image1;
    cv::Mat& second_frame = *(cv::Mat*) image2;
    cv::Mat& ref_image = *(cv::Mat*) refImage;

    jfloat *params = (*env).GetFloatArrayElements(arr, 0);

    cv::Point2f center_f = cv::Point2f(params[0], params[1]);
    cv::Point2f focal_f = cv::Point2f(params[2], params[3]);
    cv::Point2f center_c = cv::Point2f(params[4], params[5]);
    cv::Point2f focal_c = cv::Point2f(params[6], params[7]);

    process.initReconstruction(first_frame, second_frame, ref_image, center_f, focal_f, center_c, focal_c);
  }

jfloatArray JNICALL
Java_com_delaroystudios_camera_OpenCVNative_processReconstruction
(JNIEnv
*env, jclass){
jfloatArray params = (*env).NewFloatArray(2);

cv::Point2f point = process.processReconstruction();

jfloat position[2];
position[0] = point.
x;
position[1] = point.
y;

env->
SetFloatArrayRegion(params,
0, 2, position);

return
params;
  }

jfloatArray JNICALL
Java_com_delaroystudios_camera_OpenCVNative_nextPoint
  (JNIEnv *env, jclass){
jfloatArray params = (*env).NewFloatArray(2);

cv::Point2f point = process.nextPoint();

jfloat position[2];
position[0] = point.
x;
position[1] = point.
y;

     env->SetFloatArrayRegion(params, 0, 2, position);

     return params;
  }

jfloatArray JNICALL
Java_com_delaroystudios_camera_OpenCVNative_registrationPoints
  (JNIEnv *env, jclass, jdouble x, jdouble y){
     jfloatArray params =(*env).NewFloatArray(2);

     cv::Point2f point = process.registrationPoints(x, y);

     jfloat position[2];
     position[0] = point.x;
     position[1] = point.y;

     env->SetFloatArrayRegion(params, 0, 2, position);

     return params;
  }

void JNICALL
Java_com_delaroystudios_camera_OpenCVNative_initNavigation
  (JNIEnv *, jclass){
     process.initNavigation();
  }


jint JNICALL Java_com_delaroystudios_camera_OpenCVNative_processNavigation
  (JNIEnv *, jclass, jlong currentFrame, jint count_frames){
    cv::Mat& current_frame = *(cv::Mat*) currentFrame;
    return (jint) process.processNavigation(current_frame, count_frames);
  }