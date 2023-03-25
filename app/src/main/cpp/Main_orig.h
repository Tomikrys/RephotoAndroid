/**
 * Main.h
 * Author: Adam Červenka <xcerve16@stud.fit.vutbr.cz>
 *
 */

#ifndef REPHOTOGRAFING_MAIN_ORIG_H
#define REPHOTOGRAFING_MAIN_ORIG_H

#include <iostream>
#include <pthread.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/video/tracking.hpp>
#include <iomanip>
#include "opencv2/features2d.hpp"
//todo add
//#include "opencv2/xfeatures2d.hpp"

#include "PnPProblem.h"
#include "CameraCalibrator.h"
#include "Utils.h"
#include "Line.h"
#include "MSAC.h"
#include "RobustMatcher.h"


#ifdef WIN32

#include <windows.h>
//#include <opencv/cv.hpp>

#endif
#ifdef linux

#include <stdio.h>
#include <thread>

#endif

#define USE_PPHT
#define MAX_NUM_LINES    200

int getDirectory(double x, double y);
void print_matrix(const char *label, cv::Mat mat);

bool end_registration = false;
int const number_registration = 15;
int index_of_registration = 0;

// Color
cv::Scalar red(0, 0, 255);
cv::Scalar green(0, 255, 0);
cv::Scalar blue(255, 0, 0);

ModelRegistration registration;
CameraCalibrator cameraCalibrator;
RobustMatcher robustMatcher;
PnPProblem pnp_registration;
PnPProblem pnp_detection;
cv::KalmanFilter kalmanFilter;
MSAC msac;


// Robust cv::Matcher parameters
double confidenceLevel = 0.999;
float ratioTest = 0.70f;
double min_dist = 2;

// SIFT parameters
int numKeyPoints = 1000;

// MSAC parameters
int mode = MODE_NIETO;
int numVps = 3;
bool verbose = false;


// ERROR message
const std::string ERROR_READ_IMAGE = "Could not open or find the image";
//const std::string ERROR_WRITE_IMAGE = "Could not open the camera device";
const std::string ERROR_OPEN_CAMERA = "Could not open the camera device";


// RANSAC parameters
bool useExtrinsicGuess = false;
int iterationsCount = 10000;
float reprojectionError = 3.0;
double confidence = 0.999;
int pnp_method = cv::SOLVEPNP_EPNP;

// Kalman Filter parameters
int minInliersKalman = 10;
int nStates = 18;
int nMeasurements = 6;
int nInputs = 0;
double dt = 0.125;


std::vector<cv::Mat> current_frames;
int start;
int end;

std::thread robust_thread;
cv::Mat robust_last_current_frame;
int last_robust_id;

std::vector<cv::Point3f> list_3D_points_after_triangulation;
std::vector<cv::Point2f> list_2D_points_after_triangulation;
std::vector<cv::Point2f> detection_points_first_image, detection_points_second_image;
std::vector<int> key_points_first_image_convert_table_to_3d_points;
cv::Mat first_image;
cv::Mat second_image;
cv::Mat ref_image;
std::vector<int> index_points;

struct robust_matcher_struct {
    cv::Mat current_frame;
    std::vector<cv::Point3f> list_3D_points;
    cv::Mat measurements;
    int direction;
    int count_frames;
    std::atomic<bool> robust_done;
    cv:: Mat position_relative_T;
    std::vector<cv::Point3f> list_3D_points_for_lightweight;
    std::vector<cv::Point2f> list_2D_points_for_lightweight;
};

void fill_robust_matcher_arg_struct(cv::Mat current_frame_vis, int count_frames);

struct fast_robust_matcher_struct {
    cv::Mat last_current_frame;
    cv::Mat current_frame;
    std::vector<cv::Point3f> list_3D_points_from_robust;
    std::vector<cv::Point2f> list_2D_points_from_robust;
    int direction;
    int count_frames;
    int robust_id;
    cv:: Mat position_relative_T;
};

std::vector<cv::Point3f> g_list_3D_points_from_robust;
std::vector<cv::Point2f> g_list_2D_points_from_robust;
cv:: Mat g_position_relative_T;
std::vector<cv::Point3f> history_of_position_robust;
std::vector<cv::Point3f> history_of_position_fast;

void fill_fast_robust_matcher_arg_struct(cv::Mat current_frame_vis, int direction, int count_frames,
                                         std::vector<cv::Point3f> list_3D_points,
                                         std::vector<cv::Point2f> list_2D_points);

robust_matcher_struct robust_matcher_arg_struct;
fast_robust_matcher_struct fast_robust_matcher_arg_struct;

void *robust_matcher(void *arg);

void *fast_robust_matcher(void *arg);

static void onMouseModelRegistration(int event, int x, int y, int, void *);

std::vector<cv::Mat> processImage(MSAC &msac, int numVps, cv::Mat &imgGRAY, cv::Mat &outputImg);

bool getRobustEstimation(cv::Mat current_frame_vis, std::vector<cv::Point3f> list_3D_points,
                         cv::Mat measurements, int &directory, cv::Mat &position_relative_T,
                         std::vector<cv::Point3f> &list_3D_points_for_lightweight,
                         std::vector<cv::Point2f> &list_2D_points_for_lightweight);

bool
getLightweightEstimation(cv::Mat last_current_frame_vis,
                         std::vector<cv::Point3f> list_3D_points_of_last_current_frame,
                         std::vector<cv::Point2f> list_2D_points_of_last_current_frame,
                         cv::Mat current_frame_vis, int &directory, cv::Mat &position_relative_T);

void initKalmanFilter(cv::KalmanFilter &KF, int nStates, int nMeasurements, int nInputs, double dt);

void updateKalmanFilter(cv::KalmanFilter &KF, cv::Mat &measurements, cv::Mat &translation_estimated,
                        cv::Mat &rotation_estimated);

void fillMeasurements(cv::Mat &measurements, const cv::Mat &translation_measured,
                      const cv::Mat &rotation_measured);

inline pthread_t fast_robust_matcher_t, robust_matcher_t;

cv::Mat loadImage(const std::string path_to_file);

#endif
