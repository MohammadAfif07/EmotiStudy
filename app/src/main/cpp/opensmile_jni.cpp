#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_afif_emotistudy_NativeBridge_dummyFunction(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("Native bridge working");
}
