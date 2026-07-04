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
package com.manager.ssb.core.openmethod;

import android.content.Context;

import com.manager.ssb.R;
import com.manager.ssb.core.FileHandler;
import com.manager.ssb.MainActivity;
import com.manager.ssb.enums.ActivePanel;

public class CompressFileHandler implements FileHandler {

    @Override
    public void handle(Context context, String filePath, String fileName) {
        MainActivity activity = (MainActivity) context;
        ActivePanel activePanel = activity.activePanel;
        String format = extractFormat(fileName.toLowerCase());
        activity.getCompressFileManager().enterCompressFile(filePath, activePanel);
    }

    private String extractFormat(String fileName) {
        if (fileName.endsWith(".tar.gz")) return "tar.gz";
        if (fileName.endsWith(".tar.bz2")) return "tar.bz2";
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1) : "";
    }

    @Override
    public int getDisplayNameResId() {
        return R.string.option_compressed_file_viewer;
    }
}