package com.manager.ssb.util;

import java.util.Locale;
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
}