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
    }

    public static void registerAudioExtension(String extension) {
        registerExtension(extension, FileType.AUDIO);
    }

    public static void registerTextExtension(String extension) {
        registerExtension(extension, FileType.TEXT);
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