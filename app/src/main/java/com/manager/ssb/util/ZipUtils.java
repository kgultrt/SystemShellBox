/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private static volatile int nowProcess = 0;
    private static volatile String nowProcessFileName = "";
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB buffer

    /**
     * 压缩文件或目录
     */
    public static void zip(String sourcePath, String zipPath) {
        resetProcess();
        List<File> fileList = new ArrayList<>();
        File source = new File(sourcePath);
        
        if (!source.exists()) {
            throw new IllegalArgumentException("Source path does not exist: " + sourcePath);
        }

        // 收集所有需要压缩的文件
        if (source.isDirectory()) {
            getAllFiles(source, fileList);
        } else {
            fileList.add(source);
        }

        long totalSize = calculateTotalSize(fileList);
        long processedSize = 0;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            for (File file : fileList) {
                nowProcessFileName = file.getName();
                
                String entryName = getRelativePath(source, file);
                if (entryName.isEmpty()) continue;

                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);

                if (!file.isDirectory()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                            processedSize += length;
                            nowProcess = (int) ((processedSize * 100) / totalSize);
                        }
                    }
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Zip operation failed", e);
        } finally {
            resetProcess();
        }
    }

    /**
     * 解压缩文件
     */
    public static void unzip(String zipPath, String destPath) {
        resetProcess();
        File destDir = new File(destPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            
            // 先计算总大小
            long totalSize = getZipTotalSize(zipPath);
            long processedSize = 0;

            while ((entry = zis.getNextEntry()) != null) {
                nowProcessFileName = entry.getName();
                File newFile = new File(destPath, entry.getName());

                // 防止zip slip漏洞
                String canonicalPath = newFile.getCanonicalPath();
                if (!canonicalPath.startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new SecurityException("Entry is outside of the target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // 创建父目录
                    newFile.getParentFile().mkdirs();
                    
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                            processedSize += length;
                            nowProcess = (int) ((processedSize * 100) / totalSize);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unzip operation failed", e);
        } finally {
            resetProcess();
        }
    }

    /**
     * 查看zip文件内容
     */
    public static List<String> zipcat(String zipPath) {
        List<String> fileList = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                fileList.add(entry.getName() + (entry.isDirectory() ? "/" : ""));
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read zip contents", e);
        }
        
        return fileList;
    }

    /**
     * 获取当前处理进度
     */
    public static int getNowProcess() {
        return nowProcess;
    }

    /**
     * 获取当前正在处理的文件名
     */
    public static String getNowProcessFileName() {
        return nowProcessFileName;
    }

    // 私有辅助方法
    private static void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    fileList.add(file); // 添加目录条目
                    getAllFiles(file, fileList);
                } else {
                    fileList.add(file);
                }
            }
        }
    }

    private static long calculateTotalSize(List<File> files) {
        long total = 0;
        for (File file : files) {
            if (!file.isDirectory()) {
                total += file.length();
            }
        }
        return total;
    }

    private static long getZipTotalSize(String zipPath) throws IOException {
        long total = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    total += entry.getSize();
                }
                zis.closeEntry();
            }
        }
        return total;
    }

    private static String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(basePath)) {
            String relativePath = filePath.substring(basePath.length());
            // 处理目录分隔符和开头的分隔符
            relativePath = relativePath.replace(File.separator, "/");
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            
            if (file.isDirectory() && !relativePath.endsWith("/")) {
                relativePath += "/";
            }
            
            return relativePath;
        }
        return "";
    }

    private static void resetProcess() {
        nowProcess = 0;
        nowProcessFileName = "";
    }
}