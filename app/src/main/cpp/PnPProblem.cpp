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
class androidbuf : public std::streambuf
{
public:
    enum
    {
        bufsize = 4096
    }; // ... or some other suitable buffer size
    androidbuf() { this->setp(buffer, buffer + bufsize - 1); }

private:
    int overflow(int c)
    {
        if (c == traits_type::eof())
        {
            *this->pptr() = traits_type::to_char_type(c);
            this->sbumpc();
        }
        return this->sync() ? traits_type::eof() : traits_type::not_eof(c);
    }

    int sync()
    {
        int rc = 0;
        if (this->pbase() != this->pptr())
        {
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

void PnPProblem::setProjectionMatrix(const cv::Mat &rotation_matrix, const cv::Mat &translation_matrix)
{

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

void PnPProblem::estimatePoseRANSAC(const std::vector<cv::Point3f> &list_3d_points,
                                    const std::vector<cv::Point2f> &list_2d_points, int flags, bool use_extrinsic_guess,
                                    int iterations_count, float reprojection_error, double confidence)
{

    cv::Mat rvec = cv::Mat::zeros(3, 1, CV_64FC1);
    cv::Mat tvec = cv::Mat::zeros(3, 1, CV_64FC1);
    cv::Mat dist_coeffs = cv::Mat::zeros(4, 1, CV_64FC1);
    cv::Mat inliers_points;

    cv::solvePnPRansac(list_3d_points, list_2d_points, _camera_matrix,
                       dist_coeffs, rvec, tvec, use_extrinsic_guess, iterations_count,
                       reprojection_error, confidence, inliers_points, flags);

    Rodrigues(rvec, _rotation_matrix);
    _translation_matrix = tvec;

    std::cout.rdbuf(new androidbuf);
    std::cout << "_rotation_matrix " << _rotation_matrix << std::endl;
    std::cout << "_translation_matrix " << _translation_matrix << std::endl;
    std::cout << "inliers_points " << inliers_points << std::endl;

    //        TODO empty
    if (!inliers_points.empty())
    {
        _inliers_points.clear();

        for (int index = 0; index < inliers_points.rows; ++index)
        {
            int n = inliers_points.at<int>(index);
            cv::Point2f point2d = list_2d_points[n];
            _inliers_points.push_back(point2d);
        }
    }

    this->setProjectionMatrix(_rotation_matrix, _translation_matrix);
}

void PnPProblem::setOpticalCenter(double center_x, double center_y)
{
    _camera_matrix.at<double>(0, 2) = center_x;
    _camera_matrix.at<double>(1, 2) = center_y;
}

void PnPProblem::setCameraParameter(double center_x, double center_y, double focal_x, double focal_y)
{
    _camera_matrix.at<double>(0, 2) = center_x;
    _camera_matrix.at<double>(1, 2) = center_y;
    _camera_matrix.at<double>(0, 0) = focal_x;
    _camera_matrix.at<double>(1, 1) = focal_y;
    _camera_matrix.at<double>(2, 2) = 1; // TODO neni v originale
}
