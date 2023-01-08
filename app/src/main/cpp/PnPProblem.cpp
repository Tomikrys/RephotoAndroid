/**
 * PnPProblem.cpp
 * Author: Adam Červenka <xcerve16@stud.fit.vutbr.cz>
 *
 */

#include <iostream>
#include <sstream>

#include "PnPProblem.h"

#include <opencv2/calib3d/calib3d.hpp>

#include <android/log.h>

class androidbuf : public std::streambuf {
public:
    enum {
        bufsize = 4096
    }; // ... or some other suitable buffer size
    androidbuf() { this->setp(buffer, buffer + bufsize - 1); }

private:
    int overflow(int c) {
        if (c == traits_type::eof()) {
            *this->pptr() = traits_type::to_char_type(c);
            this->sbumpc();
        }
        return this->sync() ? traits_type::eof() : traits_type::not_eof(c);
    }

    int sync() {
        int rc = 0;
        if (this->pbase() != this->pptr()) {
            char writebuf[bufsize + 1];
            memcpy(writebuf, this->pbase(), this->pptr() - this->pbase());
            writebuf[this->pptr() - this->pbase()] = '\0';

            rc = __android_log_write(ANDROID_LOG_INFO, "std", writebuf) > 0;
            this->setp(buffer, buffer + bufsize - 1);
        }
        return rc;
    }

    char buffer[bufsize];
};

void
PnPProblem::setProjectionMatrix(const cv::Mat &rotation_matrix, const cv::Mat &translation_matrix) {

    _projection_matrix.at<double>(0, 0) = rotation_matrix.at<double>(0, 0);
    _projection_matrix.at<double>(0, 1) = rotation_matrix.at<double>(0, 1);
    _projection_matrix.at<double>(0, 2) = rotation_matrix.at<double>(0, 2);
    _projection_matrix.at<double>(1, 0) = rotation_matrix.at<double>(1, 0);
    _projection_matrix.at<double>(1, 1) = rotation_matrix.at<double>(1, 1);
    _projection_matrix.at<double>(1, 2) = rotation_matrix.at<double>(1, 2);
    _projection_matrix.at<double>(2, 0) = rotation_matrix.at<double>(2, 0);
    _projection_matrix.at<double>(2, 1) = rotation_matrix.at<double>(2, 1);
    _projection_matrix.at<double>(2, 2) = rotation_matrix.at<double>(2, 2);
    _projection_matrix.at<double>(0, 3) = translation_matrix.at<double>(0);
    _projection_matrix.at<double>(1, 3) = translation_matrix.at<double>(1);
    _projection_matrix.at<double>(2, 3) = translation_matrix.at<double>(2);
    _projection_matrix.at<double>(3, 0) = 0;
    _projection_matrix.at<double>(3, 1) = 0;
    _projection_matrix.at<double>(3, 2) = 0;
    _projection_matrix.at<double>(3, 3) = 1;
}

std::string generate_csv_from_Point3fa(std::vector<cv::Point3f> list_3D_points) {
    std::string csv = "x,y,z,\n";
    for (int i = 0; i < list_3D_points.size(); i++) {
        csv += std::to_string(list_3D_points[i].x) + ","
               + std::to_string(list_3D_points[i].y) + ","
               + std::to_string(list_3D_points[i].z) + ",\n";
    }
    return csv;
}

std::string generate_csv_from_Point2fa(std::vector<cv::Point2f> list_2D_points) {
    std::string csv = "x,y,z,\n";
    for (int i = 0; i < list_2D_points.size(); i++) {
        csv += std::to_string(list_2D_points[i].x) + ","
               + std::to_string(list_2D_points[i].y) + ",0,\n";
    }
    return csv;
}

// Convert a vector of Point3f to a Mat
cv::Mat vectorToMat(const std::vector<cv::Point3f> &vec) {
    // Create a new matrix with the same size as the vector
    cv::Mat mat(vec.size(), 1, CV_32FC3);

    // Copy the data from the vector into the matrix
    for (int i = 0; i < vec.size(); i++) {
        mat.at<cv::Point3f>(i) = vec[i];
    }

    return mat;
}

int findIndex(const std::vector<cv::Point3f> &vec, const cv::Point3f &value) {
    // Use std::find to search for the value in the vector
    auto it = std::find(vec.begin(), vec.end(), value);

    // If the value was found, return its index
    if (it != vec.end()) {
        return std::distance(vec.begin(), it);
    }

    // Otherwise, return -1 to indicate that the value was not found
    return -1;
}

void PnPProblem::estimatePoseRANSAC(const std::vector<cv::Point3f> &list_3d_points,
                                    const std::vector<cv::Point2f> &list_2d_points, int flags,
                                    bool use_extrinsic_guess,
                                    int iterations_count, float reprojection_error,
                                    double confidence) {

    cv::Mat rvec = cv::Mat::zeros(3, 1, CV_64FC1);
    cv::Mat tvec = cv::Mat::zeros(3, 1, CV_64FC1);
    cv::Mat dist_coeffs = cv::Mat::zeros(4, 1, CV_64FC1);
    cv::Mat inliers_points;
//    https://chart-studio.plotly.com/create
    std::string csv_3D = generate_csv_from_Point3fa(list_3d_points);
    std::string csv_2D = generate_csv_from_Point2fa(list_2d_points);

//    cv::Mat mat_3d_points = vectorToMat(list_3d_points);
//    cv::Mat labels;
//    cv::Mat centers;
//    int numClusters = 3;
//
//    kmeans(mat_3d_points, numClusters, labels,
//           cv::TermCriteria(cv::TermCriteria::EPS + cv::TermCriteria::COUNT, 10, 1.0),
//           3, cv::KMEANS_PP_CENTERS, centers);
//
//    std::vector<cv::Point3f> representative3DPoints;
//    std::vector<cv::Point2f> representative2DPoints;
//    for (int i = 0; i < numClusters; i++) {
//        // Find all points in the current cluster
//        std::vector<cv::Point3f> clusterPoints;
//        for (int j = 0; j < mat_3d_points.rows; j++) {
//            if (labels.at<int>(j) == i) {
//                clusterPoints.push_back(mat_3d_points.at<cv::Point3f>(j));
//            }
//        }
//
//        // Choose a random point from the cluster
//        int randomIndex = rand() % clusterPoints.size();
//
//        // Find index of 3d point in original array
//        int index_of_coresponding_2d_point = findIndex(list_3d_points, clusterPoints[randomIndex]);
//
//        // Push borh 3D point and corresponding 2d point
//        if (index_of_coresponding_2d_point >= 0) {
//            representative3DPoints.push_back(clusterPoints[randomIndex]);
//            representative2DPoints.push_back(list_2d_points[index_of_coresponding_2d_point]);
//        }
//    }
//
//    std::string representative_3d_csv = generate_csv_from_Point3fa(representative3DPoints);
//    std::string representative_2d_csv = generate_csv_from_Point2fa(representative2DPoints);

    if (cv::solvePnPRansac(list_3d_points, list_2d_points, _camera_matrix,
                           dist_coeffs, rvec, tvec, use_extrinsic_guess, iterations_count,
                           reprojection_error, confidence, inliers_points, flags)) {
        std::string x = "found solution";
    } else {
        std::string y = "didnt find solution";
    }

    Rodrigues(rvec, _rotation_matrix);
    _translation_matrix = tvec;

    cv::projectPoints(list_3d_points, rvec, tvec, _camera_matrix, dist_coeffs, list_2d_points);
    std::string csv_3D_projected_to_2D = generate_csv_from_Point2fa(list_2d_points);

    //        TODO empty
    if (!inliers_points.empty()) {
        _inliers_points.clear();

        for (int index = 0; index < inliers_points.rows; ++index) {
            int n = inliers_points.at<int>(index);
            cv::Point2f point2d = list_2d_points[n];
            _inliers_points.push_back(point2d);
        }
    }

    this->setProjectionMatrix(_rotation_matrix, _translation_matrix);
}

void PnPProblem::setOpticalCenter(double center_x, double center_y) {
    _camera_matrix.at<double>(0, 2) = center_x;
    _camera_matrix.at<double>(1, 2) = center_y;
}

void
PnPProblem::setCameraParameter(double center_x, double center_y, double focal_x, double focal_y) {
    _camera_matrix.at<double>(0, 2) = center_x;
    _camera_matrix.at<double>(1, 2) = center_y;
    _camera_matrix.at<double>(0, 0) = focal_x;
    _camera_matrix.at<double>(1, 1) = focal_y;
    _camera_matrix.at<double>(2, 2) = 1; // TODO neni v originale
}
