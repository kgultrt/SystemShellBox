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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.task.TaskTypes;
import java.io.File;

public class DeleteDialog {

    public interface OnDeleteCallback {
        void onDeleteSuccess(File deletedFile);
    }

    public static void show(Context context, FileItem fileItem, 
                            NotifyingExecutorService executorService, 
                            OnDeleteCallback callback) {
        
        new MaterialAlertDialogBuilder(context)
            .setTitle(R.string.confirm_delete)
            .setMessage(context.getString(R.string.delete_confirmation, fileItem.getName()))
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                SimpleProcessingDialog progressDialog = new SimpleProcessingDialog(context);
                progressDialog.show();

                executorService.execute(() -> {
                    boolean success = deleteRecursive(fileItem.getFile());

                    ((android.app.Activity) context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (success) {
                            callback.onDeleteSuccess(fileItem.getFile());
                            Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }, TaskTypes.DELETE_FILE);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private static boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (!deleteRecursive(child)) return false;
                }
            }
        }
        return file.delete();
    }
}