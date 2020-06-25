cmake_minimum_required(VERSION 3.4.1)
set(TARGET pytorch_nativeapp)
project(${TARGET} CXX)
set(CMAKE_CXX_STANDARD 14)

set(build_DIR ${CMAKE_SOURCE_DIR}/build)

set(pytorch_testapp_cpp_DIR ${CMAKE_CURRENT_LIST_DIR}/src/main/cpp)
file(GLOB pytorch_testapp_SOURCES
  ${pytorch_testapp_cpp_DIR}/pytorch_nativeapp.cpp
)

add_library(${TARGET} SHARED
    ${pytorch_testapp_SOURCES}
)

file(GLOB PYTORCH_INCLUDE_DIRS "${build_DIR}/pytorch_android*.aar/headers")
file(GLOB PYTORCH_LINK_DIRS "${build_DIR}/pytorch_android*.aar/jni/${ANDROID_ABI}")

target_compile_options(${TARGET} PRIVATE
  -fexceptions
)

set(BUILD_SUBDIR ${ANDROID_ABI})

find_library(PYTORCH_LIBRARY pytorch_jni
  PATHS ${PYTORCH_LINK_DIRS}
  NO_CMAKE_FIND_ROOT_PATH)
find_library(FBJNI_LIBRARY fbjni
  PATHS ${PYTORCH_LINK_DIRS}
  NO_CMAKE_FIND_ROOT_PATH)

# OpenCV
if(NOT DEFINED ENV{OPENCV_ANDROID_SDK})
  message(FATAL_ERROR "Environment var OPENCV_ANDROID_SDK set")
endif()

set(OPENCV_INCLUDE_DIR "$ENV{OPENCV_ANDROID_SDK}/sdk/native/jni/include")

target_include_directories(${TARGET} PRIVATE
 "${OPENCV_INCLUDE_DIR}"
  ${PYTORCH_INCLUDE_DIRS})

set(OPENCV_LIB_DIR "$ENV{OPENCV_ANDROID_SDK}/sdk/native/libs/${ANDROID_ABI}")

find_library(OPENCV_LIBRARY opencv_java4
  PATHS ${OPENCV_LIB_DIR}
  NO_CMAKE_FIND_ROOT_PATH)

target_link_libraries(${TARGET}
  ${PYTORCH_LIBRARY}
  ${FBJNI_LIBRARY}
  ${OPENCV_LIBRARY}
  log)
