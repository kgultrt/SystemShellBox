#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <android/log.h>
#include <sys/sendfile.h>
#include <sys/statvfs.h>  // 添加所需头文件
#include <utime.h>        // 添加utime头文件
#include <sys/vfs.h>      // 添加statvfs头文件

#define TAG "SSB_DAEMON"
#define BUFFER_SIZE (256 * 1024)
#define MAX_PATH_LEN 4096

// 错误处理宏
#define KISS_FAIL(env, code, msg) do { \
    __android_log_print(ANDROID_LOG_ERROR, TAG, "[%d] %s", errno, msg); \
    if (code) { \
        jclass ex = (*env)->FindClass(env, "java/io/IOException"); \
        if (ex) { \
            char full_msg[256]; \
            snprintf(full_msg, sizeof(full_msg), "%s (errno: %d)", msg, errno); \
            (*env)->ThrowNew(env, ex, full_msg); \
        } \
    } \
    return JNI_FALSE; \
} while(0)

// 提前声明辅助函数
static int mkdir_p(const char *path);
static int delete_recursive(const char *path);

JNIEXPORT jboolean JNICALL
Java_com_manager_ssb_util_NativeFileOperation_kissCopy(
    JNIEnv* env, 
    jobject thiz,
    jstring jSrc,
    jstring jDest,
    jobject jCallback
) {
    const char* src = (*env)->GetStringUTFChars(env, jSrc, NULL);
    const char* dest = (*env)->GetStringUTFChars(env, jDest, NULL);
    
    if (!src || !dest) {
        if (src) (*env)->ReleaseStringUTFChars(env, jSrc, src);
        if (dest) (*env)->ReleaseStringUTFChars(env, jDest, dest);
        return JNI_FALSE;
    }
    
    // 获取进度回调方法
    jmethodID progressMethod = NULL;
    if (jCallback) {
        jclass callbackClass = (*env)->GetObjectClass(env, jCallback);
        progressMethod = (*env)->GetMethodID(env, callbackClass, "onProgress", "(JJ)V");
    }
    
    // 检测源类型
    struct stat src_stat;
    if (lstat(src, &src_stat) != 0) 
        KISS_FAIL(env, 1, "Failed to access source");
    
    // 文件复制
    if (S_ISREG(src_stat.st_mode)) {
        int src_fd = open(src, O_RDONLY);
        if (src_fd == -1) KISS_FAIL(env, 1, "Failed to open source file");
        
        // 确保目标目录存在
        char dest_dir[MAX_PATH_LEN];
        strncpy(dest_dir, dest, MAX_PATH_LEN);
        char* last_slash = strrchr(dest_dir, '/');
        if (last_slash) {
            *last_slash = '\0';
            if (access(dest_dir, F_OK) != 0 && mkdir_p(dest_dir) != 0) {
                close(src_fd);
                KISS_FAIL(env, 1, "Failed to create parent directory");
            }
        }
        
        int dest_fd = open(dest, O_WRONLY | O_CREAT | O_EXCL, src_stat.st_mode & 0777);
        if (dest_fd == -1) {
            close(src_fd);
            KISS_FAIL(env, 1, "Failed to create destination file");
        }
        
        // 使用sendfile进行高效复制
        off_t offset = 0;
        ssize_t result;
        while (offset < src_stat.st_size) {
            result = sendfile(dest_fd, src_fd, &offset, src_stat.st_size - offset);
            
            if (result <= 0) {
                if (errno == EAGAIN || errno == EINTR) continue;
                break;
            }
            
            // 进度更新
            if (jCallback && progressMethod) {
                (*env)->CallVoidMethod(
                    env, 
                    jCallback, 
                    progressMethod, 
                    (jlong)offset, 
                    (jlong)src_stat.st_size
                );
            }
        }
        
        close(src_fd);
        close(dest_fd);
        
        if (offset != (off_t)src_stat.st_size) {
            unlink(dest); // 删除不完整的文件
            KISS_FAIL(env, 1, "Failed to complete file copy");
        }
        
        // 复制文件属性
        struct utimbuf times;
        times.actime = src_stat.st_atime;
        times.modtime = src_stat.st_mtime;
        utime(dest, &times);
        
        chmod(dest, src_stat.st_mode);
        chown(dest, src_stat.st_uid, src_stat.st_gid);
    }
    // 目录复制
    else if (S_ISDIR(src_stat.st_mode)) {
        if (mkdir_p(dest) != 0) KISS_FAIL(env, 1, "Failed to create directory");
        
        DIR *dir = opendir(src);
        if (!dir) KISS_FAIL(env, 1, "Failed to open directory");
        
        struct dirent *entry;
        int success = 1;
        while ((entry = readdir(dir)) != NULL) {
            if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
                continue;
                
            char src_path[MAX_PATH_LEN];
            char dest_path[MAX_PATH_LEN];
            snprintf(src_path, MAX_PATH_LEN, "%s/%s", src, entry->d_name);
            snprintf(dest_path, MAX_PATH_LEN, "%s/%s", dest, entry->d_name);
            
            if (!Java_com_manager_ssb_util_NativeFileOperation_kissCopy(env, thiz,
                 (*env)->NewStringUTF(env, src_path),
                 (*env)->NewStringUTF(env, dest_path),
                 jCallback)) {
                success = 0;
                break;
            }
        }
        closedir(dir);
        
        if (!success) {
            KISS_FAIL(env, 0, "Partial directory copy failed");
        }
    }
    // 符号链接处理
    else if (S_ISLNK(src_stat.st_mode)) {
        char link_target[MAX_PATH_LEN];
        ssize_t len = readlink(src, link_target, MAX_PATH_LEN - 1);
        if (len == -1) KISS_FAIL(env, 1, "Failed to read symlink");
        
        link_target[len] = '\0';
        if (symlink(link_target, dest) != 0) 
            KISS_FAIL(env, 1, "Failed to create symlink");
    }
    // 特殊文件
    else {
        KISS_FAIL(env, 0, "Unsupported file type");
    }
    
    // 成功完成
    (*env)->ReleaseStringUTFChars(env, jSrc, src);
    (*env)->ReleaseStringUTFChars(env, jDest, dest);
    return JNI_TRUE;
}

// 递归创建目录
static int mkdir_p(const char *path) {
    char tmp[MAX_PATH_LEN];
    char *p = NULL;
    
    snprintf(tmp, sizeof(tmp), "%s", path);
    size_t len = strlen(tmp);
    
    // 删除末尾斜杠
    while (len > 0 && tmp[len - 1] == '/') {
        tmp[len - 1] = '\0';
        len--;
    }
    
    for (p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = 0;
            if (mkdir(tmp, 0775) != 0 && errno != EEXIST) {
                return -1;
            }
            *p = '/';
        }
    }
    
    if (mkdir(tmp, 0775) != 0 && errno != EEXIST) {
        return -1;
    }
    
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_manager_ssb_util_NativeFileOperation_kissDelete(
    JNIEnv* env,
    jobject thiz,
    jstring jPath
) {
    (void)thiz; // 标记未使用参数，避免警告
    
    const char* path = (*env)->GetStringUTFChars(env, jPath, NULL);
    if (!path) return JNI_FALSE;
    
    int result = delete_recursive(path);
    
    (*env)->ReleaseStringUTFChars(env, jPath, path);
    return result ? JNI_TRUE : JNI_FALSE;
}

static int delete_recursive(const char *path) {
    struct stat statbuf;
    if (lstat(path, &statbuf) != 0) {
        if (errno == ENOENT) return 1; // 文件不存在视为成功
        return 0;
    }
    
    // 文件或链接
    if (S_ISREG(statbuf.st_mode) || S_ISLNK(statbuf.st_mode)) {
        if (unlink(path) != 0) return 0;
    }
    // 目录
    else if (S_ISDIR(statbuf.st_mode)) {
        DIR *dir = opendir(path);
        if (!dir) return 0;
        
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
                continue;
                
            char child_path[MAX_PATH_LEN];
            snprintf(child_path, MAX_PATH_LEN, "%s/%s", path, entry->d_name);
            
            if (!delete_recursive(child_path)) {
                closedir(dir);
                return 0;
            }
        }
        closedir(dir);
        
        if (rmdir(path) != 0) return 0;
    }
    // 其他类型 (FIFO, socket等)
    else {
        if (unlink(path) != 0) return 0;
    }
    
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_manager_ssb_util_NativeFileOperation_kissMove(
    JNIEnv* env,
    jobject thiz,
    jstring jSrc,
    jstring jDest
) {
    (void)thiz; // 标记未使用参数，避免警告
    
    const char* src = (*env)->GetStringUTFChars(env, jSrc, NULL);
    const char* dest = (*env)->GetStringUTFChars(env, jDest, NULL);
    
    if (!src || !dest) {
        if (src) (*env)->ReleaseStringUTFChars(env, jSrc, src);
        if (dest) (*env)->ReleaseStringUTFChars(env, jDest, dest);
        return JNI_FALSE;
    }
    
    // 尝试原子重命名
    if (rename(src, dest) == 0) {
        (*env)->ReleaseStringUTFChars(env, jSrc, src);
        (*env)->ReleaseStringUTFChars(env, jDest, dest);
        return JNI_TRUE;
    }
    
    // 跨设备回退到复制+删除
    if (Java_com_manager_ssb_util_NativeFileOperation_kissCopy(env, thiz, jSrc, jDest, NULL) &&
        Java_com_manager_ssb_util_NativeFileOperation_kissDelete(env, thiz, jSrc)) {
        (*env)->ReleaseStringUTFChars(env, jSrc, src);
        (*env)->ReleaseStringUTFChars(env, jDest, dest);
        return JNI_TRUE;
    }
    
    // 清理失败的复制
    Java_com_manager_ssb_util_NativeFileOperation_kissDelete(env, thiz, jDest);
    
    (*env)->ReleaseStringUTFChars(env, jSrc, src);
    (*env)->ReleaseStringUTFChars(env, jDest, dest);
    return JNI_FALSE;
}