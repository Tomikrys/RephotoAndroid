LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES := on
OPENCV_LIB_TYPE:=SHARED
include D:/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := OpenCVNative.h OpenCVNative.cpp CameraCalibrator.cpp CameraCalibrator.h errorNIETO.cpp errorNIETO.h Line.cpp Line.h lmmin.cpp lmmin.h Main.cpp Main.h ModelRegistration.cpp ModelRegistratotion.h MSAC.cpp MSAC.h PnPProblem.cpp PnPProblem.h RobustMatcher.cpp RobustMatcher.h Utils.cpp Utils.h
LOCAL_MODULE    := native-lib
LOCAL_LDLIBS +=  -llog -ldl -ffast-math
LOCAL_LDFLAGS := -Wl,--allow-multiple-definition

include $(BUILD_SHARED_LIBRARY)