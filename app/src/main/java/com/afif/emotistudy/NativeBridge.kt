package com.afif.emotistudy

object NativeBridge {
    init {
        System.loadLibrary("opensmile-jni")
    }

    external fun dummyFunction(): String
}
