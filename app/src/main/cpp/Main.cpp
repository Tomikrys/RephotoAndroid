/**
 * Main.cpp
 * Author: Adam Červenka <xcerve16@stud.fit.vutbr.cz>
 *
 */

#include <opencv2/imgproc/types_c.h>
#include "Main.h"

#include <android/bitmap.h>
#include <android/log.h>
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "MAIN", __VA_ARGS__);

class androidbuf : public std::streambuf {
public:
    enum { bufsize = 4096 }; // ... or some other suitable buffer size
    androidbuf() { this->setp(buffer, buffer + bufsize - 1); }

private:
    int overflow(int c)
    {
        if (c == traits_type::eof()) {
            *this->pptr() = traits_type::to_char_type(c);
            this->sbumpc();
        }
        return this->sync()? traits_type::eof(): traits_type::not_eof(c);
    }

    int sync()
    {
        int rc = 0;
        if (this->pbase() != this->pptr()) {
            char writebuf[bufsize+1];
            memcpy(writebuf, this->pbase(), this->pptr() - this->pbase());
            writebuf[this->pptr() - this->pbase()] = '\0';

            rc = __android_log_write(ANDROID_LOG_INFO, "std", writebuf) > 0;
            this->setp(buffer, buffer + bufsize - 1);
        }
        return rc;
    }

    char buffer[bufsize];
};

void *fast_robust_matcher(void *arg) {
    struct fast_robust_matcher_struct *param = (struct fast_robust_matcher_struct *) arg;
    cv::Mat last_current_frame = param->last_current_frame;
    cv::Mat current_frame = param->current_frame;
    std::vector<cv::Point3f> list_3D_points = param->list_3D_points;
    int directory = param->directory;

    getLightweightEstimation(last_current_frame, list_3D_points, current_frame, directory);
    return NULL;
}


void *robust_matcher(void *arg) {
    struct robust_matcher_struct *param = (struct robust_matcher_struct *) arg;
    cv::Mat current_frame = param->current_frame;
    std::vector<cv::Point3f> list_3D_points_after_registration = param->list_3D_points;
    cv::Mat measurements = param->measurements;
    int directory = param->directory;


    getRobustEstimation(current_frame, list_3D_points_after_registration, measurements, directory);
    return NULL;
}

void
Main::initReconstruction(cv::Mat image1, cv::Mat image2, cv::Mat ref, cv::Point2f cf, cv::Point2f ff, cv::Point2f cc,
                         cv::Point2f fc) {
    first_image = image1;
    second_image = image2;
    ref_image = ref;

    pnp_registration.setCameraParameter(cf.x, cf.y, ff.x, ff.y);
    pnp_detection.setCameraParameter(cc.x, cc.y, fc.x, fc.y);

}

void Main::printMat (cv::Mat mat) {
    for(int i=0; i<mat.rows; i++) {
        for (int j = 0; j < mat.cols; j++) {
            // You can now access the pixel value with cv::Vec3b
            printf("%4.2f ", mat.at<float>(i, j));
        }
        printf("\n")
    }
}

cv::Point2f Main::processReconstruction() {

    /**
     * Robust matcher
     * Dle tutorialu dostupneho na https://www.packtpub.com/books/content/learn-computer-vision-applications-open-cv
     * Dle tutorialu dostupneho na http://docs.opencv.org/3.1.0/dc/d2c/tutorial_real_time_pose.html
     */

    cv::Ptr<cv::ORB> featureDetector = cv::ORB::create(numKeyPoints);
    cv::Ptr<cv::ORB> featureExtractor = cv::ORB::create();

    std::vector<cv::KeyPoint> key_points_first_image, key_points_second_image;
    std::vector<cv::Point2f> detection_points_second_image;
    std::vector<cv::DMatch> matches;

    robustMatcher.setConfidenceLevel(confidenceLevel);
    robustMatcher.setMinDistanceToEpipolar(max_distance);
    robustMatcher.setRatio(ratioTest);
    robustMatcher.setFeatureDetector(featureDetector);
    robustMatcher.setDescriptorExtractor(featureExtractor);

    cv::Mat camera_matrix = pnp_registration.getCameraMatrix();

    cv::Mat essential_matrix = robustMatcher.robustMatchRANSAC(first_image, second_image, matches,
                                                               key_points_first_image, key_points_second_image,
                                                               camera_matrix);

    /**
     * Rozklad matic
     */

    for (std::vector<cv::DMatch>::const_iterator it = matches.begin(); it != matches.end(); ++it) {
        float x = key_points_first_image[it->queryIdx].pt.x;
        float y = key_points_first_image[it->queryIdx].pt.y;
        detection_points_first_image.push_back(cv::Point2f(x, y));

        x = key_points_second_image[it->trainIdx].pt.x;
        y = key_points_second_image[it->trainIdx].pt.y;
        detection_points_second_image.push_back(cv::Point2f(x, y));

    }

    cv::Mat R, t;
    cv::Mat points1 = cv::Mat(detection_points_first_image);
    cv::Mat points2 = cv::Mat(detection_points_second_image);
    cv::recoverPose(essential_matrix, points1, points2, camera_matrix, R, t);

    /**
     * Triangulace
     */

    cv::Mat rotation_translation_matrix_first_image, rotation_translation_matrix_second_image, result_3D_points,
            rotation_vector_first_image, translation_vector_first_image, found_3D_points, result_matrix;


    rotation_translation_matrix_second_image = cv::Mat::eye(3, 4, CV_64FC1);
    rotation_translation_matrix_first_image = cv::Mat::eye(3, 4, CV_64FC1);


    hconcat(R, t, rotation_translation_matrix_first_image);
    cv::Mat triangulation_3D_points, camera_matrix_a, camera_matrix_b;

    camera_matrix_a = camera_matrix * rotation_translation_matrix_first_image;
    camera_matrix_b = camera_matrix * rotation_translation_matrix_second_image;

    std::cout.rdbuf(new androidbuf);

    std::cout << "##### Triangulace #####" << std::endl;
    std::cout << "points1 " << points1 << std::endl;
    std::cout << "points2 " << points2 << std::endl;
    std::cout << "camera_matrix_b " << camera_matrix_b << std::endl;
    std::cout << "camera_matrix_a " << camera_matrix_a << std::endl;

    triangulatePoints(camera_matrix_b, camera_matrix_a, points1, points2, found_3D_points);
    std::cout << found_3D_points << std::endl;

    transpose(found_3D_points, triangulation_3D_points);
    convertPointsFromHomogeneous(triangulation_3D_points, triangulation_3D_points);

    std::vector<cv::Point2f> list_2D_points_after_triangulation;
    for (int i = 0; i < triangulation_3D_points.rows; i++) {
        list_3D_points.push_back(
                cv::Point3f(triangulation_3D_points.at<float>(i, 0), triangulation_3D_points.at<float>(i, 1),
                            triangulation_3D_points.at<float>(i, 2)));
    }

    /**
     * Registrace korespondencnich bodu
     * Dle tutorialu dostupneho na http://docs.opencv.org/3.1.0/dc/d2c/tutorial_real_time_pose.html
     */
     for (int i = 0; i < list_3D_points.size(); i++) {
        std::cout << list_3D_points[i] << std::endl;
    }
    registration.setRegistrationMax(number_registration);

    float pos_x = detection_points_first_image[0].x;
    float pos_y = detection_points_first_image[0].y;

    cv::Point2f point = cv::Point2f(pos_x, pos_y);

    return point;
}

cv::Point2f Main::registrationPoints(float x, float y) {
    cv::Point2f point_2d = cv::Point2f(x, y);
    bool is_registrable = registration.isRegistration();
    int index = registration.getIndexRegistration();

    if (is_registrable) {
        registration.register2DPoint(point_2d);
        cv::Point3f point3f = list_3D_points[index];
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

cv::Point2f Main::nextPoint() {

    registration.incRegistrationIndex();

    cv::Mat clone_frame_with_triangulation = first_image.clone();
    int index = registration.getIndexRegistration();

    float x = detection_points_first_image[index].x;
    float y = detection_points_first_image[index].y;

    cv::Point2f point = cv::Point2f(x, y);

    return point;
}

void saveMatToJpeg(cv::Mat img, std::string filename, bool RGBA2BGRA = false) {
    if (RGBA2BGRA) {
        cv::cvtColor(img, img,  cv::COLOR_RGBA2BGRA);
    }
    cv::imwrite("/storage/emulated/0/Download/" + filename + ".jpg", img, {cv::ImwriteFlags::IMWRITE_JPEG_QUALITY, 70});
}

void Main::initNavigation() {

    start = 4;
    end = 9;

    std::vector<cv::Point3f> list_3D_points = registration.getList3DPoints();
    std::vector<cv::Point2f> list_2D_points = registration.getList2DPoints();

    std::vector<cv::Mat> vanish_point;
    cv::Mat output_ref_image, gray_ref_image;
    cv::Size size_ref_image;
    double cx, cy;

    size_ref_image = cv::Size(ref_image.cols, ref_image.rows);

    msac.init(mode, size_ref_image, verbose);
    resize(ref_image, ref_image, size_ref_image);

    if (ref_image.channels() >= 3) {
        cv::cvtColor(ref_image, gray_ref_image, CV_BGR2GRAY);
        ref_image.copyTo(output_ref_image);
    } else {
        ref_image.copyTo(gray_ref_image);
        cv::cvtColor(ref_image, output_ref_image, CV_GRAY2BGR);
    }

//    TODO Debug Mat to jspeg file
    saveMatToJpeg(output_ref_image, "output_ref_image");

    vanish_point = processImage(msac, numVps, gray_ref_image, output_ref_image);

    std::vector<cv::Point2f> list_vanish_points;
    cv::Point2f center;
    for (int i = 0; i < vanish_point.size(); i++) {
        double x = vanish_point[i].at<float>(0, 0);
        double y = vanish_point[i].at<float>(1, 0);
        if (x < ref_image.cols && y < ref_image.rows)
            list_vanish_points.push_back(cv::Point2f(vanish_point[i].at<float>(0, 0), vanish_point[i].at<float>(1, 0)));
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

//    cv::waitKey(0);
    pnp_registration.setOpticalCenter(cx, cy);

    /**
     * Pozice historicke kamery
     */
    cv::Mat inliers_ref_frame;
    std::vector<cv::Point2f> list_points2d_scene_match;

    pnp_registration.estimatePoseRANSAC(list_3D_points, list_2D_points, pnp_method, useExtrinsicGuess, iterationsCount,
                                        reprojectionError, confidence);

    /**
     * Robust matcher
     */

    cv::Mat current_frame, current_frame_vis, last_current_frame_vis;

    cv::Ptr<cv::DescriptorMatcher> descriptorMatcher = cv::makePtr<cv::FlannBasedMatcher>();
    robustMatcher.setDescriptorMatcher(descriptorMatcher);
    robustMatcher.setRatio(ratioTest);


    /**
     * Real-time Camera Pose Estimation
     */

    measurements = cv::Mat(nMeasurements, 1, CV_64F);
    measurements.setTo(cv::Scalar(0));

    initKalmanFilter(kalmanFilter, nStates, nMeasurements, nInputs, dt);

    robust_matcher_arg_struct.list_3D_points = list_3D_points;
    robust_matcher_arg_struct.measurements = measurements;

    fast_robust_matcher_arg_struct.list_3D_points = list_3D_points;
    std::cout << "list_3D_points" << std::endl;
    std::cout << list_3D_points << std::endl;
}

int Main::processNavigation(cv::Mat current_frame, int count_frames) {

    cv::Mat last_current_frame_vis = current_frame.clone();
    cv::Mat current_frame_vis = current_frame.clone();
    current_frames.push_back(current_frame_vis);

    int directory = 0;

    if (count_frames == 1) {
        try {
            getRobustEstimation(current_frame_vis, registration.getList3DPoints(), measurements, directory);
        } catch (cv::Exception e) {
            std::cout << e.msg << std::endl;
        }
    } else if (count_frames % 4 == 1) {
        robust_matcher_arg_struct.current_frame = current_frames[count_frames - 1];
        robust_matcher_arg_struct.directory = directory;


        pthread_create(&robust_matcher_t, NULL, robust_matcher, (void *) &robust_matcher_arg_struct);
        //pthread_join(robust_matcher_t, NULL);
    } else if (count_frames % 2 == 0) {
        pthread_join(robust_matcher_t, NULL);
    }

    if (count_frames >= 5) {
        fast_robust_matcher_arg_struct.last_current_frame = current_frames[start - 2];
        fast_robust_matcher_arg_struct.current_frame = current_frames[start - 1];
        fast_robust_matcher_arg_struct.directory = directory;

        pthread_create(&fast_robust_matcher_t, NULL, fast_robust_matcher, (void *) &fast_robust_matcher_arg_struct);
        pthread_join(fast_robust_matcher_t, NULL);
        start++;

        if (start == end) {
            end += 3;
            start -= 3;
        }
    }
    return directory;
}

bool getRobustEstimation(cv::Mat current_frame_vis, std::vector<cv::Point3f> list_3D_points,
                         cv::Mat measurements, int &directory) {

    std::vector<cv::DMatch> good_matches;
    std::vector<cv::KeyPoint> key_points_current_frame;
    std::vector<cv::Point3f> list_points3d_model_match;
    std::vector<cv::Point2f> list_points2d_scene_match;

    cv::Mat revT = cv::Mat(4, 4, CV_64F);
    robustMatcher.robustMatch(current_frame_vis, good_matches, key_points_current_frame);
    for (unsigned int i = 0; i < good_matches.size(); ++i) {
        cv::Point3f point3d_model = list_3D_points[good_matches[i].trainIdx];
        cv::Point2f point2d_scene = key_points_current_frame[good_matches[i].queryIdx].pt;
        list_points3d_model_match.push_back(point3d_model);
        list_points2d_scene_match.push_back(point2d_scene);
    }


    draw2DPoints(current_frame_vis, list_points2d_scene_match, blue);
    saveMatToJpeg(current_frame_vis, "getRobustEstimation", true);

    bool good_measurement = false;

    if (good_matches.size() > 0) {

        pnp_detection.estimatePoseRANSAC(list_points3d_model_match, list_points2d_scene_match, pnp_method,
                                         useExtrinsicGuess, iterationsCount, reprojectionError, confidence);

//        todo konzultace
//        todo false pnp_detection.getInliersPoints().size() == 0
        if (pnp_detection.getInliersPoints().size() > minInliersKalman) {


            draw2DPoints(current_frame_vis, list_points2d_scene_match, red);
            saveMatToJpeg(current_frame_vis, "getRobustEstimation2", true);
            cv::Mat T = pnp_detection.getProjectionMatrix();
            cv::Mat refT = pnp_registration.getProjectionMatrix();

            revT = T.inv() * refT;

            int x = (int) revT.at<float>(0, 3);
            int y = (int) revT.at<float>(1, 3);

            directory = getDirectory(x, y);

            cv::Mat translation_measured(3, 1, CV_64F);
            translation_measured = pnp_detection.getTranslationMatrix();

            cv::Mat rotation_measured(3, 3, CV_64F);
            rotation_measured = pnp_detection.getRotationMatrix();

            good_measurement = true;
            fillMeasurements(measurements, translation_measured, rotation_measured);
        }
    }

    cv::Mat translation_estimated(3, 1, CV_64F);
    cv::Mat rotation_estimated(3, 3, CV_64F);

    updateKalmanFilter(kalmanFilter, measurements, translation_estimated, rotation_estimated);
    pnp_detection.setProjectionMatrix(rotation_estimated, translation_estimated);


    return good_measurement;
}

bool getLightweightEstimation(cv::Mat last_current_frame_vis, std::vector<cv::Point3f> list_3D_points,
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

        calcOpticalFlowPyrLK(last, current, featuresPrevious, featuresNextPos, featuresFound, err);

        for (size_t i = 0; i < featuresNextPos.size(); i++) {
            if (featuresFound[i]) {
                list_points2d_scene_match.push_back(featuresNextPos[i]);
            }
        }
        std::cout << list_3D_points.size() << std::endl;
        if (list_3D_points.size() == list_points2d_scene_match.size()) {
            pnp_detection.estimatePoseRANSAC(list_3D_points, list_points2d_scene_match, pnp_method, useExtrinsicGuess,
                                             iterationsCount, reprojectionError, confidence);


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

int getDirectory(int x, int y) {
    int directory;
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


std::vector<cv::Mat> Main::processImage(MSAC &msac, int numVps, cv::Mat &imgGRAY, cv::Mat &outputImg) {
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

#endif

    std::vector<cv::Mat> vps;
    std::vector<std::vector<int> > CS;
    std::vector<int> numInliers;

    std::vector<std::vector<std::vector<cv::Point> > > lineSegmentsClusters;

    msac.multipleVPEstimation(lineSegments, lineSegmentsClusters, numInliers, vps, numVps);
    for (int v = 0; v < vps.size(); v++) {
        double vpNorm = cv::norm(vps[v]);
        if (fabs(vpNorm - 1) < 0.001) {
        }
    }

    msac.drawCS(outputImg, lineSegmentsClusters, vps);
    saveMatToJpeg(outputImg, "outputImgMSAC", true);

    return vps;
}


/**********************************************************************************************************/
void Main::initKalmanFilter(cv::KalmanFilter &KF, int nStates, int nMeasurements, int nInputs, double dt) {

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

void updateKalmanFilter(cv::KalmanFilter &KF, cv::Mat &measurement, cv::Mat &translation_estimated,
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

void fillMeasurements(cv::Mat &measurements, const cv::Mat &translation_measured, const cv::Mat &rotation_measured) {

    cv::Mat measured_eulers(3, 1, CV_64F);
    measured_eulers = rot2euler(rotation_measured);

    measurements.at<double>(0) = translation_measured.at<double>(0); // x
    measurements.at<double>(1) = translation_measured.at<double>(1); // y
    measurements.at<double>(2) = translation_measured.at<double>(2); // z
    measurements.at<double>(3) = measured_eulers.at<double>(0);      // roll
    measurements.at<double>(4) = measured_eulers.at<double>(1);      // pitch
    measurements.at<double>(5) = measured_eulers.at<double>(2);      // yaw
}


int main(int argc, char *argv[]) {
    Main process;

    cv::Mat fist_frame = cv::imread(path_to_first_image);
    cv::Mat second_frame = cv::imread(path_to_second_image);
    cv::Mat ref_frame = cv::imread(path_to_ref_image);

    process.initReconstruction(fist_frame, second_frame, ref_frame, cv::Point2f(273, 339), cv::Point2f(585, 548),
                               cv::Point2f(341, 172), cv::Point2f(2040, 1548));
    process.processReconstruction();
    process.initNavigation();

    /*int directory = process.processNavigation(third_frame, 1);
    std::cout << directory << std::endl;*/
    return 0;
}