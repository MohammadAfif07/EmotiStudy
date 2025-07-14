cmake_minimum_required(VERSION 3.4.1)

add_library(opensmile_jni SHARED
        opensmile_jni.cpp
)

add_library(opensmile SHARED IMPORTED)
set_target_properties(opensmile PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/armeabi-v7a/libopensmile.so)

target_link_libraries(opensmile_jni opensmile log)
