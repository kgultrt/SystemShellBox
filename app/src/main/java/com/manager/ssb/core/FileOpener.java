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

package com.manager.ssb.core;

import android.content.Context;
import android.util.Log;

import com.manager.ssb.core.openmethod.UnknownFileHandler;

public class FileOpener {
    private static final String TAG = "FileOpener";
    private static final FileHandler DEFAULT_HANDLER = new UnknownFileHandler();

    /**
     * 打开文件：根据文件类型选择已注册的处理器，若无对应处理器则使用 UnknownFileHandler 兜底。
     */
    public static void openFile(Context context, String filePath, String fileName) {
        if (context == null || filePath == null) {
            Log.e(TAG, "Invalid parameters: context or filePath is null");
            return;
        }

        try {
            FileType fileType = FileTypeRegistry.getFileType(filePath);
            FileHandler handler = HandlerRegistry.getHandlerForType(fileType);

            if (handler == null) {
                Log.w(TAG, "No handler found for file type: " + fileType + ", using UnknownFileHandler");
                handler = DEFAULT_HANDLER;
            }

            Log.d(TAG, "Opening file: " + fileName + " with type: " + fileType);
            handler.handle(context, filePath, fileName);

        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + filePath, e);
            DEFAULT_HANDLER.handle(context, filePath, fileName);
        }
    }
}