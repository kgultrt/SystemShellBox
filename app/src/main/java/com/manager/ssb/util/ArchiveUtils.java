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

import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.libarchive.Archive;
import me.zhanghai.android.libarchive.ArchiveEntry;
import me.zhanghai.android.libarchive.ArchiveException;

public class ArchiveUtils {

    public interface ProgressCallback {
        void onProgress(int percent, String currentFile);
    }

    public static class ArchiveEntryInfo {
        public final String name;
        public final boolean isDirectory;
        public final long size;
        public final long lastModified;

        public ArchiveEntryInfo(String name, boolean isDirectory, long size, long lastModified) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }
    }

    // ---------- 浏览 ----------
    public static List<String> listAllEntries(String archivePath) throws IOException {
        List<String> entries = new ArrayList<>();
        readArchive(archivePath, (entryPtr, path, stat) -> {
            entries.add(path);
            return true;
        });
        return entries;
    }

    public static List<String> listDirectChildren(String archivePath, String prefix) throws IOException {
        if (prefix == null) prefix = "";
        String normalizedPrefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
        List<String> children = new ArrayList<>();
        readArchive(archivePath, (entryPtr, path, stat) -> {
            if (!path.startsWith(normalizedPrefix)) return true;
            String relative = path.substring(normalizedPrefix.length());
            if (relative.isEmpty()) return true;
            int slashIdx = relative.indexOf('/');
            if (slashIdx > 0) {
                String dirName = relative.substring(0, slashIdx + 1);
                if (!children.contains(dirName)) children.add(dirName);
            } else if (slashIdx == -1) {
                children.add(relative);
            }
            return true;
        });
        return children;
    }

    public static List<ArchiveEntryInfo> listDetailedChildren(String archivePath, String prefix) throws IOException {
        if (prefix == null) prefix = "";
        String normalizedPrefix = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
        List<ArchiveEntryInfo> children = new ArrayList<>();
        List<String> seenDirs = new ArrayList<>();
        readArchive(archivePath, (entryPtr, path, stat) -> {
            if (!path.startsWith(normalizedPrefix)) return true;
            String relative = path.substring(normalizedPrefix.length());
            if (relative.isEmpty()) return true;
            int slashIdx = relative.indexOf('/');
            if (slashIdx > 0) {
                String dirName = relative.substring(0, slashIdx + 1);
                if (!seenDirs.contains(dirName)) {
                    seenDirs.add(dirName);
                    boolean isDirEntry = path.endsWith("/");
                    children.add(new ArchiveEntryInfo(dirName, true,
                            isDirEntry ? stat.stSize : 0,
                            isDirEntry ? stat.stMtim.tvSec : 0));
                }
            } else if (slashIdx == -1) {
                children.add(new ArchiveEntryInfo(relative, false, stat.stSize, stat.stMtim.tvSec));
            }
            return true;
        });
        return children;
    }

    // ---------- 解压 ----------
    public static void extractAll(String archivePath, String destDir, ProgressCallback callback) throws IOException {
        File dest = new File(destDir);
        if (!dest.exists()) dest.mkdirs();
        String destCanonical = dest.getCanonicalPath();
        int totalEntries = countEntries(archivePath);
        int[] processed = {0};
        int[] lastPercent = {-1};
        readArchiveWithData(archivePath, (archive, entryPtr, path, stat, reader) -> {
            File outputFile = new File(dest, path);
            if (!outputFile.getCanonicalPath().startsWith(destCanonical + File.separator))
                throw new SecurityException("Entry outside target dir: " + path);
            if (path.endsWith("/")) {
                outputFile.mkdirs();
            } else {
                outputFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = reader.read(buffer)) > 0) fos.write(buffer, 0, len);
                }
            }
            processed[0]++;
            if (callback != null && totalEntries > 0) {
                int percent = (processed[0] * 100) / totalEntries;
                if (percent != lastPercent[0]) {
                    lastPercent[0] = percent;
                    callback.onProgress(percent, path);
                }
            }
            return true;
        });
    }

    public static void extractEntry(String archivePath, String entryPath, String destDir) throws IOException {
        File dest = new File(destDir);
        if (!dest.exists()) dest.mkdirs();
        String destCanonical = dest.getCanonicalPath();
        readArchiveWithData(archivePath, (archive, entryPtr, path, stat, reader) -> {
            if (!path.equals(entryPath)) return true;
            File outputFile = new File(dest, entryPath);
            if (!outputFile.getCanonicalPath().startsWith(destCanonical + File.separator))
                throw new SecurityException("Entry outside target dir: " + entryPath);
            if (path.endsWith("/")) {
                outputFile.mkdirs();
                return false;
            } else {
                outputFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = reader.read(buffer)) > 0) fos.write(buffer, 0, len);
                }
                return false;
            }
        });
    }

    /**
     * 递归解压指定目录（包含其下所有文件和子目录）
     */
    public static void extractDirectory(String archivePath, String entryDirPath, String destDir,
                                        ProgressCallback callback) throws IOException {
        // 保证 entryDirPath 以 "/" 结尾，并立即复制为 final 变量供 lambda 使用
        if (!entryDirPath.endsWith("/")) {
            entryDirPath += "/";
        }
        final String finalDirPath = entryDirPath; // 现在 effectively final

        File dest = new File(destDir);
        if (!dest.exists()) dest.mkdirs();
        String destCanonical = dest.getCanonicalPath();
        int total = countEntriesInDir(archivePath, finalDirPath);
        int[] processed = {0};
        int[] lastPercent = {-1};
        readArchiveWithData(archivePath, (archive, entryPtr, path, stat, reader) -> {
            if (path.startsWith(finalDirPath) && !path.equals(finalDirPath)) {
                File outputFile = new File(dest, path);
                if (!outputFile.getCanonicalPath().startsWith(destCanonical + File.separator))
                    throw new SecurityException("Entry outside target dir: " + path);
                if (path.endsWith("/")) {
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = reader.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
                }
                processed[0]++;
                if (callback != null && total > 0) {
                    int percent = (processed[0] * 100) / total;
                    if (percent != lastPercent[0]) {
                        lastPercent[0] = percent;
                        callback.onProgress(percent, path);
                    }
                }
            }
            return true;
        });
    }

    // ---------- 压缩 ----------
    public static void createArchive(String archivePath, String baseDir, List<File> files,
                                     int formatCode, int filterCode, ProgressCallback callback) throws IOException {
        File outFile = new File(archivePath);
        outFile.getParentFile().mkdirs();
        long archive = 0;
        try {
            archive = Archive.writeNew();
            Archive.writeSetFormat(archive, formatCode);
            if (filterCode != Archive.FILTER_NONE) Archive.writeAddFilter(archive, filterCode);
            Archive.writeOpenFileName(archive, archivePath.getBytes(StandardCharsets.UTF_8));
            int totalFiles = countFilesToAdd(files);
            int[] processed = {0};
            int[] lastPercent = {-1};
            for (File file : files)
                addFileToArchive(archive, baseDir, file, processed, totalFiles, callback, lastPercent);
            Archive.writeClose(archive);
        } catch (ArchiveException e) {
            throw new IOException("Failed to create archive: " + e.getMessage(), e);
        } finally {
            if (archive != 0) try { Archive.free(archive); } catch (ArchiveException ignored) {}
        }
    }

    private static int countFilesToAdd(List<File> files) {
        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                File[] children = f.listFiles();
                if (children != null) count += countFilesToAdd(java.util.Arrays.asList(children));
            } else count++;
        }
        return count;
    }

    private static void addFileToArchive(long archive, String baseDir, File file,
                                         int[] processed, int totalFiles,
                                         ProgressCallback callback, int[] lastPercent) throws IOException, ArchiveException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                String entryPath = getRelativePath(baseDir, file) + "/";
                writeDirectoryEntry(archive, entryPath);
                for (File child : children)
                    addFileToArchive(archive, baseDir, child, processed, totalFiles, callback, lastPercent);
            }
        } else {
            String entryPath = getRelativePath(baseDir, file);
            long entry = ArchiveEntry.new2(archive);
            ArchiveEntry.setPathnameUtf8(entry, entryPath);
            ArchiveEntry.setFiletype(entry, ArchiveEntry.AE_IFREG);
            ArchiveEntry.setSize(entry, file.length());
            ArchiveEntry.setMtime(entry, file.lastModified() / 1000, 0);
            Archive.writeHeader(archive, entry);
            ArchiveEntry.free(entry);
            byte[] buffer = new byte[8192];
            try (FileInputStream fis = new FileInputStream(file)) {
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(len);
                    byteBuffer.put(buffer, 0, len);
                    byteBuffer.flip();
                    Archive.writeData(archive, byteBuffer);
                }
            }
            Archive.writeFinishEntry(archive);
            processed[0]++;
            if (callback != null && totalFiles > 0) {
                int percent = (processed[0] * 100) / totalFiles;
                if (percent != lastPercent[0]) {
                    lastPercent[0] = percent;
                    callback.onProgress(percent, entryPath);
                }
            }
        }
    }

    private static void writeDirectoryEntry(long archive, String path) throws ArchiveException {
        long entry = ArchiveEntry.new2(archive);
        ArchiveEntry.setPathnameUtf8(entry, path);
        ArchiveEntry.setFiletype(entry, ArchiveEntry.AE_IFDIR);
        Archive.writeHeader(archive, entry);
        Archive.writeFinishEntry(archive);
        ArchiveEntry.free(entry);
    }

    private static String getRelativePath(String baseDir, File file) {
        String base = new File(baseDir).getAbsolutePath();
        String abs = file.getAbsolutePath();
        if (abs.startsWith(base)) {
            String rel = abs.substring(base.length());
            if (rel.startsWith(File.separator)) rel = rel.substring(1);
            return rel.replace(File.separatorChar, '/');
        }
        return abs.replace(File.separatorChar, '/');
    }

    // ---------- 内部遍历 ----------
    private static void readArchive(String filePath, EntryMetaProcessor processor) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("File not found: " + filePath);
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
            int fd = pfd.getFd();
            long archive = Archive.readNew();
            try {
                Archive.setCharset(archive, StandardCharsets.UTF_8.name().getBytes(StandardCharsets.UTF_8));
                Archive.readSupportFilterAll(archive);
                Archive.readSupportFormatAll(archive);
                Archive.readOpenFd(archive, fd, 8192);
                long entryPtr;
                while ((entryPtr = Archive.readNextHeader(archive)) != 0) {
                    String path = getEntryPath(entryPtr);
                    ArchiveEntry.StructStat stat = ArchiveEntry.stat(entryPtr);
                    if (!processor.process(entryPtr, path, stat)) break;
                }
            } finally {
                Archive.free(archive);
            }
        }
    }

    private static void readArchiveWithData(String filePath, DataEntryProcessor processor) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("File not found: " + filePath);
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
            int fd = pfd.getFd();
            long archive = Archive.readNew();
            try {
                Archive.setCharset(archive, StandardCharsets.UTF_8.name().getBytes(StandardCharsets.UTF_8));
                Archive.readSupportFilterAll(archive);
                Archive.readSupportFormatAll(archive);
                Archive.readOpenFd(archive, fd, 8192);
                long entryPtr;
                while ((entryPtr = Archive.readNextHeader(archive)) != 0) {
                    String path = getEntryPath(entryPtr);
                    ArchiveEntry.StructStat stat = ArchiveEntry.stat(entryPtr);
                    DataReader reader = new DataReader() {
                        private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
                        @Override
                        public int read(byte[] buffer) throws IOException {
                            byteBuffer.clear();
                            try { Archive.readData(archive, byteBuffer); } catch (ArchiveException e) { throw new IOException(e); }
                            byteBuffer.flip();
                            int remaining = byteBuffer.remaining();
                            if (remaining <= 0) return -1;
                            int toRead = Math.min(buffer.length, remaining);
                            byteBuffer.get(buffer, 0, toRead);
                            return toRead;
                        }
                    };
                    if (!processor.onEntry(archive, entryPtr, path, stat, reader)) break;
                }
            } finally {
                Archive.free(archive);
            }
        }
    }

    private static String getEntryPath(long entryPtr) {
        String utf8 = ArchiveEntry.pathnameUtf8(entryPtr);
        if (utf8 != null) return utf8;
        byte[] raw = ArchiveEntry.pathname(entryPtr);
        if (raw != null) return new String(raw, StandardCharsets.UTF_8);
        return "";
    }

    private static int countEntries(String archivePath) throws IOException {
        int[] count = {0};
        readArchive(archivePath, (entryPtr, path, stat) -> { count[0]++; return true; });
        return count[0];
    }

    private static int countEntriesInDir(String archivePath, String dirPath) throws IOException {
        int[] count = {0};
        readArchive(archivePath, (entryPtr, path, stat) -> {
            if (path.startsWith(dirPath) && !path.equals(dirPath)) count[0]++;
            return true;
        });
        return count[0];
    }

    private interface EntryMetaProcessor {
        boolean process(long entryPtr, String path, ArchiveEntry.StructStat stat) throws IOException;
    }

    private interface DataEntryProcessor {
        boolean onEntry(long archive, long entryPtr, String path, ArchiveEntry.StructStat stat, DataReader reader) throws IOException;
    }

    private interface DataReader {
        int read(byte[] buffer) throws IOException;
    }
}