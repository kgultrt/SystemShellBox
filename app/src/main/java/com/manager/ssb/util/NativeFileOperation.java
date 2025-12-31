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

package com.manager.ssb.util;

import java.io.File;

public class NativeFileOperation {
    
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR = 1;
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
        System.loadLibrary("file_utils");
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