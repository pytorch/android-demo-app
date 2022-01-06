cmake_minimum_required(VERSION 3.4.1)

project(StreamingASR)

#set(LOGGER_BUILD_HEADER_LIB ON)
#add_subdirectory(external/logger)
#set(BUILD_TESTING OFF)
add_subdirectory(external/eigen)
add_subdirectory(StreamingASR/app/src/main/cpp)
