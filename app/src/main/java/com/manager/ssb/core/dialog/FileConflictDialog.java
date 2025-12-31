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

package com.manager.ssb.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.util.NativeFileOperation;
import com.manager.ssb.R;

public class FileConflictDialog {

    public interface ConflictResolutionCallback {
        void onResolutionSelected(int action, boolean applyToAll);
    }
    
    public static void show(Context context, String fileName, 
                          ConflictResolutionCallback callback) {
        // 使用 MD3 对话框构建器
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
            context
        );
        
        View view = LayoutInflater.from(context).inflate(
            R.layout.dialog_file_conflict, 
            null
        );
        
        TextView fileNameText = view.findViewById(R.id.fileNameText);
        CheckBox applyAllCheck = view.findViewById(R.id.applyToAllCheck);
        
        fileNameText.setText(context.getString(R.string.conflict_message, fileName));
        
        builder.setTitle(R.string.file_conflict)
                .setView(view)
                // 根据 MD3 规范调整按钮顺序：确认操作在右侧
                .setPositiveButton(R.string.overwrite, null) // 延迟设置点击事件
                .setNegativeButton(R.string.keep_both, (dialog, which) -> {
                    callback.onResolutionSelected(
                        NativeFileOperation.ConflictAction.KEEP_BOTH,
                        applyAllCheck.isChecked()
                    );
                })
                .setNeutralButton(R.string.skip, (dialog, which) -> {
                    callback.onResolutionSelected(
                        NativeFileOperation.ConflictAction.SKIP,
                        applyAllCheck.isChecked()
                    );
                })
                .setCancelable(true)
                .setOnCancelListener(dialog -> {
                    callback.onResolutionSelected(
                        NativeFileOperation.ConflictAction.SKIP,
                        false
                    );
                });
        
        // 创建对话框并自定义按钮
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            // 为覆盖按钮添加破坏性操作样式
            Button overwriteBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            overwriteBtn.setOnClickListener(v -> {
                callback.onResolutionSelected(
                    NativeFileOperation.ConflictAction.OVERWRITE,
                    applyAllCheck.isChecked()
                );
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
}