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