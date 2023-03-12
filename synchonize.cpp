#include <atomic>
#include <chrono>
#include <iostream>
#include <thread>

struct robust_matcher_struct {
    cv::Mat current_frame;
    std::vector<cv::Point3f> list_3D_points;
    cv::Mat measurements;
    int direction;
    std::atomic<bool> robust_done;
    std::atomic<int> robust_id;
};

struct fast_robust_matcher_struct {
    cv::Mat last_current_frame;
    std::vector<cv::Point3f> list_3D_points;
    cv::Mat current_frame;
    int direction;
};

void *robust_matcher(void *arg) {
    struct robust_matcher_struct *param = (struct robust_matcher_struct *)arg;
    //    cv::Mat current_frame = param->current_frame;
    //    std::vector<cv::Point3f> list_3D_points_after_registration = param->list_3D_points;
    //    cv::Mat measurements = param->measurements;
    //    int directory = -1;
    //    getRobustEstimation(current_frame, list_3D_points_after_registration, measurements,
    //                        directory);

    using namespace std::chrono_literals;
    std::cout << "+ " << i << " started" << std::endl;
    std::this_thread::sleep_for(3s);
    std::cout << "+ " << i << " ended" << std::endl;
    param->robust_done = true;
    param->robust_id = i;
    return NULL;
}

void *fast_robust_matcher(void *arg) {
    //    struct fast_robust_matcher_struct *param = (struct fast_robust_matcher_struct *) arg;
    //    cv::Mat last_current_frame = param->last_current_frame;
    //    cv::Mat current_frame = param->current_frame;
    //    std::vector<cv::Point3f> list_3D_points = param->list_3D_points;
    //
    //    getLightweightEstimation(last_current_frame, list_3D_points, current_frame);
    std::cout << "- " << i << " started with " << robust_id << std::endl;
    std::this_thread::sleep_for(1s);
    std::cout << "- " << i << " ended   with " << robust_id << std::endl;
    return NULL;
}

int main() {
    for (int count_frames = 1; count_frames < 200; count_frames++) {
        if (count_frames == 1) {
            robust_done = false;
            robust_thread = std::thread(
                [&] { robust_matcher(&::robust_matcher_arg_struct); });
            robust_thread.join();
            robust_done = false;
            robust_thread = std::thread(
                [&] { robust_matcher(&::robust_matcher_arg_struct); });
        } else if (robust_done) {
            robust_done = false;
            robust_thread.join();
            robust_thread = std::thread(
                [&] { robust_matcher(&::robust_matcher_arg_struct); });
        } else {
            fast_robust_matcher(&fast_robust_matcher_arg_struct);
        }

        return 0;
    }
}