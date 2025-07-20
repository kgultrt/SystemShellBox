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

// FileOpener.java
package com.manager.ssb.core;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import com.manager.ssb.core.openmethod.AudioFileHandler;
import com.manager.ssb.core.openmethod.TextFileHandler;
import com.manager.ssb.core.openmethod.CompressFileHandler;
import com.manager.ssb.core.openmethod.UnknownFileHandler;


public class FileOpener {
    private static final Map<FileType, FileHandler> HANDLER_MAP = new HashMap<>();

    static {
        HANDLER_MAP.put(FileType.AUDIO, new AudioFileHandler());
        HANDLER_MAP.put(FileType.TEXT, new TextFileHandler());
        HANDLER_MAP.put(FileType.COMPRESS, new CompressFileHandler());
        HANDLER_MAP.put(FileType.UNKNOWN, new UnknownFileHandler());
    }

    public static void openFile(Context context, String filePath, String fileName) {
        FileType fileType = FileTypeRegistry.getFileType(filePath);
        FileHandler handler = HANDLER_MAP.getOrDefault(fileType, HANDLER_MAP.get(FileType.UNKNOWN));
        handler.handle(context, filePath, fileName);
    }
}