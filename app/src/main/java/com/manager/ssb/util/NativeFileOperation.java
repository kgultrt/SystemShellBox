package com.manager.ssb.util;

public class NativeFileOperation {
    public interface ProgressCallback {
        void onProgress(long copied, long total);
    }

    static {
        System.loadLibrary("ssb_daemon");
    }

    private native static boolean kissCopy(String src, String dest, ProgressCallback callback);
    private native static boolean kissDelete(String path);
    private native static boolean kissMove(String src, String dest);

    public static boolean copy(String src, String dest, ProgressCallback callback) {
        return kissCopy(src, dest, callback);
    }

    public static boolean delete(String path) {
        return kissDelete(path);
    }

    public static boolean move(String src, String dest) {
        return kissMove(src, dest);
    }
    
    public static boolean rename(String src, String newName) {
        return kissMove(src, newName); // 直接使用move方法实现重命名
    }
}