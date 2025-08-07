package com.manager.ssb.util;

import android.os.Handler;
import android.os.Looper;
import com.manager.ssb.dialog.FileConflictDialog;
import com.manager.ssb.dialog.CopyProgressDialog;
import com.manager.ssb.dialog.CopyDialog;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StepFileCopier {

    public static int copyFileWithRetry(String src, String dest,
                                        CopyDialog.ConflictPolicy policy,
                                        NativeFileOperation.ProgressCallback callback) {
        int result;
        File currentDest = new File(dest);
        int retryCount = 0;
        
        do {
            result = NativeFileOperation.copy(src, currentDest.getAbsolutePath(), callback);
            
            if (result == NativeFileOperation.STATUS_CONFLICT) {
                // 通知UI显示冲突状态
                callback.onProgress(src, 0, 0, NativeFileOperation.STATUS_CONFLICT);
                
                // 在UI线程显示冲突对话框
                final int[] userChoice = new int[]{NativeFileOperation.ConflictAction.SKIP};
                final boolean[] applyToAll = new boolean[]{false};
                final CountDownLatch latch = new CountDownLatch(1);
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!policy.applyToAll) {
                        FileConflictDialog.show(
                            CopyProgressDialog.getLastContext(),
                            new File(src).getName(),
                            (action, apply) -> {
                                userChoice[0] = action;
                                applyToAll[0] = apply;
                                latch.countDown();
                            }
                        );
                    } else {
                         userChoice[0] = policy.action;
                         latch.countDown();
                    }
                });
                
                // 非阻塞等待用户响应
                try {
                    latch.await(10, TimeUnit.MINUTES); // 最多等待10分钟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return NativeFileOperation.STATUS_ERROR;
                }
                
                // 记录用户选择（如果应用到全部）
                if (applyToAll[0]) {
                    policy.action = userChoice[0];
                    policy.applyToAll = true;
                }
                
                // 根据用户选择执行操作
                switch (userChoice[0]) {
                    case NativeFileOperation.ConflictAction.OVERWRITE:
                        NativeFileOperation.delete(currentDest.getAbsolutePath());
                        // 通知UI重新开始
                        callback.onProgress(src, 0, 0, NativeFileOperation.STATUS_RETRYING);
                        break;
                        
                    case NativeFileOperation.ConflictAction.KEEP_BOTH:
                        currentDest = FileUtils.generateUniqueFileName(currentDest);
                        // 通知UI新文件名
                        callback.onProgress(currentDest.getName(), 0, 0, NativeFileOperation.STATUS_RETRYING);
                        break;
                        
                    case NativeFileOperation.ConflictAction.SKIP:
                        return NativeFileOperation.STATUS_SKIPPED;
                        
                    default: // 用户取消
                        return NativeFileOperation.STATUS_ERROR;
                }
                retryCount++;
            }
        } while (result == NativeFileOperation.STATUS_CONFLICT && retryCount < 5);
        
        return result;
    }
}