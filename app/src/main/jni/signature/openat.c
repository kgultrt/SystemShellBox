//
// Created by Thom on 2019/3/30.
// Modified by kgultrt on 2025/7/29.
//

#include <unistd.h>
#include <sys/syscall.h>
#include "openat.h"
#include <jni.h>
#include <android/log.h>

#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

// 添加参数使用属性，消除警告
intptr_t openAt(intptr_t __attribute__((unused)) fd, 
                const char* __attribute__((unused)) path, 
                intptr_t __attribute__((unused)) flag) {
    
#if defined(__arm__)
    intptr_t r;
    __asm__ volatile(  // 修复：使用 __asm__ 替代 asm
#ifndef OPTIMIZE_ASM
    "mov r0, %1\n\t"
    "mov r1, %2\n\t"
    "mov r2, %3\n\t"
#endif

    "mov ip, r7\n\t"
    ".cfi_register r7, ip\n\t"
    "mov r7, #" STR(__NR_openat) "\n\t"
    "svc #0\n\t"
    "mov r7, ip\n\t"
    ".cfi_restore r7\n\t"

#ifndef OPTIMIZE_ASM
    "mov %0, r0\n\t"
#endif
    : "=r" (r)
    : "r" (fd), "r" (path), "r" (flag));
    return r;
#elif defined(__aarch64__)
    intptr_t r;
    __asm__ volatile(  // 修复：使用 __asm__ 替代 asm
#ifndef OPTIMIZE_ASM
    "mov x0, %1\n\t"
    "mov x1, %2\n\t"
    "mov x2, %3\n\t"
#endif

    "mov x8, #" STR(__NR_openat) "\n\t"
    "svc #0\n\t"

#ifndef OPTIMIZE_ASM
    "mov %0, x0\n\t"
#endif
    : "=r" (r)
    : "r" (fd), "r" (path), "r" (flag));
    return r;
#else
    return (intptr_t) syscall(__NR_openat, fd, path, flag);
#endif
}

JNIEXPORT jint JNICALL
Java_com_manager_ssb_util_SignatureVerify_openAt(JNIEnv *env, __attribute__((unused)) jclass clazz, jstring path) {
    const char* p = (*env)->GetStringUTFChars(env, path, NULL);
    __android_log_print(ANDROID_LOG_INFO, "openAt", "path=%s", p);
    
    intptr_t fd = openAt(AT_FDCWD, p, O_RDONLY);
    
    // 修复：释放获取的字符串
    (*env)->ReleaseStringUTFChars(env, path, p);
    
    return (jint)fd;  // 明确转换为 jint
}