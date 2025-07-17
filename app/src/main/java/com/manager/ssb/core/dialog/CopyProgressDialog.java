package com.manager.ssb.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder; // 新增导入
import com.manager.ssb.R;

public class CopyProgressDialog {

    private final Context context;
    private AlertDialog dialog;
    private ProgressBar progressBar;
    private TextView sourceText, targetText, fileText, etaText;

    private long startTime;

    public CopyProgressDialog(Context context) {
        this.context = context;
    }

    public void show(String sourcePath, String targetPath, String fileName) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_copy_progress, null);

        progressBar = view.findViewById(R.id.progressBar);
        sourceText = view.findViewById(R.id.sourcePathText);
        targetText = view.findViewById(R.id.targetPathText);
        fileText = view.findViewById(R.id.fileItemText);
        etaText = view.findViewById(R.id.etaText);

        sourceText.setText(context.getString(R.string.from) + ": " + sourcePath);
        targetText.setText(context.getString(R.string.to) + ": " + targetPath);
        fileText.setText(context.getString(R.string.file_item) + ": " + fileName);

        dialog = new MaterialAlertDialogBuilder(context) // 使用 Material 组件
                .setTitle(R.string.copy_progress_title)
                .setCancelable(false)
                .setView(view)
                .show(); // 直接调用 show()

        startTime = System.currentTimeMillis();
    }

    public void updateProgress(long bytesCopied, long totalBytes, String currentFileName) {
        if (progressBar == null) return;

        int progress = (int) ((bytesCopied * 100) / totalBytes);
        progressBar.setProgress(progress);
        fileText.setText(context.getString(R.string.file_item) + ": " + currentFileName);

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 0 && bytesCopied > 0) {
            long speed = bytesCopied / elapsed;
            long remaining = (totalBytes - bytesCopied) / Math.max(speed, 1);
            etaText.setText(context.getString(R.string.eta) + ": " + remaining + "s");
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}