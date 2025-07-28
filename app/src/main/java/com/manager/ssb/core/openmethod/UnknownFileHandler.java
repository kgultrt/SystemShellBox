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
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.core.FileHandler;
import com.manager.ssb.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnknownFileHandler implements FileHandler {
    private static final Map<String, FileHandler> CUSTOM_HANDLERS = new HashMap<>();

    static {
        CUSTOM_HANDLERS.put("text_editor", new TextFileHandler());
        CUSTOM_HANDLERS.put("audio_player", new AudioFileHandler());
        CUSTOM_HANDLERS.put("compressed_file_viewer", new CompressFileHandler());
        CUSTOM_HANDLERS.put("browser", new HtmlFileHandler());
    }

    @Override
    public void handle(Context context, String filePath, String fileName) {
        showOpenWithDialog(context, filePath, fileName);
    }

    private void showOpenWithDialog(final Context context, final String filePath, final String fileName) {
        // 创建选项列表（使用字符串资源）
        final List<String> options = new ArrayList<>();
        options.add(context.getString(R.string.option_text_editor));    // 文本编辑器
        options.add(context.getString(R.string.option_audio_player));   // 音频播放器
        options.add(context.getString(R.string.option_compressed_file_viewer));   // 压缩文件查看器
        options.add(context.getString(R.string.option_browser));   // 内置浏览器
        options.add(context.getString(R.string.option_system_default)); // 系统默认方式
        options.add(context.getString(R.string.cancel));                // 取消

        // 使用字符串资源设置标题
        String title = context.getString(R.string.open_with_title, fileName);
        
        // 使用MaterialAlertDialogBuilder
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title);

        ListView listView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, options);
        listView.setAdapter(adapter);
        builder.setView(listView);

        final AlertDialog dialog = builder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                String selectedOption = options.get(position);
                
                // 使用资源ID进行比较
                if (selectedOption.equals(context.getString(R.string.option_text_editor))) {
                    CUSTOM_HANDLERS.get("text_editor").handle(context, filePath, fileName);
                } else if (selectedOption.equals(context.getString(R.string.option_audio_player))) {
                    CUSTOM_HANDLERS.get("audio_player").handle(context, filePath, fileName);
                } else if (selectedOption.equals(context.getString(R.string.option_compressed_file_viewer))) {
                    CUSTOM_HANDLERS.get("compressed_file_viewer").handle(context, filePath, fileName);
                } else if (selectedOption.equals(context.getString(R.string.option_browser))) {
                    CUSTOM_HANDLERS.get("browser").handle(context, filePath, fileName);
                } else if (selectedOption.equals(context.getString(R.string.option_system_default))) {
                    openWithSystemDefault(context, filePath);
                }
                // 取消选项不需要处理
            }
        });

        dialog.show();
    }

    private void openWithSystemDefault(Context context, String filePath) {
        File file = new File(filePath);
        Uri uri;
        
        try {
            String authority = context.getPackageName() + ".fileprovider";
            uri = FileProvider.getUriForFile(context, authority, file);
        } catch (Exception e) {
            uri = Uri.fromFile(file);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }
}