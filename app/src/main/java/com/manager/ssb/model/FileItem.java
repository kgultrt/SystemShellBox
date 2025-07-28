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

package com.manager.ssb.model;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.manager.ssb.core.FileType;

public class FileItem {
    private final File file;
    private long cachedSize = -1;
    private long cachedLastModified = -1;

    private final boolean isDirectory;
    private final String fileExtension; // 预存储小写扩展名
    
    // 使用volatile保证多线程可见性，采用延迟初始化
    private volatile Boolean isAudioFile = null;
    private volatile Boolean isTextFile = null;
    private volatile Boolean isZipFile = null;
    private volatile Boolean isHtmlFile = null;

    // 扩展名集合定义为不可变静态集合
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(
        "mp3", "wav", "ogg", "aac", "flac", "m4a", "wma", "mid"
    );
    
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        "txt", "java", "c", "cpp", "cs", "py", "cxx", "js", "css", 
        "md", "go", "log", "sh", "rs", "bat", "kt", "h", "lua", "json", "properties"
    );
    
    private static final Set<String> ZIP_EXTENSIONS = Set.of(
        "zip", "tar", "gz", "bz2", "7z", "rar"
    );
    
    private static final Set<String> HTML_EXTENSIONS = Set.of(
        "html", "htm"
    );

    public FileItem(File file) {
        this.file = file;
        this.isDirectory = file.isDirectory();
        this.fileExtension = initExtension();
    }

    // 一次性计算扩展名（目录返回null）
    private String initExtension() {
        if (isDirectory) return null;
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < name.length() - 1) ? 
            name.substring(dotIndex + 1).toLowerCase() : null;
    }

    public String getName() { return file.getName(); }
    public String getPath() { return file.getPath(); }
    public boolean isDirectory() { return isDirectory; }
    public File getFile() { return file; }

    // 类型判断改为直接使用预存扩展名
    public boolean isAudioFile() {
        if (isAudioFile == null) isAudioFile = fileExtension != null && AUDIO_EXTENSIONS.contains(fileExtension);
        return isAudioFile;
    }
    
    public boolean isTextFile() {
        if (isTextFile == null) isTextFile = fileExtension != null && TEXT_EXTENSIONS.contains(fileExtension);
        return isTextFile;
    }
    
    public boolean isZipFile() {
        if (isZipFile == null) isZipFile = fileExtension != null && ZIP_EXTENSIONS.contains(fileExtension);
        return isZipFile;
    }
    
    public boolean isHtmlFile() {
        if (isHtmlFile == null) isHtmlFile = fileExtension != null && HTML_EXTENSIONS.contains(fileExtension);
        return isHtmlFile;
    }

    // 尺寸和修改时间保持原有延迟加载
    public long getSize() {
        if (cachedSize == -1) cachedSize = isDirectory ? 0 : file.length();
        return cachedSize;
    }
    
    public long getLastModified() {
        if (cachedLastModified == -1) cachedLastModified = file.lastModified();
        return cachedLastModified;
    }
    
    public FileType resolveFileType() {
        if (isDirectory()) return FileType.DIRECTORY;
        if (isAudioFile()) return FileType.AUDIO;
        if (isTextFile()) return FileType.TEXT;
        if (isZipFile()) return FileType.COMPRESS;
        if (isHtmlFile()) return FileType.HTML;
        return FileType.UNKNOWN;
    }

}