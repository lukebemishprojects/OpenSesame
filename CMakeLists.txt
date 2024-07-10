cmake_minimum_required(VERSION 3.17)

project(OpenSesameNatives
    LANGUAGES CXX C)

find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})
add_library(opensesamenative SHARED
    src/cpp/opensesamenative.cpp)