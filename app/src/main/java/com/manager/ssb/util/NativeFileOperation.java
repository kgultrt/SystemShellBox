package com.manager.ssb.util;

public class NativeFileOperation {
    public interface ProgressCallback {
        void onProgress(String currentFile, long copied, long total);
    }

    static {
        System.loadLibrary("ssb_daemon");
    }

    private native static boolean nativeCopy(String src, String dest, ProgressCallback callback);
    private native static boolean nativeDelete(String path);
    private native static boolean nativeMove(String src, String dest);

    public static boolean copy(String src, String dest, ProgressCallback callback) {
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
}