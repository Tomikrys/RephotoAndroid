/**
 * Main.cpp
 * Author: Adam Červenka <xcerve16@stud.fit.vutbr.cz>
 *
 */

#include "Main_orig.h"
#include <opencv2/features2d.hpp>
#include <opencv2/imgproc/types_c.h>
#include <thread>
#include <pthread.h>
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

int triangulation(jlong bt_first_frame, jlong bt_second_frame, jlong bt_ref_frame) {
    first_image = *(cv::Mat *) bt_first_frame;
    second_image = *(cv::Mat *) bt_second_frame;
    ref_image = *(cv::Mat *) bt_ref_frame;

//    cv::Mat first_image = loadImage(path_to_first_image);
//    cv::Mat second_image = loadImage(path_to_second_image);

    /**
     * Kalibrace fotoaparatu
     * Dle tutorialu dostupneho na https://www.packtpub.com/books/content/learn-computer-vision-applications-open-cv
     */

//    std::vector<std::string> list_file_name;
//    cv::Size border_size(7, 9);
//
//    for (int i = 1; i <= 12; i++) {
//        std::stringstream file_name;
//        file_name << "resource/image/chessboards/chessboard" << std::setw(2) << std::setfill('0')
//                  << i << ".jpg";
//        list_file_name.push_back(file_name.str());
//    }
//
//    cameraCalibrator.addChessboardPoints(list_file_name, border_size);
//    cameraCalibrator.calibrate(first_image.size());

//    cv::Mat camera_matrix = cameraCalibrator.getCameraMatrix();


    int image_x = first_image.cols;
    int image_y = first_image.rows;

    double center_x = image_x / 2;
    double center_y = image_y / 2;
    pnp_registration.setOpticalCenter(center_x, center_y);

//    todo hardcoded
//  focal_pixel = (focal_mm / sensor_width_mm) * image_width_in_pixels                              F(mm) = F(pixels) * SensorWidth(mm) / ImageWidth (pixel)

// S22
//    double sensor_x = 8.16;
//    double sensor_y = 6.12;
//    double focal_length = 5.4;

// Motorola
    double sensor_x = 5.7;
    double sensor_y = 4.31;
    double focal_length = 4.28;

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

//    pnp_registration.setCameraParameter(center_x, center_y, focal_x, focal_y);

//
//    /**
//     * Robust matcher
//     * Dle tutorialu dostupneho na https://www.packtpub.com/books/content/learn-computer-vision-applications-open-cv
//     * Dle tutorialu dostupneho na http://docs.opencv.org/3.1.0/dc/d2c/tutorial_real_time_pose.html
//     */
//
////    todo doplnit xfeatures abych mohl použít SURF
//    cv::Ptr<int> featureDetector = NULL;
//    cv::Ptr<int> featureExtractor = NULL;
//    cv::Ptr<cv::xfeatures2d::SurfFeatureDetector> featureDetector = cv::xfeatures2d::SurfFeatureDetector::create(
//            numKeyPoints);
//    cv::Ptr<cv::xfeatures2d::SurfDescriptorExtractor> featureExtractor = cv::xfeatures2d::SurfDescriptorExtractor::create();
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

//    /**
//     * Rozklad matic
//     */

    cv::Mat m1 = first_image.clone();
    cv::Mat m2 = second_image.clone();

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

//    /**
//     * Triangulace
//     */

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

    for (int i = 0; i < triangulation_3D_points.rows; i++) {
        list_3D_points_after_triangulation.push_back(
                cv::Point3f(triangulation_3D_points.at<float>(i, 0),
                            triangulation_3D_points.at<float>(i, 1),
                            triangulation_3D_points.at<float>(i, 2)));
    }

//    https://chart-studio.plotly.com/create
    std::string csv = generate_csv_from_Mat(triangulation_3D_points);
    std::string csv3D = generate_csv_from_Point3f(list_3D_points_after_triangulation);
    std::string csv2D_first = generate_csv_from_Point2f(detection_points_first_image);
    std::string csv2D_second = generate_csv_from_Point2f(detection_points_second_image);

//  testif we can use SIFT to determine projection matrix from reference photo
    std::vector<cv::DMatch> good_matches;
    std::vector<cv::KeyPoint> key_points_current_frame;
    robustMatcher.robustMatch(ref_image, good_matches, key_points_current_frame);

    std::vector<cv::Point3f> list_points3d_model_match;
    std::vector<cv::Point2f> list_points2d_scene_match;
    for (unsigned int i = 0;
         i < good_matches.size();
         ++i) { // todo list_3D_points je menší než dává good_matchees

        // trainIdx patří k key_points_first_image proto převod pomocí tabulky vytvořené při triangulaci
        int index_in_3D_points = -1;
        auto it = find(key_points_first_image_convert_table_to_3d_points.begin(),
                       key_points_first_image_convert_table_to_3d_points.end(),
                       good_matches[i].queryIdx);
        if (it != key_points_first_image_convert_table_to_3d_points.end()) { // If element was found
            // calculating the index_in_3D_points of good_matches[i].trainIdx
            index_in_3D_points = it - key_points_first_image_convert_table_to_3d_points.begin();
        } else {
            continue; // If the element is not present in the vector
        }

        cv::Point3f point3d_model = list_3D_points_after_triangulation[index_in_3D_points];
        cv::Point2f point2d_scene = key_points_current_frame[good_matches[i].trainIdx].pt;
        list_points3d_model_match.push_back(point3d_model);
        list_points2d_scene_match.push_back(point2d_scene);
    }
    bool good_measurement = false;

    if (good_matches.size() > 0) {
        pnp_registration.estimatePoseRANSAC(list_points3d_model_match, list_points2d_scene_match,
                                            pnp_method,
                                            useExtrinsicGuess, iterationsCount,
                                            reprojectionError, confidence);
        int inliers_size = pnp_registration.getInliersPoints().size();
//        TODO rovnou to poslat tam, a+t se to ned2l8 znova ten v7sledek estimate pose.
        if (inliers_size > minInliersKalman) {
            registration.setList2DPoints(list_points2d_scene_match);
            registration.setList3DPoints(list_points3d_model_match);
            return 1;
        }
    }

    return 0;
}

cv::Point2f registration_init() {
    //**
    // * Registrace korespondencnich bodu
    // * Dle tutorialu dostupneho na http://docs.opencv.org/3.1.0/dc/d2c/tutorial_real_time_pose.html
    // */
    for (int i = 0; i < list_3D_points_after_triangulation.size(); i++) {
        std::cout << list_3D_points_after_triangulation[i] << std::endl;
    }
    registration.setRegistrationMax(number_registration);

    float pos_x = detection_points_first_image[0].x;
    float pos_y = detection_points_first_image[0].y;

    cv::Point2f point = cv::Point2f(pos_x, pos_y);

    return point;
}

cv::Point2f registration_next_point() {

    registration.incRegistrationIndex();

    cv::Mat clone_frame_with_triangulation = first_image.clone();
    int index = registration.getIndexRegistration();

    float x = detection_points_first_image[index].x;
    float y = detection_points_first_image[index].y;

    cv::Point2f point = cv::Point2f(x, y);

    return point;
}

cv::Point2f registration_register_point(float x, float y) {
    cv::Point2f point_2d = cv::Point2f(x, y);
    bool is_registrable = registration.isRegistration();
    int index = registration.getIndexRegistration();

    if (is_registrable) {
        registration.register2DPoint(point_2d);
        cv::Point3f point3f = list_3D_points_after_triangulation[index];
        registration.register3DPoint(point3f);
        index_points.push_back(index);
        index++;
    }

    cv::Mat clone_frame_with_triangulation = first_image.clone();
    registration.setIndexRegistration(index);

    float pos_x = detection_points_first_image[index].x;
    float pos_y = detection_points_first_image[index].y;

    cv::Point2f point = cv::Point2f(pos_x, pos_y);

    return point;
}

cv::Mat measurements;

int navigation_init() {
    start = 4;
    end = 9;
    robust_matcher_arg_struct.robust_done = false;
    fast_robust_matcher_arg_struct.robust_id = -1;


    std::vector<cv::Point3f> list_3D_points = registration.getList3DPoints();
    std::vector<cv::Point2f> list_2D_points = registration.getList2DPoints();


    std::string csv_3D = generate_csv_from_Point3f(list_3D_points);
    std::string csv_2D = generate_csv_from_Point2f(list_2D_points);

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
//    TODO does not find vanish_point OpenCV(4.5.3) Error: Bad argument (Matrix operand is an empty matrix.) in checkOperandsExist
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

    //robust_matcher_arg_struct.list_3D_points = list_3D_points_after_triangulation;
    //robust_matcher_arg_struct.measurements = measurements;

    //fast_robust_matcher_arg_struct.list_3D_points = list_3D_points_after_triangulation;

//    https://chart-studio.plotly.com/create
    std::string csv = generate_csv_from_Point3f(list_3D_points);

    return 0;
}

cv::Mat processNavigation(long mat_current_frame, int count_frames) {

    cv::Mat current_frame = *(cv::Mat *) mat_current_frame;

    cv::Mat last_current_frame_vis = current_frame.clone();
    cv::Mat current_frame_vis = current_frame.clone();
//    current_frames.push_back(current_frame_vis);

    measurements = cv::Mat(nMeasurements, 1, CV_64F);
    measurements.setTo(cv::Scalar(0));

    int direction = 0;
    cv::Mat position_relative_T = cv::Mat(4, 4, CV_64F);
    try {
        getRobustEstimation(current_frame_vis, list_3D_points_after_triangulation, measurements,
                            direction, position_relative_T);

//        robust_matcher_arg_struct.current_frame = current_frame_vis;
//        robust_matcher_arg_struct.list_3D_points = list_3D_points_after_triangulation;
//        robust_matcher_arg_struct.measurements = measurements;
//        robust_matcher_arg_struct.direction = direction;
//        robust_matcher_arg_struct.count_frames = count_frames;
//        robust_matcher_arg_struct.robust_done = false;
//
//        robust_thread = std::thread(
//                [&] { robust_matcher(&::robust_matcher_arg_struct); }
//        );

//        robust_thread.join();
//        direction = robust_matcher_arg_struct.direction;

    } catch (cv::Exception e) {
        std::cout << e.msg << std::endl;
    }
    return position_relative_T;
//    return direction;

//    if (count_frames == 1) {
//
//        fill_robust_matcher_arg_struct(current_frame_vis, count_frames);
//        robust_thread = std::thread(
//                [&] { robust_matcher(&::robust_matcher_arg_struct); }
//        );
//        robust_thread.join();
//        direction = robust_matcher_arg_struct.direction;
//        robust_last_current_frame = robust_matcher_arg_struct.current_frame.clone();
//        last_robust_id = robust_matcher_arg_struct.count_frames;
//
//    } else if (count_frames == 2) {
//        fill_robust_matcher_arg_struct(current_frame_vis, count_frames);
//        robust_thread = std::thread(
//                [&] { robust_matcher(&::robust_matcher_arg_struct); }
//        );
//
////        fill_fast_robust_matcher_arg_struct(current_frame_vis, direction, count_frames);
////        fast_robust_matcher(&fast_robust_matcher_arg_struct);
//
//        direction = fast_robust_matcher_arg_struct.direction;
//    } else if (robust_matcher_arg_struct.robust_done) {
//        fill_robust_matcher_arg_struct(current_frame_vis, count_frames);
//
//        robust_thread.join();
//        direction = robust_matcher_arg_struct.direction;
//        robust_last_current_frame = robust_matcher_arg_struct.current_frame.clone();
//        last_robust_id = robust_matcher_arg_struct.count_frames;
//
//        robust_thread = std::thread(
//                [&] { robust_matcher(&::robust_matcher_arg_struct); }
//        );
//
//    } else {
////        fill_fast_robust_matcher_arg_struct(current_frame_vis, direction, count_frames);
////        fast_robust_matcher(&fast_robust_matcher_arg_struct);
////
////        direction = fast_robust_matcher_arg_struct.direction;
//    }




    //    if (count_frames == 1) {
    //        try {
    //            getRobustEstimation(current_frame_vis, list_3D_points_after_triangulation,
    //                                measurements,
    //                                direction);
    //        } catch (cv::Exception e) {
    //            std::cout << e.msg << std::endl;
    //        }
    //    } else if (count_frames % 4 == 1) {
    //        robust_matcher_arg_struct.current_frame = current_frames[count_frames - 1];
    //        robust_matcher_arg_struct.direction = direction;
    //
    //
    //        pthread_create(&robust_matcher_t, NULL, robust_matcher,
    //                       (void *) &robust_matcher_arg_struct);
    //        //pthread_join(robust_matcher_t, NULL);
    //    } else if (count_frames % 2 == 0) {
    //        pthread_join(robust_matcher_t, NULL);
    //    }
    //
    //    if (count_frames >= 5) {
    //        fast_robust_matcher_arg_struct.last_current_frame = current_frames[start - 2];
    //        fast_robust_matcher_arg_struct.current_frame = current_frames[start - 1];
    //        fast_robust_matcher_arg_struct.direction = direction;
    //
    //        pthread_create(&fast_robust_matcher_t, NULL, fast_robust_matcher,
    //                       (void *) &fast_robust_matcher_arg_struct);
    //        pthread_join(fast_robust_matcher_t, NULL);
    //        start++;
    //
    //        if (start == end) {
    //            end += 3;
    //            start -= 3;
    //        }
    //    }

//    return direction;
}

void fill_robust_matcher_arg_struct(cv::Mat current_frame_vis, int count_frames) {
    robust_matcher_arg_struct.current_frame = current_frame_vis;
//    robust_matcher_arg_struct.list_3D_points = list_3D_points_after_triangulation;
//    robust_matcher_arg_struct.measurements = measurements;
    robust_matcher_arg_struct.direction = 0;
    robust_matcher_arg_struct.count_frames = count_frames;
    robust_matcher_arg_struct.robust_done = false;
}

void
fill_fast_robust_matcher_arg_struct(cv::Mat current_frame_vis, int direction, int count_frames) {

    fast_robust_matcher_arg_struct.last_current_frame = robust_last_current_frame;
    fast_robust_matcher_arg_struct.current_frame = current_frame_vis;
//    fast_robust_matcher_arg_struct.list_3D_points = list_3D_points_after_triangulation;
    fast_robust_matcher_arg_struct.direction = direction;
    fast_robust_matcher_arg_struct.count_frames = count_frames;
    fast_robust_matcher_arg_struct.robust_id = last_robust_id;
}


void *robust_matcher(void *arg) {
    struct robust_matcher_struct *param = (struct robust_matcher_struct *) arg;
    cv::Mat current_frame = param->current_frame;
    std::vector<cv::Point3f> list_3D_points_after_registration = param->list_3D_points;
    cv::Mat measurements = param->measurements;
    int direction = param->direction;
    int count_frames = param->count_frames;
    printf("+ %d started", count_frames);
    std::cout << "+ " << count_frames << " started" << std::endl;

    cv::Mat position_relative_T = cv::Mat(4, 4, CV_64F);
    getRobustEstimation(current_frame, list_3D_points_after_registration, measurements,
                        direction, position_relative_T);

    printf("+ %d ended, direction %d", count_frames, direction);
    std::cout << "+ " << count_frames << " ended" << std::endl;
    param->direction = direction;
    param->position_relative_T = position_relative_T;
    param->robust_done = true;
    return nullptr;
}

void *fast_robust_matcher(void *arg) {
    struct fast_robust_matcher_struct *param = (struct fast_robust_matcher_struct *) arg;
    cv::Mat last_current_frame = param->last_current_frame;
    cv::Mat current_frame = param->current_frame;
    std::vector<cv::Point3f> list_3D_points = param->list_3D_points;
    int direction = param->direction;
    int count_frames = param->count_frames;
    int robust_id = param->robust_id;
    printf("- %d started with %d", count_frames, robust_id);
    std::cout << "- " << count_frames << " started with " << robust_id << std::endl;

//    getLightweightEstimation(last_current_frame, list_3D_points, current_frame, direction);

    param->direction = direction;
    printf("- %d ended with %d, direction %d", count_frames, robust_id, direction);
    std::cout << "- " << count_frames << " ended   with " << robust_id << std::endl;
    return nullptr;
}


bool getRobustEstimation(cv::Mat current_frame_vis, std::vector<cv::Point3f> list_3D_points,
                         cv::Mat measurements, int &directory, cv::Mat &position_relative_T) {

//    TODO patřičné nalezení optickýho centeru
    double cx = current_frame_vis.cols / 2 - 0.5;
    double cy = current_frame_vis.rows / 2 - 0.5;

    cx = abs((int) cx);
    cy = abs((int) cy);

    pnp_detection.setOpticalCenter(cx, cy);
//     TODO end

    std::vector<cv::DMatch> good_matches;
    std::vector<cv::KeyPoint> key_points_current_frame;
    std::vector<cv::Point3f> list_points3d_model_match;
    std::vector<cv::Point2f> list_points2d_scene_match;

    cv::Mat relative_T = cv::Mat(4, 4, CV_64F);
//    todo nevrací to good matches
    robustMatcher.robustMatch(current_frame_vis, good_matches, key_points_current_frame);

    std::string gm = "train, query\n";

    for (unsigned int i = 0;
         i < good_matches.size();
         ++i) { // todo list_3D_points je menší než dává good_matchees

        // trainIdx patří k key_points_first_image proto převod pomocí tabulky vytvořené při triangulaci
        int index_in_3D_points = -1;
        auto it = find(key_points_first_image_convert_table_to_3d_points.begin(),
                       key_points_first_image_convert_table_to_3d_points.end(),
                       good_matches[i].queryIdx);
        if (it !=
            key_points_first_image_convert_table_to_3d_points.end()) { // If element was found
            // calculating the index_in_3D_points of good_matches[i].trainIdx
            index_in_3D_points =
                    it - key_points_first_image_convert_table_to_3d_points.begin();
        } else {
            continue; // If the element is not present in the vector
        }

//    for (unsigned int i = 0;
//         i < good_matches.size() &
//         good_matches[i].trainIdx < index_in_3D_points &
//         good_matches[i].queryIdx < key_points_current_frame.size();
//         ++i) {
        gm += std::to_string(index_in_3D_points) + "," +
              std::to_string(good_matches[i].trainIdx) + ",\n";

//    todo v tomhle bylo špatné pole
//      cv::Point3f point3d_model = list_3D_points[good_matches[i].trainIdx];
        cv::Point3f point3d_model = list_3D_points[index_in_3D_points];
        cv::Point2f point2d_scene = key_points_current_frame[good_matches[i].trainIdx].pt;
        list_points3d_model_match.push_back(point3d_model);
        list_points2d_scene_match.push_back(point2d_scene);
    }

    std::string csv_list_3D_points = generate_csv_from_Point3f(list_points3d_model_match);

    cv::Mat image = current_frame_vis.clone();
    draw2DPoints(image, list_points2d_scene_match, blue);
    saveMatToJpeg(image, "current_frame_vis", true);

//    cv::Mat undistorted;
//    cv::Mat dist_coeffs = cv::Mat::zeros(4, 1, CV_64FC1);
//    cv::undistort(current_frame_vis, undistorted, pnp_detection.getCameraMatrix(),
//                  dist_coeffs);
//    saveMatToJpeg(current_frame_vis, "current_frame_vis_undistorted", true);


    bool good_measurement = false;

    if (good_matches.size() > 0) {

//    https://chart-studio.plotly.com/create
        std::string csv_3D = generate_csv_from_Point3f(list_points3d_model_match);
        std::string csv_2D = generate_csv_from_Point2f(list_points2d_scene_match);


        pnp_detection.estimatePoseRANSAC(list_points3d_model_match,
                                         list_points2d_scene_match,
                                         pnp_method,
                                         useExtrinsicGuess, iterationsCount,
                                         reprojectionError,
                                         confidence);

        int inliers_size = pnp_detection.getInliersPoints().size();
//        if (inliers_size > minInliersKalman) {
        if (inliers_size > 1) {

            cv::Mat image2 = current_frame_vis.clone();
            draw2DPoints(image2, list_points2d_scene_match, red);
            saveMatToJpeg(image2, "current_frame_vis_list_points2d_scene_match", true);


//            cv::Mat image3 = current_frame_vis.clone();
//            cv::perspectiveTransform(image3, image3, pnp_detection.getProjectionMatrix());
//            saveMatToJpeg(image3, "current_warped_to_ref", true);
//
//            cv::Mat image4 = ref_image.clone();
//            cv::perspectiveTransform(image4, image4,
//                                     pnp_registration.getProjectionMatrix());
//            saveMatToJpeg(image4, "ref_warped_to_current", true);


            cv::Mat T = pnp_detection.getProjectionMatrix();
            cv::Mat reference_T = pnp_registration.getProjectionMatrix();
            relative_T = T.inv() * reference_T;

//            print_matrix("T", T);
//            print_matrix("reference_T", reference_T);
            print_matrix("relative_T", relative_T);
//
//            std::string csv_projection = generate_csv_from_Mat(reference_T);
//            std::string csv_projection_inv = generate_csv_from_Mat(reference_T);

//            double sequence[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
//            print_matrix("test sequence", cv::Mat(4, 4, CV_64FC1, sequence));
//
//            double rotational[] = {1, 2, 3, 4, 5, 6, 7, 8, 9};
//            const cv::Mat rotation_matrix = cv::Mat(3, 3, CV_64FC1, rotational);
//            double translation[] = {11, 22, 33};
//            const cv::Mat translation_matrix = cv::Mat(1, 3, CV_64FC1, translation);
//            pnp_detection.setProjectionMatrix(rotation_matrix, translation_matrix);
//            print_matrix("test projection",pnp_detection.getProjectionMatrix());

            double x = (double) relative_T.at<double>(0, 3);
            double y = (double) relative_T.at<double>(1, 3);
            double z = (double) relative_T.at<double>(2, 3);

            directory = getDirectory(x, y);
            position_relative_T = relative_T;
            printf("real direction: %d, x=%f, y=%f", directory, x, y);

            cv::Mat translation_measured(3, 1, CV_64F);
            translation_measured = pnp_detection.getTranslationMatrix();

            std::string csv_translation = generate_csv_from_Mat(translation_measured);

            cv::Mat rotation_measured(3, 3, CV_64F);
            rotation_measured = pnp_detection.getRotationMatrix();

            std::string csv_rotation = generate_csv_from_Mat(rotation_measured);

            good_measurement = true;
            fillMeasurements(measurements, translation_measured, rotation_measured);

            double pos = relative_T.at<double>(3, 0) + relative_T.at<double>(3, 1);
            pos /= 2;

            position.push_back(pos);
            frames.push_back(current_frame_vis);

//            std::cout << indexFrame << " Robustni odhad: " << std::endl;
//            std::cout << pnp_detection.getProjectionMatrix() << std::endl;

            /*std::cout << "Relativni rozdil: " << std::endl;
             std::cout << relative_T << std::endl;*/

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

bool
getLightweightEstimation(cv::Mat last_current_frame_vis,
                         std::vector<cv::Point3f> list_3D_points,
                         cv::Mat current_frame_vis, int &directory) {

    std::vector<cv::Point2f> list_points2d_scene_match;

    std::vector<cv::Point2f> featuresPrevious = pnp_detection.getInliersPoints();
    std::vector<cv::Point2f> featuresCurrent = pnp_detection.getInliersPoints();
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

        for (size_t i = 0; i < featuresNextPos.size(); i++) {
            if (featuresFound[i]) {
                list_points2d_scene_match.push_back(featuresNextPos[i]);
            }
        }
        if (list_3D_points.size() == list_points2d_scene_match.size()) {
            pnp_detection.estimatePoseRANSAC(list_3D_points, list_points2d_scene_match,
                                             pnp_method,
                                             useExtrinsicGuess,
                                             iterationsCount, reprojectionError,
                                             confidence);

            if (pnp_detection.getInliersPoints().size() > minInliersKalman) {

                draw2DPoints(current_frame_vis, list_points2d_scene_match, red);
                cv::Mat T = pnp_detection.getProjectionMatrix();
                cv::Mat refT = pnp_registration.getProjectionMatrix();

                revT = T.inv() * refT;

                int x = (int) revT.at<float>(0, 3);
                int y = (int) revT.at<float>(1, 3);

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
//    double x = (*mat).at<double>(3, 0);
//    double y = (*mat).at<double>(3, 1);
//    print_matrix("returned", returned);
//    print_matrix("copied", (*mat));
    return (jlong) mat;
}

cv::Mat loadImage(const std::string path_to_ref_image) {

    cv::Mat image = cv::imread(path_to_ref_image);

    if (!image.data) {
        std::cout << ERROR_READ_IMAGE << std::endl;
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

static void onMouseModelRegistration(int event, int x, int y, int, void *) {

    if (event == cv::EVENT_LBUTTONUP) {

        cv::Point2f point_2d = cv::Point2f((float) x, (float) y);
        bool is_registrable = registration.isRegistration();

        if (is_registrable) {
            registration.register2DPoint(point_2d);
            if (registration.getRegistrationCount() == registration.getRegistrationMax()) {
                end_registration = true;
            }
        }
    } else if (event == cv::EVENT_RBUTTONUP) {
        index_of_registration++;
    }
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
