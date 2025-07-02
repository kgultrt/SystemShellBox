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

// TextFileHandler.java (具体的文件处理器)
package com.manager.ssb.core.openmethod;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.manager.ssb.core.FileHandler;
import com.manager.ssb.core.editor.MainActivity;

public class TextFileHandler implements FileHandler {
    @Override
    public void handle(Context context, String filePath, String fileName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("file_path", filePath);
        intent.putExtra("file_name", fileName);
        context.startActivity(intent);
    }
}