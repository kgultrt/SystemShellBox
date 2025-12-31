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

package com.manager.ssb.core;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import com.manager.ssb.core.openmethod.AudioFileHandler;
import com.manager.ssb.core.openmethod.TextFileHandler;
import com.manager.ssb.core.openmethod.CompressFileHandler;
import com.manager.ssb.core.openmethod.HtmlFileHandler;
import com.manager.ssb.core.openmethod.UnknownFileHandler;

public class FileOpener {
    private static final String TAG = "FileOpener";
    private static final Map<FileType, FileHandler> HANDLER_MAP = new HashMap<>();
    private static final FileHandler DEFAULT_HANDLER = new UnknownFileHandler();

    static {
        // 初始化处理器映射
        HANDLER_MAP.put(FileType.AUDIO, new AudioFileHandler());
        HANDLER_MAP.put(FileType.TEXT, new TextFileHandler());
        HANDLER_MAP.put(FileType.COMPRESS, new CompressFileHandler());
        HANDLER_MAP.put(FileType.HTML, new HtmlFileHandler());
        HANDLER_MAP.put(FileType.APK, new UnknownFileHandler());
        HANDLER_MAP.put(FileType.UNKNOWN, DEFAULT_HANDLER);
    }

    public static void openFile(Context context, String filePath, String fileName) {
        if (context == null || filePath == null) {
            Log.e(TAG, "Invalid parameters: context or filePath is null");
            return;
        }
        
        try {
            FileType fileType = FileTypeRegistry.getFileType(filePath);
            
            // 兼容低版本 Android
            FileHandler handler = HANDLER_MAP.get(fileType);
            if (handler == null) {
                handler = DEFAULT_HANDLER;
                Log.w(TAG, "No handler found for file type: " + fileType + ", using default handler");
            }
            
            Log.d(TAG, "Opening file: " + fileName + " with type: " + fileType);
            handler.handle(context, filePath, fileName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + filePath, e);
            // 确保即使用户文件打开失败，应用也不会崩溃
            DEFAULT_HANDLER.handle(context, filePath, fileName);
        }
    }
    
    /**
     * 注册自定义文件处理器
     */
    public static void registerHandler(FileType fileType, FileHandler handler) {
        if (fileType != null && handler != null) {
            HANDLER_MAP.put(fileType, handler);
            Log.d(TAG, "Registered custom handler for file type: " + fileType);
        }
    }
    
    /**
     * 取消注册文件处理器
     */
    public static void unregisterHandler(FileType fileType) {
        if (fileType != null && fileType != FileType.UNKNOWN) {
            HANDLER_MAP.remove(fileType);
            Log.d(TAG, "Unregistered handler for file type: " + fileType);
        }
    }
}