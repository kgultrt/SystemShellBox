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
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.MainActivity;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.util.FileUtils;
import com.manager.ssb.util.NativeFileOperation;
import com.manager.ssb.util.StepFileCopier;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopyDialog {

    public static class ConflictPolicy {
        public int action = NativeFileOperation.ConflictAction.OVERWRITE;
        public boolean applyToAll = false;
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
                    
                    // 检测目标目录不能是自己或子目录
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
                            Log.e("CopyDialog", "Error resolving path", e);
                        }
                    }
                    
                    // 创建冲突策略对象
                    ConflictPolicy policy = new ConflictPolicy();
                    
                    CopyProgressDialog progressDialog = new CopyProgressDialog(context);
                    progressDialog.show(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), 
                                      srcFile.getName());
                    
                    executorService.execute(() -> {
                        try {
                            copyWithConflictHandling(srcFile, destFile, progressDialog, policy, callback);
                        } catch (Exception e) {
                            Log.e("CopyDialog", "Copy failed", e);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                progressDialog.dismiss();
                                showError(context, context.getString(R.string.copy_failed) + 
                                          ": " + e.getMessage());
                            });
                        }
                    }, TaskTypes.COPY_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private static void copyWithConflictHandling(File srcFile, File destFile,
                                             CopyProgressDialog progressDialog,
                                             ConflictPolicy policy,
                                             OnCopyCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final AtomicBoolean canceled = new AtomicBoolean(false);
        
        progressDialog.setOnCancelListener(() -> canceled.set(true));
        
        // 为后台线程保存进度对话框上下文
        CopyProgressDialog.setLastContext(progressDialog.getContext());
        
        new Thread(() -> {
            int finalResult = NativeFileOperation.STATUS_SUCCESS;
            final int[] resultHolder = new int[]{NativeFileOperation.STATUS_SUCCESS};
            final File[] destHolder = new File[]{destFile};
            
            // 创建进度回调适配器
            NativeFileOperation.ProgressCallback progressCallback = 
                (currentFile, copied, total, status) -> 
                    mainHandler.post(() -> 
                        progressDialog.updateProgress(currentFile, copied, total, status)
                    );
            
            try {
                if (srcFile.isFile()) {
                    finalResult = StepFileCopier.copyFileWithRetry(
                        srcFile.getAbsolutePath(),
                        destFile.getAbsolutePath(),
                        policy,
                        progressCallback
                    );
                } else if (srcFile.isDirectory()) {
                    finalResult = copyDirectoryWithConflict(
                        srcFile, 
                        destFile, 
                        policy,
                        progressCallback
                    );
                }
            } catch (Exception e) {
                finalResult = NativeFileOperation.STATUS_ERROR;
                Log.e("CopyDialog", "Error during copy", e);
            }
            
            // 最终结果处理
            mainHandler.post(() -> {
                progressDialog.dismiss();
                
                if (resultHolder[0] == NativeFileOperation.STATUS_SUCCESS) {
                    showToast(progressDialog.getContext(), 
                             progressDialog.getContext().getString(R.string.copy_success));
                    callback.onCopySuccess(destHolder[0]);
                } else if (resultHolder[0] == NativeFileOperation.STATUS_SKIPPED) {
                    // 部分文件被跳过
                    showToast(progressDialog.getContext(), 
                             progressDialog.getContext().getString(R.string.copy_partially_completed));
                    callback.onCopySuccess(destHolder[0]);
                } else {
                    // 失败时清理目标文件
                    FileUtils.deleteRecursive(destHolder[0]);
                    showError(progressDialog.getContext(), 
                             progressDialog.getContext().getString(R.string.copy_failed));
                }
            });
        }).start();
    }
    
    private static int copyDirectoryWithConflict(File srcDir, File destDir,
                                             ConflictPolicy policy,
                                             NativeFileOperation.ProgressCallback callback) {
        // 创建目标目录（如果不存在）
        if (!destDir.exists()) {
            if (!destDir.mkdirs()) {
                callback.onProgress(srcDir.getName(), 0, 0, NativeFileOperation.STATUS_ERROR);
                return NativeFileOperation.STATUS_ERROR;
            }
        }
        
        File[] children = srcDir.listFiles();
        if (children == null) return NativeFileOperation.STATUS_SUCCESS;
        
        for (File child : children) {
            File childDest = new File(destDir, child.getName());
            
            if (child.isDirectory()) {
                int childResult = copyDirectoryWithConflict(child, childDest, policy, callback);
                if (childResult == NativeFileOperation.STATUS_ERROR) {
                    return NativeFileOperation.STATUS_ERROR;
                }
            } else {
                int result = StepFileCopier.copyFileWithRetry(
                    child.getAbsolutePath(),
                    childDest.getAbsolutePath(),
                    policy,
                    callback
                );
                
                if (result == NativeFileOperation.STATUS_ERROR) {
                    return result;
                }
            }
        }
        
        return NativeFileOperation.STATUS_SUCCESS;
    }
    
    private static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    private static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    public interface OnCopyCallback {
        void onCopySuccess(File copiedFile);
    }
}