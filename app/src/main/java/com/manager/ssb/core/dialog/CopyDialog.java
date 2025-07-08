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

import java.io.*;

public class CopyDialog {

    public interface OnCopyCallback {
        void onCopySuccess(File copiedFile);
    }

    public static void show(@NonNull Context context,
                            @NonNull FileItem fileItem,
                            @NonNull NotifyingExecutorService executorService,
                            @NonNull ActivePanel activePanel,
                            @NonNull OnCopyCallback callback) {

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
                .setTitle(R.string.copy_file)
                .setMessage(context.getString(R.string.enter_target_directory))
                .setView(input)
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    String targetDirPath = input.getText().toString().trim();
                    if (targetDirPath.isEmpty()) {
                        Toast.makeText(context, R.string.invalid_path, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    File srcFile = fileItem.getFile();
                    File targetDir = new File(targetDirPath);
                    File targetFile = new File(targetDir, srcFile.getName());

                    // 防止复制到自身或子目录
                    if (srcFile.isDirectory()) {
                        try {
                            String srcCanonical = srcFile.getCanonicalPath();
                            String targetCanonical = targetFile.getCanonicalPath();
                            if (targetCanonical.equals(srcCanonical)
                                || targetCanonical.startsWith(srcCanonical + File.separator)) {
                                Toast.makeText(context,
                                        R.string.copy_into_self_error,
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch (IOException e) {
                            e.printStackTrace(); // 可选日志
                        }
                    }

                    SimpleProcessingDialog progressDialog = new SimpleProcessingDialog(context);
                    progressDialog.show();

                    executorService.execute(() -> {
                        boolean success = copyRecursive(srcFile, targetFile);

                        ((android.app.Activity) context).runOnUiThread(() -> {
                            progressDialog.dismiss();
                            if (success) {
                                callback.onCopySuccess(targetFile);
                                Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, R.string.copy_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }, TaskTypes.COPY_FILE);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static boolean copyRecursive(File src, File dest) {
        if (src.isDirectory()) {
            if (!dest.mkdirs() && !dest.exists()) {
                return false;
            }
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    File destChild = new File(dest, child.getName());
                    if (!copyRecursive(child, destChild)) {
                        return false;
                    }
                }
            }
        } else {
            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(dest)) {

                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

            } catch (IOException e) {
                e.printStackTrace(); // 可选日志
                return false;
            }
        }
        return true;
    }
}