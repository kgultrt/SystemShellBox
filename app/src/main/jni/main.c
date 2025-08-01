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
#include <utime.h>
#include <sys/statvfs.h>


#define TAG "SSB_UTILS"
#define BUFFER_SIZE (256 * 1024)
#define MAX_PATH_LEN 4096
#define MAX_RECURSION_DEPTH 50

// 定义状态码
#define STATUS_SUCCESS 0
#define STATUS_ERROR -1
#define STATUS_CONFLICT -100

// 错误处理宏
#define KISS_FAIL(env, code, msg) do { \
    char full_msg[256]; \
    const char *err_str = strerror(errno); \
    snprintf(full_msg, sizeof(full_msg), "%s (errno: %d [%s]) at line %d", \
            msg, errno, err_str ? err_str : "unknown", __LINE__); \
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s", full_msg); \
    if (code) { \
        jclass ex = (*env)->FindClass(env, "java/io/IOException"); \
        if (ex) { \
            (*env)->ThrowNew(env, ex, full_msg); \
        } \
    } \
    return JNI_FALSE; \
} while(0)

// 安全路径拼接
static void safe_path_join(char *dest, const char *base, const char *append) {
    strncpy(dest, base, MAX_PATH_LEN - 1);
    dest[MAX_PATH_LEN - 1] = '\0';
    
    size_t len = strlen(dest);
    if (len > 0 && dest[len - 1] != '/') {
        strncat(dest, "/", MAX_PATH_LEN - len - 1);
        len++;
    }
    
    strncat(dest, append, MAX_PATH_LEN - len - 1);
}

// 递归创建目录
static int mkdir_p(const char *path) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "mkdir_p: %s", path);
    
    char tmp[MAX_PATH_LEN];
    char *p = NULL;
    
    snprintf(tmp, sizeof(tmp), "%s", path);
    size_t len = strlen(tmp);
    
    // 删除末尾斜杠
    while (len > 0 && tmp[len - 1] == '/') {
        tmp[len - 1] = '\0';
        len--;
    }
    
    // 跳过根目录
    p = (*tmp == '/') ? tmp + 1 : tmp;
    
    for (; *p; p++) {
        if (*p == '/') {
            *p = '\0';
            if (mkdir(tmp, 0775) != 0 && errno != EEXIST) {
                __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to create: %s (errno %d)", tmp, errno);
                return -1;
            }
            *p = '/';
        }
    }
    
    if (mkdir(tmp, 0775) != 0 && errno != EEXIST) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to create: %s (errno %d)", tmp, errno);
        return -1;
    }
    
    return 0;
}

// 递归删除函数
static int delete_recursive(const char *path) {
    struct stat statbuf;
    if (lstat(path, &statbuf) != 0) {
        if (errno == ENOENT) {
            __android_log_print(ANDROID_LOG_WARN, TAG, "Path does not exist: %s", path);
            return 1;
        }
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to lstat: %s (errno %d)", path, errno);
        return 0;
    }
    
    // 文件或链接
    if (S_ISREG(statbuf.st_mode) || S_ISLNK(statbuf.st_mode)) {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Deleting file/link: %s", path);
        if (unlink(path) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to unlink: %s (errno %d)", path, errno);
            return 0;
        }
    }
    // 目录
    else if (S_ISDIR(statbuf.st_mode)) {
        DIR *dir = opendir(path);
        if (!dir) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open dir: %s (errno %d)", path, errno);
            return 0;
        }
        
        int success = 1;
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
                continue;
                
            char child_path[MAX_PATH_LEN];
            safe_path_join(child_path, path, entry->d_name);
            
            if (!delete_recursive(child_path)) {
                success = 0;
                break;
            }
        }
        closedir(dir);
        
        if (!success) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Partial delete failed: %s", path);
            return 0;
        }
        
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Deleting directory: %s", path);
        if (rmdir(path) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to rmdir: %s (errno %d)", path, errno);
            return 0;
        }
    }
    // 其他类型 (FIFO, socket等)
    else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Deleting special file: %s", path);
        if (unlink(path) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to unlink special: %s (errno %d)", path, errno);
            return 0;
        }
    }
    
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_manager_ssb_util_NativeFileOperation_nativeCopy(
    JNIEnv* env, 
    jobject thiz,
    jstring jSrc,
    jstring jDest,
    jobject jCallback
) {
    // 递归深度检查
    static jint recursion_depth = 0;
    if (recursion_depth > MAX_RECURSION_DEPTH) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Recursion too deep! Max depth reached");
        KISS_FAIL(env, 1, "Directory structure too deep");
    }
    recursion_depth++;

    const char* src = (*env)->GetStringUTFChars(env, jSrc, NULL);
    const char* dest = (*env)->GetStringUTFChars(env, jDest, NULL);
    
    if (!src || !dest) {
        if (src) (*env)->ReleaseStringUTFChars(env, jSrc, src);
        if (dest) (*env)->ReleaseStringUTFChars(env, jDest, dest);
        recursion_depth--;
        return JNI_FALSE;
    }
    
    // 记录正在处理的项目
    __android_log_print(ANDROID_LOG_INFO, TAG, "Copying: '%s' -> '%s'", src, dest);
    
    // 获取进度回调方法
    jmethodID progressMethod = NULL;
    if (jCallback) {
        jclass callbackClass = (*env)->GetObjectClass(env, jCallback);
        progressMethod = (*env)->GetMethodID(env, callbackClass, "onProgress", "(Ljava/lang/String;JJI)V");
    }
    
    // 检测源类型
    struct stat src_stat;
    if (lstat(src, &src_stat) != 0) 
        KISS_FAIL(env, 1, "Failed to access source");
        
    const char* filename = strrchr(src, '/');
        if (filename) filename++;
        else filename = src;
        
    // 在文件复制前检查目标是否存在
    if (access(dest, F_OK) == 0 && S_ISREG(src_stat.st_mode)) {
        __android_log_print(ANDROID_LOG_WARN, TAG, "File conflict detected: %s", dest);
        
        // 如果文件存在，返回冲突状态码
        if (jCallback && progressMethod) {
            jstring jFilename = (*env)->NewStringUTF(env, filename);
            (*env)->CallVoidMethod(
                env, 
                jCallback, 
                progressMethod, 
                jFilename,
                0L,
                src_stat.st_size,
                (jint)STATUS_CONFLICT
            );
            (*env)->DeleteLocalRef(env, jFilename);
        }
        
        recursion_depth--;
        return STATUS_CONFLICT;
    }
    
    // 文件复制
    if (S_ISREG(src_stat.st_mode)) {
        int src_fd = open(src, O_RDONLY | O_CLOEXEC);
        if (src_fd == -1) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open source file: %s", src);
            KISS_FAIL(env, 1, "Failed to open source file");
        }
        
        // 确保目标目录存在
        char dest_dir[MAX_PATH_LEN];
        strncpy(dest_dir, dest, MAX_PATH_LEN);
        char* last_slash = strrchr(dest_dir, '/');
        if (last_slash) {
            *last_slash = '\0';
            if (access(dest_dir, F_OK) != 0) {
                __android_log_print(ANDROID_LOG_DEBUG, TAG, "Creating parent dir: %s", dest_dir);
                if (mkdir_p(dest_dir) != 0) {
                    close(src_fd);
                    KISS_FAIL(env, 1, "Failed to create parent directory");
                }
            }
        }
        
        int dest_fd = open(dest, O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC, 
                          src_stat.st_mode & 0777);
        if (dest_fd == -1) {
            close(src_fd);
            KISS_FAIL(env, 1, "Failed to create destination file");
        }
        
        // 获取文件名（不含路径）
        const char* filename = strrchr(src, '/');
        if (filename) filename++;
        else filename = src;
        
        // 使用sendfile进行高效复制
        off_t offset = 0;
        ssize_t result;
        while (offset < src_stat.st_size) {
            result = sendfile(dest_fd, src_fd, &offset, src_stat.st_size - offset);
            
            if (result <= 0) {
                if (errno == EAGAIN || errno == EINTR) continue;
                break;
            }
            
            // 进度更新（添加文件名参数）
            if (jCallback && progressMethod) {
                jstring jFilename = (*env)->NewStringUTF(env, filename);
                (*env)->CallVoidMethod(
                    env, 
                    jCallback, 
                    progressMethod, 
                    jFilename,
                    (jlong)offset, 
                    (jlong)src_stat.st_size,
                    (jint)STATUS_SUCCESS
                );
                (*env)->DeleteLocalRef(env, jFilename);
            }
        }
        
        close(src_fd);
        close(dest_fd);
        
        if (offset != (off_t)src_stat.st_size) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, 
                "Incomplete copy: %lld of %lld bytes copied to %s", 
                (long long)offset, (long long)src_stat.st_size, dest);
                
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
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "Creating directory: %s", dest);
        if (mkdir_p(dest) != 0) {
            KISS_FAIL(env, 1, "Failed to create directory");
        }
        
        // 设置目录权限
        if (chmod(dest, src_stat.st_mode) != 0) {
            __android_log_print(ANDROID_LOG_WARN, TAG, "Failed to set dir permissions: %s", dest);
        }
        
        DIR *dir = opendir(src);
        if (!dir) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to open source dir: %s", src);
            KISS_FAIL(env, 1, "Failed to open directory");
        }
        
        struct dirent *entry;
        int success = 1;
        while ((entry = readdir(dir)) != NULL) {
            if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
                continue;
                
            char src_path[MAX_PATH_LEN];
            char dest_path[MAX_PATH_LEN];
            safe_path_join(src_path, src, entry->d_name);
            safe_path_join(dest_path, dest, entry->d_name);
            
            // 检查访问权限
            if (access(src_path, R_OK) != 0) {
                __android_log_print(ANDROID_LOG_WARN, TAG, "Skipping inaccessible: %s", src_path);
                continue;
            }
            
            // 创建Java字符串
            jstring jSrcPath = (*env)->NewStringUTF(env, src_path);
            jstring jDestPath = (*env)->NewStringUTF(env, dest_path);
            
            if (!jSrcPath || !jDestPath) {
                __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to create JNI strings");
                success = 0;
                if (jSrcPath) (*env)->DeleteLocalRef(env, jSrcPath);
                if (jDestPath) (*env)->DeleteLocalRef(env, jDestPath);
                break;
            }
            
            // 递归复制
            jboolean result = Java_com_manager_ssb_util_NativeFileOperation_nativeCopy(
                env, thiz, jSrcPath, jDestPath, jCallback);
            
            // 释放JNI引用
            (*env)->DeleteLocalRef(env, jSrcPath);
            (*env)->DeleteLocalRef(env, jDestPath);
            
            if (result == STATUS_ERROR) {
                __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to copy: %s to %s", src_path, dest_path);
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
        if (len == -1) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to read symlink: %s", src);
            KISS_FAIL(env, 1, "Failed to read symlink");
        }
        
        link_target[len] = '\0';
        if (symlink(link_target, dest) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to create symlink: %s -> %s", dest, link_target);
            KISS_FAIL(env, 1, "Failed to create symlink");
        }
    }
    // 特殊文件处理
    else {
        __android_log_print(ANDROID_LOG_WARN, TAG, "Unsupported file type: %s (mode: %o)", src, src_stat.st_mode);
        KISS_FAIL(env, 0, "Unsupported file type");
    }
    
    // 成功完成
    __android_log_print(ANDROID_LOG_INFO, TAG, "Successfully copied: %s", src);
    (*env)->ReleaseStringUTFChars(env, jSrc, src);
    (*env)->ReleaseStringUTFChars(env, jDest, dest);
    
    // 正常返回改为0表示成功
    recursion_depth--;
    return STATUS_SUCCESS;
}

JNIEXPORT jboolean JNICALL
Java_com_manager_ssb_util_NativeFileOperation_nativeDelete(
    JNIEnv* env,
    jobject thiz,
    jstring jPath
) {
    (void)thiz; // 标记未使用参数，避免警告
    
    const char* path = (*env)->GetStringUTFChars(env, jPath, NULL);
    if (!path) return JNI_FALSE;
    
    __android_log_print(ANDROID_LOG_INFO, TAG, "Deleting: %s", path);
    int result = delete_recursive(path);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Delete result: %d for %s", result, path);
    
    (*env)->ReleaseStringUTFChars(env, jPath, path);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_manager_ssb_util_NativeFileOperation_nativeMove(
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
    
    __android_log_print(ANDROID_LOG_INFO, TAG, "Moving: '%s' -> '%s'", src, dest);
    
    // 尝试原子重命名
    if (rename(src, dest) == 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Move successful (rename)");
        (*env)->ReleaseStringUTFChars(env, jSrc, src);
        (*env)->ReleaseStringUTFChars(env, jDest, dest);
        return JNI_TRUE;
    }
    
    __android_log_print(ANDROID_LOG_WARN, TAG, "Rename failed (errno %d), using copy-delete", errno);
    
    // 跨设备回退到复制+删除
    if (Java_com_manager_ssb_util_NativeFileOperation_nativeCopy(env, thiz, jSrc, jDest, NULL)) {
        if (Java_com_manager_ssb_util_NativeFileOperation_nativeDelete(env, thiz, jSrc)) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "Move successful (copy-delete)");
            (*env)->ReleaseStringUTFChars(env, jSrc, src);
            (*env)->ReleaseStringUTFChars(env, jDest, dest);
            return JNI_TRUE;
        } else {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to delete source after copy");
        }
    }
    
    // 清理失败的复制
    __android_log_print(ANDROID_LOG_ERROR, TAG, "Move failed, cleaning up partial destination");
    Java_com_manager_ssb_util_NativeFileOperation_nativeDelete(env, thiz, jDest);
    
    (*env)->ReleaseStringUTFChars(env, jSrc, src);
    (*env)->ReleaseStringUTFChars(env, jDest, dest);
    return JNI_FALSE;
}