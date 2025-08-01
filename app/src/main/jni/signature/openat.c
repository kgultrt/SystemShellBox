//
// Created by Thom on 2019/3/30.
// Modified by kgultrt on 2025/7/29.
//

#include <unistd.h>
#include <sys/syscall.h>
#include <errno.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#ifndef __NR_openat
#define __NR_openat 322 // ARM EABI 默认值
#endif

intptr_t openAt(intptr_t fd, const char* path, intptr_t flag) {
#if defined(__arm__)
    intptr_t r;
    int syscall_num = __NR_openat;
    __android_log_print(ANDROID_LOG_DEBUG, "openAt", "Using syscall number: %d", syscall_num);
    __asm__ volatile(
        "mov r0, %[fd]\n\t"
        "mov r1, %[path]\n\t"
        "mov r2, %[flag]\n\t"
        "mov r7, %[syscall]\n\t"
        "svc #0\n\t"
        "mov %[result], r0\n\t"
        : [result] "=r" (r)
        : [fd] "r" (fd),
          [path] "r" (path),
          [flag] "r" (flag),
          [syscall] "r" (syscall_num)
        : "r0", "r1", "r2", "r7", "memory"
    );
    if (r < 0) {
        errno = -r;
        __android_log_print(ANDROID_LOG_ERROR, "openAt", "Syscall failed, errno: %d (%s)", errno, strerror(errno));
        return -1;
    }
    return r;
#elif defined(__aarch64__)
    intptr_t r;
    __asm__ volatile(
        "mov x0, %[fd]\n\t"
        "mov x1, %[path]\n\t"
        "mov x2, %[flag]\n\t"
        "mov x8, %[syscall]\n\t"
        "svc #0\n\t"
        "mov %[result], x0\n\t"
        : [result] "=r" (r)
        : [fd] "r" (fd),
          [path] "r" (path),
          [flag] "r" (flag),
          [syscall] "r" (__NR_openat)
        : "x0", "x1", "x2", "x8", "memory"
    );
    if (r < 0) {
        errno = -r;
        return -1;
    }
    return r;
#elif defined(__i386__)
    intptr_t r;
    __asm__ volatile(
        "int $0x80"
        : "=a" (r)
        : "a" (__NR_openat),
          "b" (fd),
          "c" (path),
          "d" (flag)
        : "memory"
    );
    if (r < 0) {
        errno = -r;
        return -1;
    }
    return r;
#elif defined(__x86_64__)
    intptr_t r;
    __asm__ volatile(
        "syscall"
        : "=a" (r)
        : "a" (__NR_openat),
          "D" (fd),
          "S" (path),
          "d" (flag)
        : "rcx", "r11", "memory"
    );
    if (r < 0) {
        errno = -r;
        return -1;
    }
    return r;
#else
    intptr_t r = (intptr_t) syscall(__NR_openat, fd, path, flag);
    if (r < 0) {
        errno = -r;
        return -1;
    }
    return r;
#endif
}

JNIEXPORT jint JNICALL
Java_com_manager_ssb_util_SignatureVerify_openAt(JNIEnv *env, jclass clazz, jstring path) {
    if (path == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "openAt", "Path is NULL");
        return -1;
    }

    const char* p = (*env)->GetStringUTFChars(env, path, NULL);
    if (p == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "openAt", "Failed to get UTF chars");
        return -1;
    }

    if (access(p, R_OK) != 0) {
        int err = errno;
        __android_log_print(ANDROID_LOG_ERROR, "openAt", "Cannot access path: %s, error: %s", p, strerror(err));
        (*env)->ReleaseStringUTFChars(env, path, p);
        return -1;
    }

    __android_log_print(ANDROID_LOG_INFO, "openAt", "Opening file: %s", p);
    intptr_t fd = openAt(AT_FDCWD, p, O_RDONLY);

    if (fd < 0) {
        int err = errno;
        __android_log_print(ANDROID_LOG_ERROR, "openAt", 
                            "Failed to open file: %s, error: %s", 
                            p, strerror(err));
    } else {
        __android_log_print(ANDROID_LOG_INFO, "openAt", 
                            "Successfully opened file, fd: %ld", 
                            (long)fd);
    }

    (*env)->ReleaseStringUTFChars(env, path, p);
    return (jint)fd;
}