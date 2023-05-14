/**
 * Main.cpp
 * Author: Adam Červenka <xcerve16@stud.fit.vutbr.cz>
 *
 */

#include "Main.h"
#include <opencv2/features2d.hpp>
#include <opencv2/imgproc/types_c.h>
#include <random>
#include <thread>
#include <pthread.h>
#include <utility>
#include <opencv2/videoio.hpp>
#include <opencv2/core/cvstd.hpp>


#include <android/bitmap.h>
#include <android/log.h>
#include <dirent.h>

#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "MAIN", __VA_ARGS__);


std::vector<cv::Mat> frames;
std::vector<double> position;
int indexFrame = 0;

void saveMatToJpeg(cv::Mat img, std::string filename, bool RGBA2BGRA = false) {
    if (RGBA2BGRA) {
        cv::cvtColor(img, img, cv::COLOR_RGBA2BGRA);
    }
    cv::imwrite("/storage/emulated/0/Download/Rephoto/" + filename + ".jpg", img,
                {cv::ImwriteFlags::IMWRITE_JPEG_QUALITY, 70});
}

std::string generate_csv_from_Mat(cv::Mat list_3D_points) {
    std::string csv = "x,y,z,\n";
    for (int i = 0; i < list_3D_points.rows; i++) {
        csv += std::to_string(list_3D_points.at<float>(i, 0)) + ","
               + std::to_string(list_3D_points.at<float>(i, 1)) + ","
               + std::to_string(list_3D_points.at<float>(i, 2)) + ",\n";
    }
    return csv;
}

std::string generate_csv_from_Point3f(std::vector<cv::Point3f> list_3D_points) {
    std::string csv = "x,y,z,\n";
    for (int i = 0; i < list_3D_points.size(); i++) {
        csv += std::to_string(list_3D_points[i].x) + ","
               + std::to_string(list_3D_points[i].y) + ","
               + std::to_string(list_3D_points[i].z) + ",\n";
    }
    return csv;
}

std::string generate_csv_from_Point2f(std::vector<cv::Point2f> list_2D_points) {
    std::string csv = "x,y,z,\n";
    for (int i = 0; i < list_2D_points.size(); i++) {
        csv += std::to_string(list_2D_points[i].x) + ","
               + std::to_string(list_2D_points[i].y) + ",0,\n";
    }
    return csv;
}

cv::Mat drawPoint(cv::Mat image, int x, int y) {
    cv::Point center(x, y);
    int radius = 10;
    cv::Scalar line_Color(0, 0, 255);
    int thickness = 2;
    circle(image, center, radius, line_Color, thickness);
    return image;
}

cv::Mat drawRedPoint(cv::Mat image, int x, int y) {
    cv::Point center(x, y);
    int radius = 10;
    cv::Scalar line_Color(255, 0, 0);
    int thickness = 2;
    circle(image, center, radius, line_Color, thickness);
    return image;
}

int triangulation(jlong bt_first_frame, jlong bt_second_frame, jlong bt_ref_frame) {
    first_image = *(cv::Mat *) bt_first_frame;
    second_image = *(cv::Mat *) bt_second_frame;
    ref_image = *(cv::Mat *) bt_ref_frame;

    int image_x = first_image.cols;
    int image_y = first_image.rows;

    double center_x = image_x / 2;
    double center_y = image_y / 2;
    pnp_registration.setOpticalCenter(center_x, center_y);

//    todo hardcoded
//  focal_pixel = (focal_mm / sensor_width_mm) * image_width_in_pixels                              F(mm) = F(pixels) * SensorWidth(mm) / ImageWidth (pixel)

// S22
    double sensor_x = 8.16;
    double sensor_y = 6.12;
    double focal_length = 5.4;

// Motorola
//    double sensor_x = 5.7;
//    double sensor_y = 4.31;
//    double focal_length = 4.28;

    double focal_x = (focal_length / sensor_x) * image_x;
    double focal_y = (focal_length / sensor_y) * image_y;


    cv::Mat camera_matrix = cv::Mat::zeros(3, 3, CV_64FC1);
    camera_matrix.at<double>(0, 2) = center_x;
    camera_matrix.at<double>(1, 2) = center_y;
    camera_matrix.at<double>(0, 0) = focal_x;
    camera_matrix.at<double>(1, 1) = focal_y;
    camera_matrix.at<double>(2, 2) = 1;

    pnp_registration.setCameraMatrix(camera_matrix);
    pnp_detection.setCameraMatrix(camera_matrix);

    /**
     * Robust matcher
     * Dle tutorialu dostupneho na https://www.packtpub.com/books/content/learn-computer-vision-applications-open-cv
     * Dle tutorialu dostupneho na http://docs.opencv.org/3.1.0/dc/d2c/tutorial_real_time_pose.html
     */

    cv::Ptr<cv::SIFT> featureDetector = cv::SIFT::create(numKeyPoints);
    cv::Ptr<cv::SIFT> featureExtractor = cv::SIFT::create();
    std::vector<cv::KeyPoint> key_points_first_image, key_points_second_image;
    std::vector<cv::DMatch> matches;

    robustMatcher.setConfidenceLevel(confidenceLevel);
    robustMatcher.setMinDistanceToEpipolar(min_dist);
    robustMatcher.setRatio(ratioTest);
    robustMatcher.setFeatureDetector(featureDetector);
    robustMatcher.setDescriptorExtractor(featureExtractor);

    cv::Ptr<cv::DescriptorMatcher> descriptorMatcher = cv::makePtr<cv::FlannBasedMatcher>();
    robustMatcher.setDescriptorMatcher(descriptorMatcher);

    camera_matrix = pnp_registration.getCameraMatrix();

    cv::Mat essential_matrix = robustMatcher.robustMatchRANSAC(first_image, second_image, matches,
                                                               key_points_first_image,
                                                               key_points_second_image,
                                                               camera_matrix);

    /**
     * Rozklad matic
     */

    cv::Mat m1 = first_image.clone();
    cv::Mat m2 = second_image.clone();

    detection_points_first_image.clear();
    key_points_first_image_convert_table_to_3d_points.clear();
    detection_points_second_image.clear();


    for (std::vector<cv::DMatch>::const_iterator it = matches.begin(); it != matches.end(); ++it) {
        float x = key_points_first_image[it->queryIdx].pt.x;
        float y = key_points_first_image[it->queryIdx].pt.y;
        detection_points_first_image.push_back(cv::Point2f(x, y));
        key_points_first_image_convert_table_to_3d_points.push_back(it->queryIdx);
        drawPoint(m1, x, y);

        x = key_points_second_image[it->trainIdx].pt.x;
        y = key_points_second_image[it->trainIdx].pt.y;
        detection_points_second_image.push_back(cv::Point2f(x, y));
        drawPoint(m2, x, y);
    }
    saveMatToJpeg(m1, "rozklad_first", true);
    saveMatToJpeg(m2, "rozklad_second", true);

    cv::Mat R, t;
    cv::Mat points1 = cv::Mat(detection_points_first_image);
    cv::Mat points2 = cv::Mat(detection_points_second_image);
    cv::recoverPose(essential_matrix, points1, points2, camera_matrix, R, t);

    /**
     * Triangulace
     */

    cv::Mat rotation_translation_matrix_first_image, rotation_translation_matrix_second_image, result_3D_points,
            rotation_vector_first_image, translation_vector_first_image, found_3D_points, result_matrix;


    rotation_translation_matrix_first_image = cv::Mat::eye(3, 4, CV_64FC1);
    rotation_translation_matrix_second_image = cv::Mat::eye(3, 4, CV_64FC1);


    hconcat(R, t, rotation_translation_matrix_first_image);
    cv::Mat triangulation_3D_points, camera_matrix_a, camera_matrix_b;

    camera_matrix_a = camera_matrix * rotation_translation_matrix_first_image;
    camera_matrix_b = camera_matrix * rotation_translation_matrix_second_image;

    triangulatePoints(camera_matrix_b, camera_matrix_a, points1, points2, found_3D_points);

    transpose(found_3D_points, triangulation_3D_points);
    convertPointsFromHomogeneous(triangulation_3D_points, triangulation_3D_points);

    list_3D_points_after_triangulation.clear();
    for (int i = 0; i < triangulation_3D_points.rows; i++) {
        list_3D_points_after_triangulation.push_back(
                cv::Point3f(triangulation_3D_points.at<float>(i, 0),
                            triangulation_3D_points.at<float>(i, 1),
                            triangulation_3D_points.at<float>(i, 2)));
    }

//    https://chart-studio.plotly.com/create
//    std::string csv = generate_csv_from_Mat(triangulation_3D_points);
//    std::string csv3D = generate_csv_from_Point3f(list_3D_points_after_triangulation);
//    std::string csv2D_first = generate_csv_from_Point2f(detection_points_first_image);
//    std::string csv2D_second = generate_csv_from_Point2f(detection_points_second_image);

//  testif we can use SIFT to determine projection matrix from reference photo
//  TODO this should be true in production!!!!
    bool try_to_find_historic_position_now = true;
    if (try_to_find_historic_position_now) {
        std::vector<cv::DMatch> good_matches;
        std::vector<cv::KeyPoint> key_points_current_frame;
        robustMatcher.robustMatch(ref_image, good_matches, key_points_current_frame);

        cv::Mat m_ref = ref_image.clone();
        std::vector<cv::Point3f> list_points3d_model_match;
        std::vector<cv::Point2f> list_points2d_scene_match;
        for (unsigned int i = 0;
             i < good_matches.size();
             ++i) {

            // trainIdx patří k key_points_first_image proto převod pomocí tabulky vytvořené při triangulaci
            int index_in_3D_points = -1;
            auto it = find(key_points_first_image_convert_table_to_3d_points.begin(),
                           key_points_first_image_convert_table_to_3d_points.end(),
                           good_matches[i].queryIdx);
            if (it !=
                key_points_first_image_convert_table_to_3d_points.end()) {
                index_in_3D_points = it - key_points_first_image_convert_table_to_3d_points.begin();
            } else {
                continue;
            }

            cv::Point3f point3d_model = list_3D_points_after_triangulation[index_in_3D_points];
            cv::Point2f point2d_scene = key_points_current_frame[good_matches[i].trainIdx].pt;
            list_points3d_model_match.push_back(point3d_model);
            list_points2d_scene_match.push_back(point2d_scene);
            drawPoint(m_ref, point2d_scene.x, point2d_scene.y);
        }
        saveMatToJpeg(m_ref, "reference_points", true);
        bool good_measurement = false;
//        std::string csv2D_history = generate_csv_from_Point2f(list_points2d_scene_match);

        if (good_matches.size() > 0) {
            pnp_registration.estimatePoseRANSAC(list_points3d_model_match,
                                                list_points2d_scene_match,
                                                pnp_method,
                                                useExtrinsicGuess, iterationsCount,
                                                reprojectionError, confidence);
            int inliers_size = pnp_registration.getInliersPoints().size();
            if (inliers_size > minInliersKalman) {
                registration.setList2DPoints(list_points2d_scene_match);
                registration.setList3DPoints(list_points3d_model_match);
                return 1;
            }
        }
    }

    return 0;
}

void registration_prepare_points(void) {
    registration.setOriginal2DPoints(detection_points_second_image);
    registration.setOriginal3DPoints(list_3D_points_after_triangulation);
}

cv::Point2f registration_init() {
    //**
    // * Registrace korespondencnich bodu
    // * Dle tutorialu dostupneho na http://docs.opencv.org/3.1.0/dc/d2c/tutorial_real_time_pose.html
    // */

    registration.setRegistrationMax(number_registration);
    std::vector<cv::Point2f> emptyVector2D{};
    std::vector<cv::Point3f> emptyVector3D{};
    registration.setList2DPoints(emptyVector2D);
    registration.setList3DPoints(emptyVector3D);
    registration.setIndexRegistration(0);

    registration_prepare_points();

    float pos_x = registration.getOriginal2DPoints()[0].x;
    float pos_y = registration.getOriginal2DPoints()[0].y;

    cv::Point2f point = cv::Point2f(pos_x, pos_y);

    return point;
}

cv::Point2f registration_next_point() {

    registration.incRegistrationIndex();

    int index = registration.getIndexRegistration();

    float x = registration.getOriginal2DPoints()[index].x;
    float y = registration.getOriginal2DPoints()[index].y;

    cv::Point2f point = cv::Point2f(x, y);

    return point;
}

cv::Point2f registration_register_point(float x, float y) {
    cv::Point2f point_2d = cv::Point2f(x, y);
    bool is_registrable = registration.isRegistration();
    int index = registration.getIndexRegistration();

    if (is_registrable) {
        registration.register2DPoint(point_2d);
        cv::Point3f point3f = registration.getOriginal3DPoints()[index];
        registration.register3DPoint(point3f);
        index_points.push_back(index);
        index++;
    }

    registration.setIndexRegistration(index);

    float pos_x = registration.getOriginal2DPoints()[index].x;
    float pos_y = registration.getOriginal2DPoints()[index].y;

    cv::Point2f point = cv::Point2f(pos_x, pos_y);

    return point;
}

cv::Mat measurements;

int navigation_init() {

    std::vector<cv::Point3f> list_3D_points = registration.getList3DPoints();
    std::vector<cv::Point2f> list_2D_points = registration.getList2DPoints();

//    std::string csv_3D = generate_csv_from_Point3f(list_3D_points);
//    std::string csv_2D = generate_csv_from_Point2f(list_2D_points);

    /**
     * Vypocet optickeho stredu
     */

    std::vector<cv::Mat> vanish_point;
    cv::Mat output_ref_image, gray_ref_image;
    cv::Size size_ref_image;
    double cx, cy;

    size_ref_image = cv::Size(ref_image.cols, ref_image.rows);

    msac.init(mode, size_ref_image, verbose);
    resize(ref_image, ref_image, size_ref_image);

    if (ref_image.channels() == 4) {
        cv::cvtColor(ref_image, gray_ref_image, CV_BGRA2GRAY);
        ref_image.copyTo(output_ref_image);
    } else if (ref_image.channels() == 3) {
        cv::cvtColor(ref_image, gray_ref_image, CV_BGR2GRAY);
        ref_image.copyTo(output_ref_image);
    } else {
        ref_image.copyTo(gray_ref_image);
        cv::cvtColor(ref_image, output_ref_image, CV_GRAY2BGR);
    }

    cv::Mat output_ref_image_points, first_image_points;
    output_ref_image.copyTo(output_ref_image_points);
    first_image.copyTo(first_image_points);
    draw2DPoints(output_ref_image_points, list_2D_points, blue);
    saveMatToJpeg(output_ref_image_points, "output_ref_image_points", true);

    try {
        vanish_point = processImage(msac, numVps, gray_ref_image, output_ref_image);
    } catch (cv::Exception e) {
        std::cout << e.msg << std::endl;
        printf("ERROR find vanish point: %s", e.msg.c_str());
    }

    std::vector<cv::Point2f> list_vanish_points;
    cv::Point2f center;
    for (int i = 0; i < vanish_point.size(); i++) {
        double x = vanish_point[i].at<float>(0, 0);
        double y = vanish_point[i].at<float>(1, 0);
        if (x < ref_image.cols && y < ref_image.rows)
            list_vanish_points.push_back(
                    cv::Point2f(vanish_point[i].at<float>(0, 0),
                                vanish_point[i].at<float>(1, 0)));
    }


    if (list_vanish_points.size() == 3) {
        cv::Point2f A = list_vanish_points[0];
        cv::Point2f B = list_vanish_points[1];
        cv::Point2f C = list_vanish_points[2];

        cv::Point2f tmp;
        center.x = (A.x + B.x) / 2;
        center.y = (A.y + B.y) / 2;
        tmp.x = C.x;
        tmp.y = C.y;
        Line primka1(center.x, center.y, tmp.x, tmp.y);

        center.x = (A.x + C.x) / 2;
        center.y = (A.y + C.y) / 2;
        tmp.x = B.x;
        tmp.y = B.y;
        Line primka2(center.x, center.y, tmp.x, tmp.y);

        primka1.getIntersection(primka2, cy, cx);
    } else {
        cx = ref_image.cols / 2 - 0.5;
        cy = ref_image.rows / 2 - 0.5;
    }

    cx = abs((int) cx);
    cy = abs((int) cy);

    pnp_registration.setOpticalCenter(cx, cy);

    /**
     * Pozice historicke kamery
     */
    cv::Mat inliers_ref_frame;
    std::vector<cv::Point2f> list_points2d_scene_match;

    pnp_registration.estimatePoseRANSAC(list_3D_points, list_2D_points, pnp_method,
                                        useExtrinsicGuess, iterationsCount,
                                        reprojectionError, confidence);

    std::cout << "Historicka kamera: " << std::endl;
    std::cout << pnp_registration.getProjectionMatrix() << std::endl;

    std::string my_str = "my mat :";
    my_str << pnp_registration.getProjectionMatrix();


    /**
     * Robust matcher
     */

    cv::Mat current_frame, current_frame_vis, last_current_frame_vis;

    /**
     * Real-time Camera Pose Estimation
     */

    measurements = cv::Mat(nMeasurements, 1, CV_64F);
    measurements.setTo(cv::Scalar(0));

    initKalmanFilter(kalmanFilter, nStates, nMeasurements, nInputs, dt);

    robust_matcher_arg_struct.measurements = measurements;
    robust_matcher_arg_struct.position_relative_T = cv::Mat(4, 4, CV_64F);

    g_position_relative_T = cv::Mat(4, 4, CV_64F);
    robust_matcher_arg_struct.robust_done = false;
    fast_robust_matcher_arg_struct.robust_id = -1;


//    https://chart-studio.plotly.com/create
//    std::string csv = generate_csv_from_Point3f(list_3D_points);

    return 0;
}

cv::Mat processNavigation(long mat_current_frame, int count_frames) {
    printf("start processNavigation %d", count_frames);
    cv::Mat current_frame = *(cv::Mat *) mat_current_frame;

    cv::Mat last_current_frame_vis = current_frame.clone();
    cv::Mat current_frame_vis = current_frame.clone();
    current_frames.push_back(current_frame_vis);

    measurements = cv::Mat(nMeasurements, 1, CV_64F);
    measurements.setTo(cv::Scalar(0));

    int direction = 0;
    cv::Mat position_relative_T = cv::Mat(4, 4, CV_64F);

    if (count_frames == 1) {

        fill_robust_matcher_arg_struct(current_frame_vis, count_frames);
        robust_thread = std::thread(
                [&] { robust_matcher(&::robust_matcher_arg_struct); }
        );
        robust_thread.join();
        direction = robust_matcher_arg_struct.direction;
        position_relative_T = robust_matcher_arg_struct.position_relative_T.clone();
        robust_last_current_frame = robust_matcher_arg_struct.current_frame.clone();
        last_robust_id = robust_matcher_arg_struct.count_frames;

    } else if (count_frames == 2) {
        fill_robust_matcher_arg_struct(current_frame_vis, count_frames);
        robust_thread = std::thread(
                [&] { robust_matcher(&::robust_matcher_arg_struct); }
        );

        fill_fast_robust_matcher_arg_struct(current_frame_vis, direction, count_frames,
                                            robust_matcher_arg_struct.list_3D_points_for_lightweight,
                                            robust_matcher_arg_struct.list_2D_points_for_lightweight);
        fast_robust_matcher(&fast_robust_matcher_arg_struct);

        direction = fast_robust_matcher_arg_struct.direction;
        position_relative_T = g_position_relative_T.clone();

    } else if (robust_matcher_arg_struct.robust_done) {
        robust_thread.join();

        direction = robust_matcher_arg_struct.direction;
        position_relative_T = robust_matcher_arg_struct.position_relative_T.clone();
        robust_last_current_frame = robust_matcher_arg_struct.current_frame.clone();
        last_robust_id = robust_matcher_arg_struct.count_frames;

        fill_robust_matcher_arg_struct(current_frame_vis, count_frames);
        robust_thread = std::thread(
                [&] { robust_matcher(&::robust_matcher_arg_struct); }
        );

    } else {
        fill_fast_robust_matcher_arg_struct(current_frame_vis, direction, count_frames,
                                            robust_matcher_arg_struct.list_3D_points_for_lightweight,
                                            robust_matcher_arg_struct.list_2D_points_for_lightweight);
        fast_robust_matcher(&fast_robust_matcher_arg_struct);

        direction = fast_robust_matcher_arg_struct.direction;
        position_relative_T = g_position_relative_T;
    }
    return position_relative_T;
}

void fill_robust_matcher_arg_struct(cv::Mat current_frame_vis, int count_frames) {
    robust_matcher_arg_struct.current_frame = current_frame_vis.clone();
    robust_matcher_arg_struct.direction = 0;
    robust_matcher_arg_struct.count_frames = count_frames;
    robust_matcher_arg_struct.robust_done = false;
}

void
fill_fast_robust_matcher_arg_struct(cv::Mat current_frame_vis, int direction, int count_frames,
                                    std::vector<cv::Point3f> list_3D_points_from_robust,
                                    std::vector<cv::Point2f> list_2D_points_from_robust) {

    fast_robust_matcher_arg_struct.last_current_frame = robust_last_current_frame;
    fast_robust_matcher_arg_struct.current_frame = current_frame_vis.clone();
    g_list_3D_points_from_robust = list_3D_points_from_robust;
    g_list_2D_points_from_robust = list_2D_points_from_robust;
    fast_robust_matcher_arg_struct.direction = direction;
    fast_robust_matcher_arg_struct.count_frames = count_frames;
    fast_robust_matcher_arg_struct.robust_id = last_robust_id;
}


void *robust_matcher(void *arg) {
    struct robust_matcher_struct *param = (struct robust_matcher_struct *) arg;
    cv::Mat current_frame = param->current_frame;
    cv::Mat measurements = param->measurements;
    int direction = param->direction;
    int count_frames = param->count_frames;
    printf("+ %d started", count_frames);
    std::cout << "+ " << count_frames << " started" << std::endl;

    try {
        getRobustEstimation(current_frame, list_3D_points_after_triangulation, measurements,
                            direction, param->position_relative_T,
                            param->list_3D_points_for_lightweight,
                            param->list_2D_points_for_lightweight);
    } catch (cv::Exception e) {
        printf("ERROR robust_matcher: %s", e.msg.c_str());
    }

    printf("+ %d ended, direction %d", count_frames, direction);
    std::cout << "+ " << count_frames << " ended" << std::endl;
    param->direction = direction;
    param->robust_done = true;
    return nullptr;
}

void *fast_robust_matcher(void *arg) {
    struct fast_robust_matcher_struct *param = (struct fast_robust_matcher_struct *) arg;
    cv::Mat last_current_frame = param->last_current_frame;
    cv::Mat current_frame = param->current_frame;
    std::vector<cv::Point3f> list_3D_points_from_robust = g_list_3D_points_from_robust;
    std::vector<cv::Point2f> list_2D_points_from_robust = g_list_2D_points_from_robust;
    int direction = param->direction;
    int count_frames = param->count_frames;
    int robust_id = param->robust_id;
    printf("- %d started with %d", count_frames, robust_id);
    std::cout << "- " << count_frames << " started with " << robust_id << std::endl;

    getLightweightEstimation(last_current_frame,
                             list_3D_points_from_robust,
                             list_2D_points_from_robust,
                             current_frame, direction,
                             g_position_relative_T);

    param->direction = direction;
    printf("- %d ended with %d, direction %d", count_frames, robust_id, direction);
    std::cout << "- " << count_frames << " ended   with " << robust_id << std::endl;
    return nullptr;
}


bool getRobustEstimation(cv::Mat current_frame_vis, std::vector<cv::Point3f> list_3D_points,
                         cv::Mat measurements, int &directory, cv::Mat &position_relative_T,
                         std::vector<cv::Point3f> &list_3D_points_for_lightweight,
                         std::vector<cv::Point2f> &list_2D_points_for_lightweight) {

    double cx = current_frame_vis.cols / 2 - 0.5;
    double cy = current_frame_vis.rows / 2 - 0.5;

    cx = abs((int) cx);
    cy = abs((int) cy);

    pnp_detection.setOpticalCenter(cx, cy);

    std::vector<cv::DMatch> good_matches;
    std::vector<cv::KeyPoint> key_points_current_frame;
    std::vector<cv::Point3f> list_points3d_model_match;
    std::vector<cv::Point2f> list_points2d_scene_match;

    cv::Mat relative_T = cv::Mat(4, 4, CV_64F);
    robustMatcher.robustMatch(current_frame_vis, good_matches, key_points_current_frame);

    std::string gm = "train, query\n";

    for (unsigned int i = 0;
         i < good_matches.size();
         ++i) {

        // trainIdx patří k key_points_first_image proto převod pomocí tabulky vytvořené při triangulaci
        int index_in_3D_points = -1;
        auto it = find(key_points_first_image_convert_table_to_3d_points.begin(),
                       key_points_first_image_convert_table_to_3d_points.end(),
                       good_matches[i].queryIdx);
        if (it !=
            key_points_first_image_convert_table_to_3d_points.end()) {
            index_in_3D_points =
                    it - key_points_first_image_convert_table_to_3d_points.begin();
        } else {
            continue;
        }

        gm += std::to_string(index_in_3D_points) + "," +
              std::to_string(good_matches[i].trainIdx) + ",\n";

//    todo v tomhle bylo špatné pole
//      cv::Point3f point3d_model = list_3D_points[good_matches[i].trainIdx];
        cv::Point3f point3d_model = list_3D_points[index_in_3D_points];
        cv::Point2f point2d_scene = key_points_current_frame[good_matches[i].trainIdx].pt;
        list_points3d_model_match.push_back(point3d_model);
        list_points2d_scene_match.push_back(point2d_scene);
    }

//    std::string csv_list_3D_points = generate_csv_from_Point3f(list_points3d_model_match);

    cv::Mat image = current_frame_vis.clone();
    draw2DPoints(image, list_points2d_scene_match, blue);
    saveMatToJpeg(image, "current_frame_vis", true);

    bool good_measurement = false;

    if (good_matches.size() > 0) {

//    https://chart-studio.plotly.com/create
//        std::string csv_3D = generate_csv_from_Point3f(list_points3d_model_match);
//        std::string csv_2D = generate_csv_from_Point2f(list_points2d_scene_match);

        list_points3d_current_frame = list_points3d_model_match;
        list_points2d_current_frame = list_points2d_scene_match;
        pnp_detection.estimatePoseRANSAC(list_points3d_model_match,
                                         list_points2d_scene_match,
                                         pnp_method,
                                         useExtrinsicGuess, iterationsCount,
                                         reprojectionError,
                                         confidence);

        std::vector<cv::Point3f> v3(list_points3d_model_match.begin(),
                                    list_points3d_model_match.end());
        std::vector<cv::Point2f> v2(list_points2d_scene_match.begin(),
                                    list_points2d_scene_match.end());
        list_3D_points_for_lightweight = v3;
        list_2D_points_for_lightweight = v2;

        int inliers_size = pnp_detection.getInliersPoints().size();

        printf("inliers size: %d", inliers_size);
        if (inliers_size > minInliersKalman) {
            cv::Mat T = pnp_detection.getProjectionMatrix();
            cv::Mat reference_T = pnp_registration.getProjectionMatrix();
            relative_T = T.inv() * reference_T;

            print_matrix("relative_T", relative_T);

            double x = (double) relative_T.at<double>(0, 3);
            double y = (double) relative_T.at<double>(1, 3);
            double z = (double) relative_T.at<double>(2, 3);

            history_of_position_robust.emplace_back(
                    (float) T.at<double>(0, 3),
                    (float) T.at<double>(1, 3),
                    (float) T.at<double>(2, 3));
//    https://chart-studio.plotly.com/create
//            std::string history_of_position_robust_csv = generate_csv_from_Point3f(
//                    history_of_position_robust);
            printf("robust position push")
            std::string history_of_position_fast_csv = generate_csv_from_Point3f(
                    history_of_position_fast);
            std::string points_from_triangulation_csv = generate_csv_from_Point3f(list_3D_points);

            std::vector<cv::Point3f> reference_position;
            reference_position.emplace_back((float) reference_T.at<double>(0, 3),
                                            (float) reference_T.at<double>(1, 3),
                                            (float) reference_T.at<double>(2, 3));
//            std::string reference_position_csv = generate_csv_from_Point3f(reference_position);

            directory = getDirectory(x, y);
            position_relative_T = relative_T;
            printf("real direction: %d, x=%f, y=%f", directory, x, y);

            cv::Mat translation_measured(3, 1, CV_64F);
            translation_measured = pnp_detection.getTranslationMatrix();

//            std::string csv_translation = generate_csv_from_Mat(translation_measured);

            cv::Mat rotation_measured(3, 3, CV_64F);
            rotation_measured = pnp_detection.getRotationMatrix();

//            std::string csv_rotation = generate_csv_from_Mat(rotation_measured);

            good_measurement = true;
            fillMeasurements(measurements, translation_measured, rotation_measured);

            double pos = relative_T.at<double>(3, 0) + relative_T.at<double>(3, 1);
            pos /= 2;

            position.push_back(pos);
            frames.push_back(current_frame_vis);

            indexFrame++;
        }
    }

    cv::Mat translation_estimated(3, 1, CV_64F);
    cv::Mat rotation_estimated(3, 3, CV_64F);

    updateKalmanFilter(kalmanFilter, measurements, translation_estimated,
                       rotation_estimated);
    pnp_detection.setProjectionMatrix(rotation_estimated, translation_estimated);

    return good_measurement;
}


void print_matrix(const char *label, cv::Mat mat) {
    printf("mat %s\n%f, %f, %f, %f \n%f, %f, %f, %f \n%f, %f, %f, %f \n%f, %f, %f, %f",
           label,
           (double) mat.at<double>(0, 0),
           (double) mat.at<double>(0, 1),
           (double) mat.at<double>(0, 2),
           (double) mat.at<double>(0, 3),

           (double) mat.at<double>(1, 0),
           (double) mat.at<double>(1, 1),
           (double) mat.at<double>(1, 2),
           (double) mat.at<double>(1, 3),

           (double) mat.at<double>(2, 0),
           (double) mat.at<double>(2, 1),
           (double) mat.at<double>(2, 2),
           (double) mat.at<double>(2, 3),

           (double) mat.at<double>(3, 0),
           (double) mat.at<double>(3, 1),
           (double) mat.at<double>(3, 2),
           (double) mat.at<double>(3, 3));
}

void printOpticalFlow(cv::Mat previous_image, cv::Mat current_image,
                      std::vector<cv::Point2f> previous_points,
                      std::vector<cv::Point2f> current_points) {
    cv::Mat m1 = previous_image.clone();
    cv::Mat m2 = current_image.clone();
    for (int i = 0; i < previous_points.size(); i++) {
        int x = static_cast<int>(previous_points[i].x);
        int y = static_cast<int>(previous_points[i].y);
        drawPoint(m1, x, y);
        drawPoint(m2, x, y);
    }
    for (int i = 0; i < current_points.size(); i++) {
        int x = static_cast<int>(current_points[i].x);
        int y = static_cast<int>(current_points[i].y);
        drawRedPoint(m1, x, y);
        drawRedPoint(m2, x, y);
    }
    saveMatToJpeg(m1, "opticalFlowPrevious", true);
    saveMatToJpeg(m2, "opticalFlowCurrent", true);
}

bool
getLightweightEstimation(cv::Mat last_current_frame_vis,
                         std::vector<cv::Point3f> list_3D_points_of_last_current_frame,
                         std::vector<cv::Point2f> list_2D_points_of_last_current_frame,
                         cv::Mat current_frame_vis, int &directory, cv::Mat &position_relative_T) {


    std::vector<cv::Point2f> featuresPrevious = list_2D_points_of_last_current_frame;
    std::vector<cv::Point2f> featuresCurrent = list_2D_points_of_last_current_frame;
    std::vector<cv::Point2f> featuresNextPos;
    std::vector<uchar> featuresFound;

    cv::Mat last, current, err;
    cv::Mat revT = cv::Mat(4, 4, CV_64F);


    cvtColor(last_current_frame_vis, last, CV_BGR2GRAY);
    cvtColor(current_frame_vis, current, CV_BGR2GRAY);

    last.convertTo(last_current_frame_vis, CV_8U);
    current.convertTo(current_frame_vis, CV_8U);
    featuresPrevious = featuresCurrent;

    if (!featuresPrevious.empty()) {
        goodFeaturesToTrack(current_frame_vis, featuresCurrent, 30, 0.01, 30);

        calcOpticalFlowPyrLK(last, current, featuresPrevious, featuresNextPos,
                             featuresFound, err);

        printOpticalFlow(last_current_frame_vis, current_frame_vis, featuresPrevious,
                         featuresNextPos);

        std::vector<cv::Point2f> list_points2d_scene_match;
        std::vector<cv::Point3f> list_points3d_scene_match;

        for (size_t i = 0; i < featuresNextPos.size(); i++) {
            if (featuresFound[i]) {
                list_points2d_scene_match.push_back(featuresNextPos[i]);

                auto it = find(list_2D_points_of_last_current_frame.begin(),
                               list_2D_points_of_last_current_frame.end(),
                               featuresPrevious[i]);
                int index_in_3D_points = -1;
                if (it != list_2D_points_of_last_current_frame.end()) {
                    index_in_3D_points = it - list_2D_points_of_last_current_frame.begin();
                } else {
                    continue;
                }
                list_points3d_scene_match.push_back(
                        list_3D_points_of_last_current_frame[index_in_3D_points]);
            }
        }

        if (list_points3d_scene_match.size() == list_points2d_scene_match.size()) {
            list_points3d_current_frame = list_points3d_scene_match;
            list_points2d_current_frame = list_points2d_scene_match;
            pnp_detection.estimatePoseRANSAC(list_points3d_scene_match, list_points2d_scene_match,
                                             pnp_method,
                                             useExtrinsicGuess,
                                             iterationsCount, reprojectionError,
                                             confidence);

            if (pnp_detection.getInliersPoints().size() > minInliersKalman) {

                draw2DPoints(current_frame_vis, list_points2d_scene_match, red);
                cv::Mat T = pnp_detection.getProjectionMatrix();
                cv::Mat refT = pnp_registration.getProjectionMatrix();


                history_of_position_fast.emplace_back(
                        (float) T.at<double>(0, 3),
                        (float) T.at<double>(1, 3),
                        (float) T.at<double>(2, 3));
                //    https://chart-studio.plotly.com/create
//                std::string history_of_position_fast_csv = generate_csv_from_Point3f(
//                        history_of_position_fast);

                revT = T.inv() * refT;

                double x = (double) revT.at<double>(0, 3);
                double y = (double) revT.at<double>(1, 3);
                double z = (double) revT.at<double>(2, 3);

                position_relative_T = revT;

                directory = getDirectory(x, y);
            }
        }
    }

    return true;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_triangulation(JNIEnv *env, jclass clazz,
                                                       jlong bt_first_frame,
                                                       jlong bt_second_frame,
                                                       jlong bt_ref_frame) {
    return triangulation(bt_first_frame, bt_second_frame, bt_ref_frame);
}


extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_registration_1init(JNIEnv *env, jclass clazz) {
    jfloatArray params = (*env).NewFloatArray(2);
    cv::Point2f point = registration_init();
    jfloat position[2];
    position[0] = point.x;
    position[1] = point.y;
    env->SetFloatArrayRegion(params, 0, 2, position);
    return params;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_registration_1register_1point(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jdouble x,
                                                                       jdouble y) {
    jfloatArray params = (*env).NewFloatArray(2);
    cv::Point2f point = registration_register_point(x, y);
    jfloat position[2];
    position[0] = point.x;
    position[1] = point.y;
    env->SetFloatArrayRegion(params, 0, 2, position);
    return params;
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_registration_1next_1point(JNIEnv *env,
                                                                   jclass clazz) {
    jfloatArray params = (*env).NewFloatArray(2);
    cv::Point2f point = registration_next_point();
    jfloat position[2];
    position[0] = point.x;
    position[1] = point.y;
    env->SetFloatArrayRegion(params, 0, 2, position);
    return params;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_navigation_1init(JNIEnv *env, jclass clazz) {
    return navigation_init();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_process_1navigation(JNIEnv *env, jclass clazz,
                                                             jlong mat_current_frame,
                                                             jint count_frames) {
    cv::Mat returned = processNavigation(mat_current_frame, count_frames);
    cv::Mat *mat = new cv::Mat(returned);
    return (jlong) mat;
}

cv::Mat warpImageWithProjectionMatrix(cv::Mat image, cv::Mat projection_matrix) {
    std::vector<cv::Point3f> point_cloud = list_points3d_current_frame;

    std::vector<cv::Point3f> transformed_point_cloud;
    for (const cv::Point3f &point: point_cloud) {
        cv::Mat point_mat(4, 1, CV_32F);
        point_mat.at<float>(0) = point.x;
        point_mat.at<float>(1) = point.y;
        point_mat.at<float>(2) = point.z;
        point_mat.at<float>(3) = 1;

        projection_matrix.convertTo(projection_matrix, CV_32F);

        cv::Mat transformed_point_mat = projection_matrix * point_mat;
        transformed_point_cloud.push_back(cv::Point3f(
                transformed_point_mat.at<float>(0) / transformed_point_mat.at<float>(3),
                transformed_point_mat.at<float>(1) / transformed_point_mat.at<float>(3),
                transformed_point_mat.at<float>(2) / transformed_point_mat.at<float>(3)
        ));
    }

    // Project the transformed 3D points back onto a 2D image plane
    cv::Mat K = pnp_registration.getCameraMatrix();
    K.convertTo(K, CV_32F);
    K.at<float>(0, 2) = image.cols / 2;
    K.at<float>(1, 2) = image.rows / 2;

    std::vector<cv::Point2f> dstPoints;
    for (const cv::Point3f &point: transformed_point_cloud) {
        cv::Point2f image_point;
        image_point.x = K.at<float>(0, 0) * point.x / point.z + K.at<float>(0, 2);
        image_point.y = K.at<float>(1, 1) * point.y / point.z + K.at<float>(1, 2);

        dstPoints.push_back(image_point);
    }

    std::vector<cv::Point2f> srcPoints = list_points2d_current_frame;
    cv::Mat homography = cv::findHomography(srcPoints, dstPoints);
    cv::Mat transformed_image;
    cv::warpPerspective(image, transformed_image, homography, image.size());
    return transformed_image;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_vyw_rephotoandroid_OpenCVNative_warpPerspectiveOfRephoto(JNIEnv *env, jclass clazz,
                                                                  jlong rephoto,
                                                                  jlong reference_image) {
    cv::Mat mat_rephoto = *(cv::Mat *) rephoto;
    cv::Mat mat_ref_image = *(cv::Mat *) reference_image;
    std::vector<cv::DMatch> matches;
    std::vector<cv::KeyPoint> key_points_ref_image;
    std::vector<cv::KeyPoint> key_points_rephoto;
    cv::Mat camera_matrix = cv::Mat::zeros(3, 3, CV_64FC1);
    camera_matrix = pnp_registration.getCameraMatrix();

    robustMatcher.robustMatchRANSAC(mat_ref_image, mat_rephoto, matches,
                                    key_points_ref_image,
                                    key_points_rephoto,
                                    camera_matrix);

    std::vector<cv::Point2f> points_ref_image;
    std::vector<cv::Point2f> points_rephoto;

//    TODO UNCOMENT RIGHT NOW
    if (matches.size() > minInliersKalman) {
//    if (false) {
        // dostatek nově nalezených společných bodů

        cv::Mat m1 = mat_ref_image.clone();
        cv::Mat m2 = mat_rephoto.clone();
        for (std::vector<cv::DMatch>::const_iterator it = matches.begin();
             it != matches.end(); ++it) {
            float x = key_points_ref_image[it->queryIdx].pt.x;
            float y = key_points_ref_image[it->queryIdx].pt.y;
            points_ref_image.push_back(cv::Point2f(x, y));
            drawPoint(m1, x, y);

            x = key_points_rephoto[it->trainIdx].pt.x;
            y = key_points_rephoto[it->trainIdx].pt.y;
            points_rephoto.push_back(cv::Point2f(x, y));
            drawPoint(m2, x, y);
        }
        saveMatToJpeg(m1, "upload_points_ref_image", true);
        saveMatToJpeg(m2, "upload_points_rephoto", true);

        cv::Mat homographyMatrix = cv::findHomography(points_rephoto, points_ref_image, cv::RANSAC,
                                                      1.0);

        int h1 = mat_ref_image.rows;
        int w1 = mat_ref_image.cols;
        int h2 = mat_rephoto.rows;
        int w2 = mat_rephoto.cols;
        double test = homographyMatrix.at<double>(0, 0);
        double test2 = homographyMatrix.at<double>(1, 1);
        if (h1 < h2 and w1 < w2) {
            homographyMatrix.at<double>(0, 0) *= static_cast<double>(w2) / w1;
            homographyMatrix.at<double>(1, 1) *= static_cast<double>(h2) / h1;
        }
        double test3 = homographyMatrix.at<double>(0, 0);
        double test4 = homographyMatrix.at<double>(1, 1);

        cv::Mat dst_mat_rephoto = mat_rephoto.clone();
        cv::warpPerspective(mat_rephoto, dst_mat_rephoto, homographyMatrix, mat_ref_image.size());

        cv::Mat *mat = new cv::Mat(dst_mat_rephoto);
        return (jlong) mat;
    } else {
        // pouziti bodu z registrace

        cv::Mat warped_img_mat = mat_rephoto.clone();
        warped_img_mat = warpImageWithProjectionMatrix(mat_rephoto,
                                                       pnp_registration.getProjectionMatrix());
        saveMatToJpeg(warped_img_mat, "test", true);
        cv::cvtColor(warped_img_mat, warped_img_mat, cv::COLOR_BGRA2RGBA);
        cv::Mat *mat = new cv::Mat(warped_img_mat);
        return (jlong) mat;
    }
}

cv::Mat loadImage(const std::string path_to_ref_image) {

    cv::Mat image = cv::imread(path_to_ref_image);

    if (!image.data) {
        exit(1);
    }
    return image;
}


int getDirectory(double x, double y) {
    int directory = 0;
    if (x == 0) {
        if (y > 0) {
            directory = 1;
        } else if (y < 0) {
            directory = 2;
        }
    } else if (y == 0) {
        if (x > 0) {
            directory = 3;
        } else if (x < 0) {
            directory = 4;
        }
    } else if (x < 0 && y < 0) {
        directory = 14;
    } else if (x > 0 && y < 0) {
        directory = 13;
    } else if (x < 0 && y > 0) {
        directory = 24;
    } else if (x > 0 && y > 0) {
        directory = 23;
    }
    return directory;
}

std::vector<cv::Mat>
processImage(MSAC &msac, int numVps, cv::Mat &imgGRAY, cv::Mat &outputImg) {
    cv::Mat imgCanny;
    cv::Canny(imgGRAY, imgCanny, 180, 120, 3);
    std::vector<std::vector<cv::Point> > lineSegments;
    std::vector<cv::Point> aux;
#ifndef USE_PPHT
    vector <Vec2f> lines;
    cv::HoughLines(imgCanny, lines, 1, CV_PI / 180, 200);

    for (size_t i = 0; i < lines.size(); i++) {
        float rho = lines[i][0];
        float theta = lines[i][1];

        double a = cos(theta), b = sin(theta);
        double x0 = a * rho, y0 = b * rho;

        Point pt1, pt2;
        pt1.x = cvRound(x0 + 1000 * (-b));
        pt1.y = cvRound(y0 + 1000 * (a));
        pt2.x = cvRound(x0 - 1000 * (-b));
        pt2.y = cvRound(y0 - 1000 * (a));

        aux.clear();
        aux.push_back(pt1);
        aux.push_back(pt2);
        lineSegments.push_back(aux);

        line(outputImg, pt1, pt2, CV_RGB(0, 0, 0), 1, 8);

    }
#else
    std::vector<cv::Vec4i> lines;
    int houghThreshold = 70;
    if (imgGRAY.cols * imgGRAY.rows < 400 * 400)
        houghThreshold = 100;

    cv::HoughLinesP(imgCanny, lines, 1, CV_PI / 180, houghThreshold, 10, 10);

    while (lines.size() > MAX_NUM_LINES) {
        lines.clear();
        houghThreshold += 10;
        cv::HoughLinesP(imgCanny, lines, 1, CV_PI / 180, houghThreshold, 10, 10);
    }
    for (size_t i = 0; i < lines.size(); i++) {
        cv::Point pt1, pt2;
        pt1.x = lines[i][0];
        pt1.y = lines[i][1];
        pt2.x = lines[i][2];
        pt2.y = lines[i][3];
        line(outputImg, pt1, pt2, CV_RGB(0, 0, 0), 2);

        aux.clear();
        aux.push_back(pt1);
        aux.push_back(pt2);
        lineSegments.push_back(aux);
    }
    saveMatToJpeg(outputImg, "vanish_lines", true);

#endif

    std::vector<cv::Mat> vps;
    std::vector<std::vector<int> > CS;
    std::vector<int> numInliers;

    std::vector<std::vector<std::vector<cv::Point> > > lineSegmentsClusters;


    try {
        msac.multipleVPEstimation(lineSegments, lineSegmentsClusters, numInliers, vps, numVps);
    } catch (cv::Exception e) {
        std::cout << e.msg << std::endl;
        printf("ERROR VPE estimation: %s", e.msg.c_str());
    }
    for (int v = 0; v < vps.size(); v++) {
        double vpNorm = cv::norm(vps[v]);
        if (fabs(vpNorm - 1) < 0.001) {
        }
    }

    msac.drawCS(outputImg, lineSegmentsClusters, vps);
    saveMatToJpeg(outputImg, "vanish_points", true);
    return vps;
}


/**********************************************************************************************************/
void
initKalmanFilter(cv::KalmanFilter &KF, int nStates, int nMeasurements, int nInputs,
                 double dt) {

    KF.init(nStates, nMeasurements, nInputs, CV_64F);
    setIdentity(KF.processNoiseCov, cv::Scalar::all(1e-5));
    setIdentity(KF.measurementNoiseCov, cv::Scalar::all(1e-2));
    setIdentity(KF.errorCovPost, cv::Scalar::all(1));

    /** DYNAMIC MODEL **/

    //  [1 0 0 dt  0  0 dt2   0   0 0 0 0  0  0  0   0   0   0]
    //  [0 1 0  0 dt  0   0 dt2   0 0 0 0  0  0  0   0   0   0]
    //  [0 0 1  0  0 dt   0   0 dt2 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  1  0  0  dt   0   0 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  0  1  0   0  dt   0 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  0  0  1   0   0  dt 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  0  0  0   1   0   0 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  0  0  0   0   1   0 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  0  0  0   0   0   1 0 0 0  0  0  0   0   0   0]
    //  [0 0 0  0  0  0   0   0   0 1 0 0 dt  0  0 dt2   0   0]
    //  [0 0 0  0  0  0   0   0   0 0 1 0  0 dt  0   0 dt2   0]
    //  [0 0 0  0  0  0   0   0   0 0 0 1  0  0 dt   0   0 dt2]
    //  [0 0 0  0  0  0   0   0   0 0 0 0  1  0  0  dt   0   0]
    //  [0 0 0  0  0  0   0   0   0 0 0 0  0  1  0   0  dt   0]
    //  [0 0 0  0  0  0   0   0   0 0 0 0  0  0  1   0   0  dt]
    //  [0 0 0  0  0  0   0   0   0 0 0 0  0  0  0   1   0   0]
    //  [0 0 0  0  0  0   0   0   0 0 0 0  0  0  0   0   1   0]
    //  [0 0 0  0  0  0   0   0   0 0 0 0  0  0  0   0   0   1]

    // position
    KF.transitionMatrix.at<double>(0, 3) = dt;
    KF.transitionMatrix.at<double>(1, 4) = dt;
    KF.transitionMatrix.at<double>(2, 5) = dt;
    KF.transitionMatrix.at<double>(3, 6) = dt;
    KF.transitionMatrix.at<double>(4, 7) = dt;
    KF.transitionMatrix.at<double>(5, 8) = dt;
    KF.transitionMatrix.at<double>(0, 6) = 0.5 * dt * dt;
    KF.transitionMatrix.at<double>(1, 7) = 0.5 * dt * dt;
    KF.transitionMatrix.at<double>(2, 8) = 0.5 * dt * dt;

    // orientation
    KF.transitionMatrix.at<double>(9, 12) = dt;
    KF.transitionMatrix.at<double>(10, 13) = dt;
    KF.transitionMatrix.at<double>(11, 14) = dt;
    KF.transitionMatrix.at<double>(12, 15) = dt;
    KF.transitionMatrix.at<double>(13, 16) = dt;
    KF.transitionMatrix.at<double>(14, 17) = dt;
    KF.transitionMatrix.at<double>(9, 15) = 0.5 * dt * dt;
    KF.transitionMatrix.at<double>(10, 16) = 0.5 * dt * dt;
    KF.transitionMatrix.at<double>(11, 17) = 0.5 * dt * dt;


    /** Mereny model **/

    //  [1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
    //  [0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
    //  [0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
    //  [0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0]
    //  [0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0]
    //  [0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0]

    KF.measurementMatrix.at<double>(0, 0) = 1;  // x
    KF.measurementMatrix.at<double>(1, 1) = 1;  // y
    KF.measurementMatrix.at<double>(2, 2) = 1;  // z
    KF.measurementMatrix.at<double>(3, 9) = 1;  // roll
    KF.measurementMatrix.at<double>(4, 10) = 1; // pitch
    KF.measurementMatrix.at<double>(5, 11) = 1; // yaw

}

void updateKalmanFilter(cv::KalmanFilter &KF, cv::Mat &measurement,
                        cv::Mat &translation_estimated,
                        cv::Mat &rotation_estimated) {

    cv::Mat prediction = KF.predict();

    cv::Mat estimated = KF.correct(measurement);

    translation_estimated.at<double>(0) = estimated.at<double>(0);
    translation_estimated.at<double>(1) = estimated.at<double>(1);
    translation_estimated.at<double>(2) = estimated.at<double>(2);


    cv::Mat eulers_estimated(3, 1, CV_64F);
    eulers_estimated.at<double>(0) = estimated.at<double>(9);
    eulers_estimated.at<double>(1) = estimated.at<double>(10);
    eulers_estimated.at<double>(2) = estimated.at<double>(11);

    rotation_estimated = euler2rot(eulers_estimated);
}

void fillMeasurements(cv::Mat &measurements, const cv::Mat &translation_measured,
                      const cv::Mat &rotation_measured) {

    cv::Mat measured_eulers(3, 1, CV_64F);
    measured_eulers = rot2euler(rotation_measured);

    measurements.at<double>(0) = translation_measured.at<double>(0); // x
    measurements.at<double>(1) = translation_measured.at<double>(1); // y
    measurements.at<double>(2) = translation_measured.at<double>(2); // z
    measurements.at<double>(3) = measured_eulers.at<double>(0);      // roll
    measurements.at<double>(4) = measured_eulers.at<double>(1);      // pitch
    measurements.at<double>(5) = measured_eulers.at<double>(2);      // yaw
}