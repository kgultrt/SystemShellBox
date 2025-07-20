package com.manager.ssb.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.manager.ssb.util.NativeFileOperation;
import java.io.File;

public class CopyDialog {

    private static final String TAG = "CopyDialog";

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
                    
                    // 新增：检测目标目录不能是自己或子目录
                    if (srcFile.isDirectory()) {
                        try {
                            String srcCanonical = srcFile.getCanonicalPath();
                            String targetCanonical = destFile.getCanonicalPath();

                            if (targetCanonical.equals(srcCanonical) ||
                                targetCanonical.startsWith(srcCanonical + File.separator)) {
                                Toast.makeText(context,
                                    R.string.copy_into_self_error,
                                    Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch (Exception e) {
                            // 解析路径失败时忽略，不阻止操作
                        }
                    }
                    
                    CopyProgressDialog progressDialog = new CopyProgressDialog(context);
                    progressDialog.show(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), 
                                      srcFile.getName());
                    
                    executorService.execute(() -> {
                        boolean success = false;
                        Exception error = null;
                        
                        try {
                            // 执行复制操作
                            success = copyItem(fileItem, destFile, callback, progressDialog);
                        } catch (Exception e) {
                            Log.e(TAG, "Copy failed", e);
                            error = e;
                        }
                        
                        // 获取最终状态
                        final boolean finalSuccess = success;
                        final Exception finalError = error;
                        
                        // 在主线程处理结果
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progressDialog.dismiss();
                            
                            if (finalSuccess) {
                                showToast(context, context.getString(R.string.copy_success));
                            } else {
                                // 失败时清理目标文件
                                if (destFile.exists()) {
                                    NativeFileOperation.delete(destFile.getAbsolutePath());
                                }
                                
                                String errorMsg = finalError != null ? 
                                    finalError.getMessage() : 
                                    context.getString(R.string.copy_failed);
                                    
                                showError(context, errorMsg);
                            }
                        });
                    }, TaskTypes.COPY_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private static boolean copyItem(FileItem fileItem, File destFile, 
                                   OnCopyCallback callback, CopyProgressDialog progressDialog) 
                                   throws Exception {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        try {
            boolean success = NativeFileOperation.copy(
                fileItem.getFile().getAbsolutePath(), 
                destFile.getAbsolutePath(),
                (currentFile, copied, total) -> {
                    // 将UI更新任务发送到主线程
                    mainHandler.post(() -> {
                        progressDialog.updateProgress(currentFile, copied, total);
                    });
                }
            );
            
            return success;
        } finally {
            // 确保回调执行，无论成功与否
            mainHandler.post(() -> {
                if (destFile.exists()) {
                    callback.onCopySuccess(destFile);
                }
            });
        }
    }
}