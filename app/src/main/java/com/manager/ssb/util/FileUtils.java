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

package com.manager.ssb.util;

import java.util.Locale;
import java.io.File;

import com.manager.ssb.Application;
import com.manager.ssb.R;

public class FileUtils {

    public static String getShortPath(String fullPath) {
        if (fullPath == null || fullPath.length() < 40) {
            return fullPath;
        }
        return "..." + fullPath.substring(fullPath.length() - 37);
    }

    public static String getShortName(String fileName) {
        if (fileName == null || fileName.length() < 30) {
            return fileName;
        }
        return fileName.substring(0, 15) + "..." + fileName.substring(fileName.length() - 10);
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        
        double kb = size / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        
        double gb = mb / 1024.0;
        return String.format(Locale.US, "%.1f GB", gb);
    }
    
    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + Application.getAppContext().getString(R.string.second);
        }
        
        long minutes = seconds / 60;
        seconds %= 60;
        
        if (minutes < 60) {
            return String.format(Locale.US, "%d" + Application.getAppContext().getString(R.string.minute) + "%02d" + Application.getAppContext().getString(R.string.second), minutes, seconds);
        }
        
        long hours = minutes / 60;
        minutes %= 60;
        return String.format(Locale.US, "%d" + Application.getAppContext().getString(R.string.hour) + "%02d" + Application.getAppContext().getString(R.string.minute), hours, minutes);
    }
    
    // 生成唯一文件名
    public static File generateUniqueFileName(File file) {
        String name = file.getName();
        String extension = "";
        String baseName = name;
        
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = name.substring(dotIndex);
            baseName = name.substring(0, dotIndex);
        }
        
        int counter = 1;
        File newFile;
        do {
            String newName = baseName + "_" + counter + extension;
            newFile = new File(file.getParent(), newName);
            counter++;
        } while (newFile.exists());
        
        return newFile;
    }
    
    public static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null) return false;
        
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        
        return fileOrDirectory.delete();
    }
}