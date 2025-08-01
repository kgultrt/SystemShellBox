package com.manager.ssb.util;

import java.io.File;

public class NativeFileOperation {
    
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR = -1;
    public static final int STATUS_CONFLICT = -100;
    public static final int STATUS_SKIPPED = -101;
    public static final int STATUS_RETRYING = -102;
    
    // 冲突处理选项
    public interface ConflictAction {
        int OVERWRITE = 0;
        int SKIP = 1;
        int KEEP_BOTH = 2;
    }
    
    // 修改后的回调接口
    public interface ProgressCallback {
        void onProgress(String currentFile, long copied, long total, int status);
    }
    
    static {
        System.loadLibrary("ssb_daemon");
    }
    
    // 修改后的JNI方法签名
    private native static int nativeCopy(String src, String dest, ProgressCallback callback);
    
    public static int copy(String src, String dest, ProgressCallback callback) {
        return nativeCopy(src, dest, callback);
    }
    
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