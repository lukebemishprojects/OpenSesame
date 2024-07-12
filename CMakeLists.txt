cmake_minimum_required(VERSION 3.17)

project(OpenSesameNatives
    LANGUAGES CXX C)

find_package(JNI REQUIRED)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -nostdlib")

include_directories(${JNI_INCLUDE_DIRS})
add_library(opensesamenative SHARED
    src/cpp/opensesamenative.cpp)