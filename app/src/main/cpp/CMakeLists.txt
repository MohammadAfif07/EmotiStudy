cmake_minimum_required(VERSION 3.10.2)
project("opensmile-jni")

add_library(opensmile-jni SHARED opensmile_jni.cpp)

# Link to openSMILE binary (.so)
add_library(opensmile-lib SHARED IMPORTED)
set_target_properties(opensmile-lib PROPERTIES
        IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/armeabi-v7a/libopensmile.so
)

target_include_directories(opensmile-jni PRIVATE
        ${CMAKE_SOURCE_DIR}/include
)

target_link_libraries(opensmile-jni opensmile-lib)
