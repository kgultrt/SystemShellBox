// FileOpener.java
package com.manager.ssb.core;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import com.manager.ssb.core.openmethod.AudioFileHandler;
import com.manager.ssb.core.openmethod.TextFileHandler;
import com.manager.ssb.core.openmethod.UnknownFileHandler;


public class FileOpener {
    private static final Map<FileType, FileHandler> HANDLER_MAP = new HashMap<>();

    static {
        HANDLER_MAP.put(FileType.AUDIO, new AudioFileHandler());
        HANDLER_MAP.put(FileType.TEXT, new TextFileHandler());
        HANDLER_MAP.put(FileType.UNKNOWN, new UnknownFileHandler());
    }

    public static void openFile(Context context, String filePath, String fileName) {
        FileType fileType = FileTypeRegistry.getFileType(filePath);
        FileHandler handler = HANDLER_MAP.getOrDefault(fileType, HANDLER_MAP.get(FileType.UNKNOWN));
        handler.handle(context, filePath, fileName);
    }
}