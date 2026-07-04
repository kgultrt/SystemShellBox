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
import com.manager.ssb.core.HandlerRegistry;
import com.manager.ssb.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UnknownFileHandler implements FileHandler {

    @Override
    public void handle(Context context, String filePath, String fileName) {
        // 获取所有已注册的处理器（不包括自身，因为自身不会注册）
        List<FileHandler> allHandlers = HandlerRegistry.getAllHandlers();
        showOpenWithDialog(context, filePath, fileName, allHandlers);
    }

    @Override
    public int getDisplayNameResId() {
        // UnknownFileHandler 不显示在菜单中，返回 0
        return 0;
    }

    private void showOpenWithDialog(final Context context, final String filePath,
                                    final String fileName, final List<FileHandler> handlers) {
        // 动态构建选项名称列表
        final List<String> optionNames = new ArrayList<>();
        final List<FileHandler> handlerMapping = new ArrayList<>(handlers); // 用于点击时定位

        for (FileHandler handler : handlers) {
            int resId = handler.getDisplayNameResId();
            if (resId != 0) {
                optionNames.add(context.getString(resId));
            } else {
                // 如果处理器没有提供显示名称，用类名作为后备
                optionNames.add(handler.getClass().getSimpleName());
            }
        }

        // 添加系统默认和取消选项（它们不是 FileHandler）
        optionNames.add(context.getString(R.string.option_system_default));
        optionNames.add(context.getString(R.string.cancel));

        // 构建对话框
        String title = context.getString(R.string.open_with_title, fileName);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title);

        ListView listView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, optionNames);
        listView.setAdapter(adapter);
        builder.setView(listView);

        final AlertDialog dialog = builder.create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();

                if (position < handlerMapping.size()) {
                    // 用户选择了一个处理器
                    FileHandler selectedHandler = handlerMapping.get(position);
                    selectedHandler.handle(context, filePath, fileName);
                } else if (position == handlerMapping.size()) {
                    // 系统默认
                    openWithSystemDefault(context, filePath);
                }
                // 最后一个位置是取消，不做任何事
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