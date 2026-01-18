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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;

public class ZipUtils {
    private static volatile int nowProcess = 0;
    private static volatile String nowProcessFileName = "";
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB buffer

    /**
     * 压缩文件或目录（修复版）
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
            // 如果压缩的是目录，首先创建根目录条目
            if (source.isDirectory()) {
                String rootEntryName = source.getName();
                if (!rootEntryName.endsWith("/")) {
                    rootEntryName += "/";
                }
                zos.putNextEntry(new ZipEntry(rootEntryName));
                zos.closeEntry();
            }

            for (File file : fileList) {
                nowProcessFileName = file.getName();
                
                String entryName = getRelativePath(source, file);
                if (entryName.isEmpty()) continue;

                // 确保目录以"/"结尾
                if (file.isDirectory() && !entryName.endsWith("/")) {
                    entryName += "/";
                }

                ZipEntry zipEntry = new ZipEntry(entryName);
                
                // 设置时间戳
                zipEntry.setTime(file.lastModified());
                
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
                String entryName = entry.getName();
                
                // 修复：正确处理目录和文件名
                File newFile = new File(destPath, entryName);
                
                // 防止zip slip漏洞
                String canonicalPath = newFile.getCanonicalPath();
                if (!canonicalPath.startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new SecurityException("Entry is outside of the target dir: " + entryName);
                }

                // 如果是目录，直接创建
                if (entryName.endsWith("/")) {
                    newFile.mkdirs();
                } else {
                    // 确保父目录存在
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
     * 查看zip文件内容（修复版）
     * 返回完整的路径列表，目录以"/"结尾
     */
    public static List<String> zipcat(String zipPath) {
        List<String> fileList = new ArrayList<>();
        
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // 确保目录以"/"结尾
                if (entry.isDirectory() && !entryName.endsWith("/")) {
                    entryName += "/";
                }
                
                fileList.add(entryName);
            }
        } catch (IOException e) {
            // 尝试使用旧方法作为备选
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (entry.isDirectory() && !entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    fileList.add(entryName);
                    zis.closeEntry();
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to read zip contents", ex);
            }
        }
        
        // 按字母排序，目录在前
        fileList.sort((a, b) -> {
            boolean aIsDir = a.endsWith("/");
            boolean bIsDir = b.endsWith("/");
            if (aIsDir && !bIsDir) return -1;
            if (!aIsDir && bIsDir) return 1;
            return a.compareTo(b);
        });
        
        return fileList;
    }

    /**
     * 获取压缩文件中指定路径的内容
     */
    public static List<String> listZipContents(String zipPath, String internalPath) {
        List<String> result = new ArrayList<>();
        
        // 确保内部路径以"/"结尾（如果是目录的话）
        if (internalPath != null && !internalPath.isEmpty() && !internalPath.endsWith("/")) {
            internalPath += "/";
        }
        
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // 跳过不匹配的条目
                if (internalPath != null && !internalPath.isEmpty()) {
                    if (!entryName.startsWith(internalPath)) {
                        continue;
                    }
                    // 跳过自身
                    if (entryName.equals(internalPath)) {
                        continue;
                    }
                }
                
                // 获取相对路径
                String relativePath = internalPath != null && !internalPath.isEmpty() 
                    ? entryName.substring(internalPath.length()) 
                    : entryName;
                
                // 如果为空，跳过
                if (relativePath.isEmpty()) continue;
                
                // 只添加直接子项
                int slashIndex = relativePath.indexOf('/');
                if (slashIndex > 0) {
                    // 这是一个子目录
                    String dirName = relativePath.substring(0, slashIndex + 1);
                    if (!result.contains(dirName)) {
                        result.add(dirName);
                    }
                } else if (slashIndex == -1) {
                    // 这是一个文件
                    result.add(relativePath);
                }
                // slashIndex == 0 的情况应该不会发生
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list zip contents", e);
        }
        
        return result;
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

    /**
     * 获取压缩文件总大小（修复版）
     */
    private static long getZipTotalSize(String zipPath) throws IOException {
        long total = 0;
        try (ZipFile zipFile = new ZipFile(zipPath)) {
            java.util.Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    long size = entry.getSize();
                    if (size > 0) {
                        total += size;
                    } else {
                        // 如果大小未知，尝试使用压缩大小
                        long compressedSize = entry.getCompressedSize();
                        if (compressedSize > 0) {
                            total += compressedSize;
                        }
                    }
                }
            }
        }
        return total;
    }

    // 其他辅助方法保持不变，但我会修复 getRelativePath
    private static void getAllFiles(File dir, List<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 添加目录本身
                    fileList.add(file);
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
            
            // 如果是目录且不以"/"结尾，添加"/"
            if (file.isDirectory() && !relativePath.isEmpty() && !relativePath.endsWith("/")) {
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
    
    /**
     * 调试方法：打印ZIP文件详细内容
     */
    public static void debugZipContents(String zipPath) {
        System.out.println("=== ZIP File: " + zipPath + " ===");
        try {
            List<String> contents = zipcat(zipPath);
            for (int i = 0; i < contents.size(); i++) {
                System.out.printf("%3d. %s%n", i + 1, contents.get(i));
            }
            System.out.println("=== Total: " + contents.size() + " entries ===");
        } catch (Exception e) {
            System.err.println("Error reading zip: " + e.getMessage());
        }
    }
}