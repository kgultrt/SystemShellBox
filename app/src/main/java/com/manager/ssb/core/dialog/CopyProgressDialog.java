package com.manager.ssb.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.util.FileUtils;
import com.manager.ssb.util.NativeFileOperation;
import java.io.File;
import java.util.Locale;

public class CopyProgressDialog {
    
    private static Context sLastContext;

    private final Context context;
    private AlertDialog dialog;
    private ProgressBar progressBar;
    private TextView sourceText, targetText, fileText, progressText, etaText;
    private OnCancelListener cancelListener;
    
    private long startTime;
    private long lastCopied;
    private long lastTotal;
    private String currentFileName;

    public CopyProgressDialog(Context context) {
        this.context = context;
        sLastContext = context;
    }
    
    public static Context getLastContext() {
        return sLastContext;
    }
    
    public static void setLastContext(Context context) {
        sLastContext = context;
    }
    
    public void setOnCancelListener(OnCancelListener listener) {
        this.cancelListener = listener;
    }
    
    public interface OnCancelListener {
        void onCancel();
    }

    public void show(String sourcePath, String targetPath, String fileName) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_copy_progress, null);

        progressBar = view.findViewById(R.id.progressBar);
        sourceText = view.findViewById(R.id.sourcePathText);
        targetText = view.findViewById(R.id.targetPathText);
        fileText = view.findViewById(R.id.fileItemText);
        progressText = view.findViewById(R.id.progressText);
        etaText = view.findViewById(R.id.etaText);

        // 缩短路径显示
        sourceText.setText(context.getString(R.string.from) + ": " + 
                          FileUtils.getShortPath(sourcePath));
        targetText.setText(context.getString(R.string.to) + ": " + 
                          FileUtils.getShortPath(targetPath));
        fileText.setText(context.getString(R.string.file_item) + ": " + 
                        FileUtils.getShortName(fileName));

        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
        progressText.setText(context.getString(R.string.copy_progress_message) + ": 0%");
        etaText.setText(context.getString(R.string.eta) + ": 计算中...");

        dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.copy_progress_title)
                .setCancelable(false)
                .setView(view)
                .show();

        startTime = System.currentTimeMillis();
        lastCopied = 0;
        lastTotal = 0;
        currentFileName = fileName;
    }
    
    public Context getContext() {
        return context;
    }
    
    public void updateProgress(String currentFile, long bytesCopied, long totalBytes, int status) {
        // 确保在主线程执行UI更新
        new Handler(Looper.getMainLooper()).post(() -> {
            switch (status) {
                case NativeFileOperation.STATUS_CONFLICT:
                    showConflictState();
                    break;
                    
                case NativeFileOperation.STATUS_RETRYING:
                    showRetryState();
                    break;
                    
                default:
                    showNormalProgress(currentFile, bytesCopied, totalBytes);
                    break;
            }
        });
    }
    
    private void showConflictState() {
        // progressBar.setIndeterminate(true);
        // progressText.setText(context.getString(R.string.file_conflict_detected));
        // etaText.setText(context.getString(R.string.waiting_for_user));
    }
    
    private void showRetryState() {
        progressBar.setIndeterminate(false);
        progressText.setText(context.getString(R.string.retrying_copy));
        etaText.setText(""); // 清除ETA
    }
    
    private void showNormalProgress(String currentFile, long bytesCopied, long totalBytes) {
        progressBar.setIndeterminate(false);
        
        // 记录当前值用于ETA计算
        lastCopied = bytesCopied;
        lastTotal = totalBytes;
        
        // 更新当前文件进度
        int fileProgress = totalBytes > 0 ? (int) (bytesCopied * 100 / totalBytes) : 0;
        
        // 更新UI
        progressBar.setProgress(fileProgress);
        
        String progressStr = String.format(Locale.US, "%s: %d%% (%s/%s)",
            FileUtils.getShortName(currentFile),
            fileProgress,
            FileUtils.formatFileSize(bytesCopied),
            FileUtils.formatFileSize(totalBytes));
        progressText.setText(progressStr);
        
        // 计算ETA（基于当前文件）
        if (etaText != null && bytesCopied > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 0) {
                // 计算平均速度（字节/秒）
                long speed = bytesCopied * 1000 / elapsed;
                if (speed > 0) {
                    long remainingBytes = totalBytes - bytesCopied;
                    long remainingSeconds = remainingBytes / speed;
                    
                    String etaStr = String.format(Locale.US, "%s: %s",
                        context.getString(R.string.eta),
                        FileUtils.formatTime(remainingSeconds));
                    etaText.setText(etaStr);
                }
            }
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}