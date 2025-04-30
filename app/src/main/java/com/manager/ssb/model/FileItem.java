package com.manager.ssb.model;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileItem {
    private final File file;
    private long cachedSize = -1;
    private long cachedLastModified = -1;

    private final boolean isDirectory;
    private volatile Boolean isAudioFile = null;
    private volatile Boolean isTextFile = null;

    // 支持的音频文件扩展名集合
    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(
            Arrays.asList("mp3", "wav", "ogg", "aac", "flac", "m4a", "wma", "mid")
    );

    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(
            Arrays.asList("txt", "java", "c", "cpp", "cs", "py", "cxx", "js", "css", "md", "go", "log", "sh", "rs", "bat", "kt", "h", "lua")
    );

    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
    }

    public String getName() {
        return file.getName();
    }

    public String getPath() {
        return file.getPath();
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isAudioFile() {
        if (isAudioFile == null) {
            isAudioFile = checkIsAudioFile();
        }
        return isAudioFile;
    }

    private boolean checkIsAudioFile() {
        if (isDirectory) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1);
            return AUDIO_EXTENSIONS.contains(extension);
        }
        return false;
    }

    public boolean isTextFile() {
        if (isTextFile == null) {
            isTextFile = checkIsTextFile();
        }
        return isTextFile;
    }

    private boolean checkIsTextFile() {
        if (isDirectory) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            String extension = fileName.substring(lastDotIndex + 1);
            return TEXT_EXTENSIONS.contains(extension);
        }
        return false;
    }

    public long getSize() {
        if (cachedSize == -1) {
            cachedSize = isDirectory ? 0 : file.length();
        }
        return cachedSize;
    }

    public long getLastModified() {
        if (cachedLastModified == -1) {
            cachedLastModified = file.lastModified();
        }
        return cachedLastModified;
    }

    public File getFile() {
        return file;
    }
}