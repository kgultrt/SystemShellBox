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

// FileTypeRegistry.java
package com.manager.ssb.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileTypeRegistry {
    private static final Map<String, FileType> EXTENSION_MAP = new HashMap<>();

    static {
        registerAudioExtension(".mp3");
        registerAudioExtension(".wav");
        registerAudioExtension(".ogg");
        registerAudioExtension(".m4a");
        registerAudioExtension(".mid");
        registerAudioExtension(".flac");

        registerTextExtension(".txt");
        registerTextExtension(".java");
        registerTextExtension(".c");
        registerTextExtension(".cpp");
        registerTextExtension(".cs");
        registerTextExtension(".py");
        registerTextExtension(".cxx");
        registerTextExtension(".js");
        registerTextExtension(".css");
        registerTextExtension(".md");
        registerTextExtension(".go");
        registerTextExtension(".log");
        registerTextExtension(".sh");
        registerTextExtension(".rs");
        registerTextExtension(".bat");
        registerTextExtension(".kt");
        registerTextExtension(".h");
        registerTextExtension(".lua");
        registerTextExtension(".json");
        
        registerCompressExtension(".zip");
        registerCompressExtension(".tar"); 
        registerCompressExtension("gz");
        registerCompressExtension("bz2");
        registerCompressExtension("7z");
        registerCompressExtension("rar");
    }

    public static void registerAudioExtension(String extension) {
        registerExtension(extension, FileType.AUDIO);
    }

    public static void registerTextExtension(String extension) {
        registerExtension(extension, FileType.TEXT);
    }
    
    public static void registerCompressExtension(String extension) {
        registerExtension(extension, FileType.COMPRESS);
    }

    public static void registerExtension(String extension, FileType fileType) {
        if (extension == null || fileType == null) return;
        EXTENSION_MAP.put(extension.toLowerCase(), fileType);
    }

    public static FileType getFileType(String filePath) {
        String ext = getFileExtension(filePath);
        return EXTENSION_MAP.getOrDefault(ext, FileType.UNKNOWN);
    }

    public static String getFileExtension(String filePath) {
        if (filePath == null) return "";
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDotIndex).toLowerCase();
    }
}