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
package com.manager.ssb.core.openmethod;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.manager.ssb.R;
import com.manager.ssb.Application;
import com.manager.ssb.core.FileHandler;
import com.manager.ssb.core.editor.MainActivity;

import java.util.Arrays;
import java.util.List;

public class CompressFileHandler implements FileHandler {
    // 支持的压缩格式列表
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "zip", "tar", "gz", "bz2", "7z", "rar"
    );

    @Override
    public void handle(Context context, String filePath, String fileName) {
        // 提取小写格式后缀（考虑带点的格式如".tar.gz"）
        String format = extractFormat(fileName.toLowerCase());
        
        if (SUPPORTED_FORMATS.contains(format)) {
            String formatMessage = Application.getAppContext().getString(
                R.string.compress_format_detected, format.toUpperCase()
            );
            Toast.makeText(context, formatMessage, Toast.LENGTH_SHORT).show();
        } else {
            // 添加额外的无效格式提示
            String invalidMsg = Application.getAppContext().getString(
                R.string.unsupported_compress_format
            );
            Toast.makeText(context, invalidMsg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 智能提取压缩格式（优先检测复合格式）
     */
    private String extractFormat(String fileName) {
        // 优先检测双扩展名格式
        if (fileName.endsWith(".tar.gz")) return "tar.gz";
        if (fileName.endsWith(".tar.bz2")) return "tar.bz2";
        
        // 标准单扩展名提取
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1) : "";
    }
}