cmake_minimum_required(VERSION 3.17)

project(OpenSesameNatives
    LANGUAGES CXX C)

add_custom_command(
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/share/native/include"
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/unix/native/include"
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/windows/native/include"
    COMMAND bash -c "mkdir -p \"${CMAKE_CURRENT_BINARY_DIR}/jni_headers\""
    COMMAND bash -c "git -C \"${CMAKE_CURRENT_BINARY_DIR}/jni_headers\" init"
    COMMAND bash -c "git -C \"${CMAKE_CURRENT_BINARY_DIR}/jni_headers\" remote add origin https://github.com/openjdk/jdk.git"
    COMMAND bash -c "git -C \"${CMAKE_CURRENT_BINARY_DIR}/jni_headers\" sparse-checkout set src/java.base/share/native/include src/java.base/unix/native/include src/java.base/windows/native/include"
    COMMAND bash -c "git -C \"${CMAKE_CURRENT_BINARY_DIR}/jni_headers\" fetch --depth=1 --filter=blob:none origin dfacda488bfbe2e11e8d607a6d08527710286982"
    COMMAND bash -c "git -C \"${CMAKE_CURRENT_BINARY_DIR}/jni_headers\" checkout dfacda488bfbe2e11e8d607a6d08527710286982"
    VERBATIM
)

add_custom_target(fetch_jni_headers
    DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/share/native/include"
    DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/unix/native/include"
    DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/windows/native/include")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -nostdlib")

add_library(opensesamenative SHARED
    src/cpp/opensesamenative.cpp)
add_dependencies(opensesamenative fetch_jni_headers)

target_include_directories(opensesamenative PUBLIC
    "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/share/native/include")
if(CMAKE_SYSTEM_NAME STREQUAL "Linux" OR CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    target_include_directories(opensesamenative PUBLIC
        "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/unix/native/include")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
    target_include_directories(opensesamenative PUBLIC
        "${CMAKE_CURRENT_BINARY_DIR}/jni_headers/src/java.base/windows/native/include")
endif()