cmake_minimum_required(VERSION 3.4.1)
project(CTest LANGUAGES C CXX)

add_library( MainActivityJNI SHARED MainActivityJNI.cpp )
target_link_libraries( MainActivityJNI Eigen3::Eigen)
