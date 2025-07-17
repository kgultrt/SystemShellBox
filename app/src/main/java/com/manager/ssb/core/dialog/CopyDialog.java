package com.manager.ssb.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.Application;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.MainActivity;
import com.manager.ssb.util.NativeFileOperation;
import java.io.File;

public class CopyDialog {

    public interface OnCopyCallback {
        void onCopySuccess(File copiedFile);
    }

    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    private static void showError(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }

    public static void show(@NonNull Context context,
                            @NonNull FileItem fileItem,
                            @NonNull NotifyingExecutorService executorService,
                            @NonNull ActivePanel activePanel,
                            @NonNull OnCopyCallback callback) {

        MainActivity activity = (MainActivity) context;
        String target = activePanel == ActivePanel.LEFT ? 
                        activity.getRightDir().getAbsolutePath() : 
                        activity.getLeftDir().getAbsolutePath();

        EditText input = new EditText(context);
        input.setHint(R.string.enter_new_path);
        input.setText(target);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.copy_file)
                .setMessage(context.getString(R.string.enter_target_directory))
                .setView(input)
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    String targetDir = input.getText().toString().trim();
                    if (targetDir.isEmpty()) {
                        showError(context, context.getString(R.string.invalid_path));
                        return;
                    }

                    File srcFile = fileItem.getFile();
                    File destFile = new File(targetDir, srcFile.getName());
                    
                    CopyProgressDialog progressDialog = new CopyProgressDialog(context);
                    progressDialog.show(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), 
                                       srcFile.getName());
                    
                    executorService.execute(() -> {
                        boolean success = copyItem(fileItem, destFile, callback, progressDialog);
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progressDialog.dismiss();
                            if (!success) {
                                NativeFileOperation.delete(destFile.getAbsolutePath());
                                showError(context, context.getString(R.string.copy_failed));
                            }
                        });
                    }, TaskTypes.COPY_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private static boolean copyItem(FileItem fileItem, File destFile, 
                                   OnCopyCallback callback, CopyProgressDialog progressDialog) {
        try {
            boolean success = NativeFileOperation.copy(
                fileItem.getFile().getAbsolutePath(), 
                destFile.getAbsolutePath(),
                (copied, total) -> {
                    // 进度更新回调
                    progressDialog.updateProgress(copied, total, fileItem.getFile().getName());
                }
            );
            
            if (success) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onCopySuccess(destFile);
                    showToast(Application.getAppContext(), Application.getAppContext().getString(R.string.copy_success));
                });
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }
}