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

package com.manager.ssb.dialog;

import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.MainActivity;

import java.io.File;

public class MoveDialog {

    public interface OnMoveCallback {
        void onMoveSuccess(File newFile);
    }

    public static void show(@NonNull Context context,
                            @NonNull FileItem fileItem,
                            @NonNull NotifyingExecutorService executorService,
                            @NonNull ActivePanel activePanel,
                            @NonNull OnMoveCallback callback) {

        boolean isLeftPanel;
        MainActivity activity = (MainActivity) context;
        String target;
        if (activePanel == ActivePanel.LEFT) {
            isLeftPanel = true;
            target = activity.getRightDir().getAbsolutePath();
        } else {
            isLeftPanel = false;
            target = activity.getLeftDir().getAbsolutePath();
        }
        
        EditText input = new EditText(context);
        input.setHint(context.getString(R.string.enter_new_path));
        input.setText(target);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.move_file)
                .setMessage(context.getString(R.string.enter_target_directory))
                .setView(input)
                .setPositiveButton(R.string.move, (dialog, which) -> {
                    String targetDirPath = input.getText().toString().trim();
                    if (targetDirPath.isEmpty()) {
                        Toast.makeText(context, R.string.invalid_path, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    File srcFile = fileItem.getFile();
                    File targetDir = new File(targetDirPath);
                    File targetFile = new File(targetDir, srcFile.getName());

                    // 新增：检测目标目录不能是自己或子目录
                    if (srcFile.isDirectory()) {
                        try {
                            String srcCanonical = srcFile.getCanonicalPath();
                            String targetCanonical = targetFile.getCanonicalPath();

                            if (targetCanonical.equals(srcCanonical) ||
                                targetCanonical.startsWith(srcCanonical + File.separator)) {
                                Toast.makeText(context,
                                    R.string.move_into_self_error,
                                    Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch (Exception e) {
                            // 解析路径失败时忽略，不阻止操作
                        }
                    }

                    SimpleProcessingDialog progressDialog = new SimpleProcessingDialog(context);
                    progressDialog.show();

                    executorService.execute(() -> {
                        boolean success = srcFile.renameTo(targetFile);

                        ((android.app.Activity) context).runOnUiThread(() -> {
                            progressDialog.dismiss();
                            if (success) {
                                callback.onMoveSuccess(targetFile);
                                Toast.makeText(context, R.string.move_success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, R.string.move_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }, TaskTypes.MOVE_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

}

