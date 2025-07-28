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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.core.browser.HtmlActivity;
import com.manager.ssb.core.FileHandler;
import java.io.File;

public class HtmlFileHandler implements FileHandler {

    @Override
    public void handle(Context context, String filePath, String fileName) {
        // 创建选项列表
        CharSequence[] options = new CharSequence[]{
                context.getString(R.string.edit),               // 编辑
                context.getString(R.string.internal_browser) + " " + context.getString(R.string.not_recommended), // 内置浏览器（不推荐）
                context.getString(R.string.external_browser),    // 外部浏览器
                context.getString(R.string.cancel)              // 取消
        };

        // 构建MD3风格对话框
        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.html_file_action, fileName))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // 编辑
                            new TextFileHandler().handle(context, filePath, fileName);
                            break;
                        case 1: // 内置浏览器（不推荐）
                            openInternalBrowser(context, filePath, fileName);
                            break;
                        case 2: // 外部浏览器
                            openExternalBrowser(context, filePath);
                            break;
                        // 取消选项不做任何操作
                    }
                })
                .show();
    }

    private void openInternalBrowser(Context context, String filePath, String fileName) {
        Intent intent = new Intent(context, HtmlActivity.class);
        intent.putExtra("file_path", filePath);
        intent.putExtra("file_name", fileName);
        context.startActivity(intent);
    }

    private void openExternalBrowser(Context context, String filePath) {
        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_SHORT).show();
        }
    }
}