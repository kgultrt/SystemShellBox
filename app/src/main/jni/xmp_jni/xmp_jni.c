/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <xmp.h>          // xmp 的主头文件，已经在 xmp/include 下

#define LOG_TAG "xmp_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static xmp_context ctx = NULL;   // 全局 xmp 上下文，演示用，实际最好封装成对象

// 初始化 xmp，并加载一个模块文件
JNIEXPORT jboolean JNICALL
Java_com_manager_nativelayer_XmpPlayer_nativeInit(JNIEnv *env, jobject thiz,
                                              jstring filePath) {
    const char *path = (*env)->GetStringUTFChars(env, filePath, NULL);
    ctx = xmp_create_context();
    if (xmp_load_module(ctx, (char *)path) != 0) {
        LOGI("Failed to load module: %s", path);
        (*env)->ReleaseStringUTFChars(env, filePath, path);
        xmp_free_context(ctx);
        ctx = NULL;
        return JNI_FALSE;
    }
    xmp_start_player(ctx, 44100, 0);  // 采样率44100，立体声
    (*env)->ReleaseStringUTFChars(env, filePath, path);
    LOGI("Module loaded: %s", path);
    return JNI_TRUE;
}

// 填充一段音频缓冲区
JNIEXPORT jint JNICALL
Java_com_manager_nativelayer_XmpPlayer_nativeFillBuffer(JNIEnv *env, jobject thiz,
                                                    jshortArray buffer, jint size) {
    if (ctx == NULL) return 0;
    jshort *buf = (*env)->GetShortArrayElements(env, buffer, NULL);
    // xmp_play_buffer 要求提供缓冲区指针、字节大小、是否循环
    int ret = xmp_play_buffer(ctx, buf, size * sizeof(short), 0);
    (*env)->ReleaseShortArrayElements(env, buffer, buf, 0);
    return ret;  // 返回 0 表示继续，-1 表示播放结束等
}

// 停止并释放资源
JNIEXPORT void JNICALL
Java_com_manager_nativelayer_XmpPlayer_nativeRelease(JNIEnv *env, jobject thiz) {
    if (ctx) {
        xmp_end_player(ctx);
        xmp_release_module(ctx);
        xmp_free_context(ctx);
        ctx = NULL;
    }
}