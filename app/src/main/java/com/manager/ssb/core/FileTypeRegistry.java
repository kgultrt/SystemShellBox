/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
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

import java.util.HashMap;
import java.util.Map;

public class FileTypeRegistry {
    private static final Map<String, FileType> EXTENSION_MAP = new HashMap<>();
    
    // 文件类型配置
    private static final String[][] FILE_TYPE_CONFIG = {
        // {扩展名, 文件类型名称}
        {".mp3", "AUDIO"}, {".wav", "AUDIO"}, {".ogg", "AUDIO"}, 
        {".m4a", "AUDIO"}, {".mid", "AUDIO"}, {".flac", "AUDIO"},
        
        {".txt", "TEXT"}, {".java", "TEXT"}, {".c", "TEXT"}, 
        {".cpp", "TEXT"}, {".cs", "TEXT"}, {".py", "TEXT"},
        {".cxx", "TEXT"}, {".js", "TEXT"}, {".css", "TEXT"},
        {".md", "TEXT"}, {".go", "TEXT"}, {".log", "TEXT"},
        {".sh", "TEXT"}, {".rs", "TEXT"}, {".bat", "TEXT"},
        {".kt", "TEXT"}, {".h", "TEXT"}, {".lua", "TEXT"},
        {".json", "TEXT"}, {".properties", "TEXT"},
        
        {".zip", "COMPRESS"}, {".tar", "COMPRESS"}, {".gz", "COMPRESS"},
        {".bz2", "COMPRESS"}, {".7z", "COMPRESS"}, {".rar", "COMPRESS"},
        
        {".html", "HTML"}, {".htm", "HTML"},
        
        {".apk", "APK"}
    };

    static {
        initializeFileTypes();
    }

    private static void initializeFileTypes() {
        for (String[] config : FILE_TYPE_CONFIG) {
            if (config.length == 2) {
                String extension = config[0].toLowerCase();
                FileType fileType = parseFileType(config[1]);
                if (fileType != null) {
                    EXTENSION_MAP.put(extension, fileType);
                }
            }
        }
    }

    private static FileType parseFileType(String typeName) {
        try {
            return FileType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return FileType.UNKNOWN;
        }
    }

    public static void registerExtension(String extension, FileType fileType) {
        if (extension != null && fileType != null) {
            EXTENSION_MAP.put(extension.toLowerCase(), fileType);
        }
    }

    public static void unregisterExtension(String extension) {
        if (extension != null) {
            EXTENSION_MAP.remove(extension.toLowerCase());
        }
    }

    public static FileType getFileType(String filePath) {
        if (filePath == null) {
            return FileType.UNKNOWN;
        }
        
        String ext = getFileExtension(filePath);
        FileType fileType = EXTENSION_MAP.get(ext);
        
        // 兼容低版本 Android
        return fileType != null ? fileType : FileType.UNKNOWN;
    }

    public static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        
        return filePath.substring(lastDotIndex).toLowerCase();
    }
    
    public static boolean isRegisteredExtension(String extension) {
        return extension != null && EXTENSION_MAP.containsKey(extension.toLowerCase());
    }
    
    public static void clearAllExtensions() {
        EXTENSION_MAP.clear();
    }
}