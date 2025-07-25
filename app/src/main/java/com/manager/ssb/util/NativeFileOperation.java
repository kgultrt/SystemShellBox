package com.manager.ssb.util;

import java.io.File;

public class NativeFileOperation {
    
    // 添加冲突状态码
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR = -1;
    public static final int STATUS_CONFLICT = -100;
    
    // 添加冲突处理选项
    public interface ConflictAction {
        int OVERWRITE = 0;
        int SKIP = 1;
        int KEEP_BOTH = 2;
    }
    
    // 修改回调接口添加状态码
    public interface ProgressCallback {
        void onProgress(String currentFile, long copied, long total, int status);
    }
    
    static {
        System.loadLibrary("ssb_daemon");
    }
    // 修改JNI方法签名
    private native static int nativeCopy(String src, String dest, ProgressCallback callback);
    
    // 修改copy方法
    public static int copy(String src, String dest, ProgressCallback callback) {
        return nativeCopy(src, dest, callback);
    }
    
    // 其他方法保持不变
    public static boolean delete(String path) {
        return nativeDelete(path);
    }
    
    public static boolean move(String src, String dest) {
        return nativeMove(src, dest);
    }
    
    public static boolean rename(String src, String newName) {
        return nativeMove(src, newName);
    }
    
    private native static boolean nativeDelete(String path);
    private native static boolean nativeMove(String src, String dest);
}
