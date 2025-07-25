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
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CopyDialog {

    private static class ConflictPolicy {
        int action = FileConflictDialog.ConflictResolution.OVERWRITE;
        boolean applyToAll = false;
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
                            // 解析路径失败时忽略
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
                                             OnCopyCallback callback) throws Exception {
    final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 使用 final 数组包装 destFile，使其可以在 lambda 中修改
    final File[] destFileHolder = new File[]{destFile};
    
    // 复制源文件到目标路径
    int result = NativeFileOperation.copy(
        srcFile.getAbsolutePath(), 
        destFileHolder[0].getAbsolutePath(),
        (currentFile, copied, total, status) -> {
            // 当遇到冲突时
            if (status == NativeFileOperation.STATUS_CONFLICT) {
                // 创建工作线程等待对象
                final Object lock = new Object();
                final AtomicInteger resolution = new AtomicInteger(NativeFileOperation.ConflictAction.OVERWRITE);
                final AtomicBoolean userResponded = new AtomicBoolean(false);
                
                // 在UI线程显示冲突对话框
                mainHandler.post(() -> {
                    FileConflictDialog.show(progressDialog.getContext(), 
                        new File(currentFile).getName(), 
                        (action, applyToAll) -> {
                            policy.action = action;
                            policy.applyToAll = applyToAll;
                            resolution.set(action);
                            
                            synchronized (lock) {
                                userResponded.set(true);
                                lock.notifyAll();
                            }
                        });
                });
                
                // 阻塞当前工作线程直到用户响应
                synchronized (lock) {
                    while (!userResponded.get()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                
                // 根据用户选择执行操作
                switch (resolution.get()) {
                    case NativeFileOperation.ConflictAction.OVERWRITE:
                        NativeFileOperation.delete(destFileHolder[0].getAbsolutePath());
                        break;
                        
                    case NativeFileOperation.ConflictAction.KEEP_BOTH:
                        // 生成唯一文件名并更新 destFileHolder
                        destFileHolder[0] = FileUtils.generateUniqueFileName(destFileHolder[0]);
                        break;
                        
                    case NativeFileOperation.ConflictAction.SKIP:
                        return;
                }
            }
            
            // 更新进度
            progressDialog.updateProgress(currentFile, copied, total);
        });
    
    // 处理最终结果
    mainHandler.post(() -> {
        progressDialog.dismiss();
        
        if (result == NativeFileOperation.STATUS_SUCCESS) {
            showToast(progressDialog.getContext(), 
                     progressDialog.getContext().getString(R.string.copy_success));
            callback.onCopySuccess(destFileHolder[0]);
        } else {
            // 失败时清理目标文件
            if (destFileHolder[0].exists()) {
                NativeFileOperation.delete(destFileHolder[0].getAbsolutePath());
            }
            showError(progressDialog.getContext(), 
                     progressDialog.getContext().getString(R.string.copy_failed));
        }
    });
}

    
    // 辅助方法
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