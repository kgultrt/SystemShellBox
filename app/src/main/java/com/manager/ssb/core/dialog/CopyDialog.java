package com.manager.ssb.dialog;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Stack;

public class CopyDialog {

    public interface OnCopyCallback {
        void onCopySuccess(File copiedFile);
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
        input.setHint(context.getString(R.string.enter_new_path));
        input.setText(target);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.copy_file)
                .setMessage(context.getString(R.string.enter_target_directory))
                .setView(input)
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    String targetDirPath = input.getText().toString().trim();
                    if (targetDirPath.isEmpty()) {
                        showError(context, context.getString(R.string.invalid_path));
                        return;
                    }

                    File srcFile = fileItem.getFile();
                    File targetDir = new File(targetDirPath);

                    // 尝试创建目标目录（如果不存在）
                    if (!targetDir.exists() && !targetDir.mkdirs()) {
                        // 仅当创建失败且目录仍不存在时才报错
                        if (!targetDir.exists()) {
                            showError(context, context.getString(R.string.dir_create_failed));
                            return;
                        }
                    }

                    // 检查目标目录是否可写（实际测试）
                    if (!isDirectoryWritable(targetDir)) {
                        showError(context, context.getString(R.string.dir_not_writable));
                        return;
                    }

                    try {
                        // 防止复制到自身
                        if (srcFile.getCanonicalPath().equals(targetDir.getCanonicalPath())) {
                            showError(context, context.getString(R.string.copy_into_self_error));
                            return;
                        }
                    } catch (IOException e) {
                        // 路径比较失败不是致命错误，继续操作
                        Log.w("CopyDialog", "Path comparison error", e);
                    }

                    File targetFile = new File(targetDir, srcFile.getName());
                    CopyProgressDialog progressDialog = new CopyProgressDialog(context);
                    progressDialog.show(srcFile.getAbsolutePath(), targetFile.getAbsolutePath(), srcFile.getName());

                    executorService.execute(() -> {
                        try {
                            // 直接执行复制操作，错误在复制过程中处理
                            boolean success = copyItem(srcFile, targetFile, progressDialog);
                            
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                progressDialog.dismiss();
                                if (success) {
                                    callback.onCopySuccess(targetFile);
                                    showToast(context, R.string.copy_success);
                                } else {
                                    // 不显示具体错误，只显示通用失败提示
                                    showToast(context, R.string.copy_failed);
                                    // 清理不完整的文件
                                    deleteRecursive(targetFile);
                                }
                            });
                        } catch (Exception e) {
                            Log.e("CopyDialog", "Unexpected copy error", e);
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                progressDialog.dismiss();
                                showToast(context, R.string.copy_failed);
                                deleteRecursive(targetFile);
                            });
                        }
                    }, TaskTypes.COPY_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 简化复制核心方法 - 专注于实际复制操作
     */
    private static boolean copyItem(File src, File dest, CopyProgressDialog progressDialog) {
        if (src.isFile()) {
            return copyFileWithChannel(src, dest, progressDialog);
        }
        
        // 目录复制逻辑
        Stack<CopyTask> taskStack = new Stack<>();
        taskStack.push(new CopyTask(src, dest));
        
        while (!taskStack.isEmpty()) {
            CopyTask task = taskStack.pop();
            File sourceDir = task.source;
            File targetDir = task.target;
            
            // 尝试创建目标目录
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                // 如果创建失败但目录已存在，继续操作
                if (!targetDir.exists()) return false;
            }
            
            File[] children = sourceDir.listFiles();
            if (children == null) continue;
            
            for (int i = children.length - 1; i >= 0; i--) {
                File child = children[i];
                File destChild = new File(targetDir, child.getName());
                
                if (child.isDirectory()) {
                    taskStack.push(new CopyTask(child, destChild));
                } else {
                    if (!copyFileWithChannel(child, destChild, progressDialog)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    /**
     * 文件复制核心 - 专注于实际复制操作
     */
    private static boolean copyFileWithChannel(File src, File dest, CopyProgressDialog progressDialog) {
        // 创建父目录（如果不存在）
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            // 如果创建失败但目录已存在，继续操作
            if (!parent.exists()) return false;
        }

        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest);
             FileChannel inChannel = fis.getChannel();
             FileChannel outChannel = fos.getChannel()) {

            long totalBytes = src.length();
            long transferred = 0;
            
            // 32MB缓冲区
            final int MAX_CHUNK = 32 * 1024 * 1024;
            
            while (transferred < totalBytes) {
                long remaining = totalBytes - transferred;
                long chunk = Math.min(MAX_CHUNK, remaining);
                
                long bytes = inChannel.transferTo(transferred, chunk, outChannel);
                if (bytes <= 0) break;
                
                transferred += bytes;
                
                // 更新进度
                progressDialog.updateProgress(transferred, totalBytes, src.getName());
            }
            
            return transferred == totalBytes;
        } catch (IOException e) {
            Log.e("CopyDialog", "File copy error: " + src.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 实际测试目录是否可写
     */
    private static boolean isDirectoryWritable(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return false;
        
        // 尝试创建一个测试文件来验证写权限
        File testFile = new File(dir, ".write_test_" + System.currentTimeMillis());
        try {
            if (testFile.createNewFile()) {
                testFile.delete();
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static void showRenameDialog(Context context, File targetFile, 
                                        NotifyingExecutorService executorService, 
                                        OnCopyCallback callback, File srcFile) {
        EditText renameInput = new EditText(context);
        renameInput.setHint(context.getString(R.string.enter_new_name));
        renameInput.setText(targetFile.getName());

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.file_exists)
                .setMessage(context.getString(R.string.rename_file_prompt))
                .setView(renameInput)
                .setPositiveButton(R.string.rename, (dialog, which) -> {
                    String newName = renameInput.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(context, R.string.invalid_name, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    File newTargetFile = getNewFileName(targetFile.getParentFile(), newName);
                    CopyProgressDialog progressDialog = new CopyProgressDialog(context);
                    progressDialog.show(srcFile.getAbsolutePath(), newTargetFile.getAbsolutePath(), srcFile.getName());

                    executorService.execute(() -> {
                        try {
                            boolean success = copyItem(srcFile, newTargetFile, progressDialog);
                            
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                progressDialog.dismiss();
                                if (success) {
                                    callback.onCopySuccess(newTargetFile);
                                    Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, R.string.copy_failed, Toast.LENGTH_SHORT).show();
                                    deleteRecursive(newTargetFile);
                                }
                            });
                        } catch (Exception e) {
                            Log.e("CopyDialog", "Rename copy error", e);
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(context, R.string.copy_failed, Toast.LENGTH_SHORT).show();
                                deleteRecursive(newTargetFile);
                            });
                        }
                    }, TaskTypes.COPY_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static File getNewFileName(File parentDir, String baseName) {
        File newFile = new File(parentDir, baseName);
        int count = 1;
        while (newFile.exists()) {
            String newName = baseName + " (" + count + ")";
            newFile = new File(parentDir, newName);
            count++;
        }
        return newFile;
    }

    /**
     * 递归删除文件或目录
     */
    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) {
            return;
        }
        
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
    
    /**
     * 非递归任务结构
     */
    private static class CopyTask {
        final File source;
        final File target;
        
        CopyTask(File source, File target) {
            this.source = source;
            this.target = target;
        }
    }

    // ================= 简化工具方法 =================
    
    /**
     * 显示错误提示
     */
    private static void showError(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }
    
    private static void showToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }
}