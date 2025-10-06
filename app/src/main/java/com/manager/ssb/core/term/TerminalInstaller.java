/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.core.term;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import com.manager.ssb.R;
import com.manager.ssb.core.config.Config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TerminalInstaller {
    private static final String TAG = "TerminalInstaller";
    private static final String GITHUB_RELEASE_URL = "https://api.github.com/repos/kgultrt/SystemShellBox-Package/releases/latest";
    private static final String DOWNLOAD_URL = "https://github.com/kgultrt/SystemShellBox-Package/releases/latest/download/base.zip";
    
    private static AlertDialog progressDialog;
    private static LinearProgressIndicator progressIndicator;
    private static TextView progressTitle;
    private static TextView progressPercentage;
    private static TextView progressStatus;
    private static InstallCallback currentInstallCallback;
    private static String latestVersionCache;
    
    // 用于控制进度更新频率，避免过于频繁的UI更新
    private static final long PROGRESS_UPDATE_INTERVAL = 200; // 毫秒
    private static long lastProgressUpdateTime = 0;
    private static final AtomicInteger currentProgress = new AtomicInteger(0);
    private static final AtomicBoolean isUpdatingProgress = new AtomicBoolean(false);

    public interface InstallCallback {
        void onInstallFinished();
        void onInstallFailed(String reason);
    }

    // 检查是否已经安装
    public static boolean isEnvironmentInstalled(Context context) {
        File shFile = new File(context.getFilesDir(), "usr/bin/bash");
        return shFile.exists();
    }

    // 主入口方法 - 检查安装和更新
    public static void installCheck(Context context, InstallCallback callback) {
        currentInstallCallback = callback;
        
        if (isEnvironmentInstalled(context)) {
            Log.i(TAG, "Environment already installed");
            // 环境已安装，检查更新
            checkForUpdates(context, true);
        } else {
            // 环境未安装，显示安装选项
            showInstallOptionsDialog(context);
        }
    }

    // 检查更新
    private static void checkForUpdates(Context context, boolean silent) {
        long lastCheck = getLastUpdateCheck();
        long currentTime = System.currentTimeMillis();
        // 24小时内不重复检查
        if (silent && (currentTime - lastCheck < 24 * 60 * 60 * 1000)) {
            if (currentInstallCallback != null) {
                currentInstallCallback.onInstallFinished();
            }
            return;
        }

        if (!silent) {
            showProgressDialog(context, "检查更新", "正在连接到服务器...", 0);
        }

        new AsyncTask<Void, Void, UpdateInfo>() {
            @Override
            protected UpdateInfo doInBackground(Void... params) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(GITHUB_RELEASE_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    conn.setRequestProperty("User-Agent", "SystemShellBox-App");
                    
                    int responseCode = conn.getResponseCode();
                    Log.d(TAG, "GitHub API response code: " + responseCode);
                    
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "HTTP error response: " + responseCode);
                        return null;
                    }
                    
                    InputStream is = new BufferedInputStream(conn.getInputStream());
                    StringBuilder response = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }
                    
                    Log.d(TAG, "GitHub response: " + response.toString());
                    
                    JSONObject json = new JSONObject(response.toString());
                    String version = json.getString("tag_name");
                    String releaseNotes = json.getString("body");
                    String publishedAt = json.getString("published_at");
                    
                    // 缓存最新版本号
                    latestVersionCache = version;
                    
                    return new UpdateInfo(version, releaseNotes, publishedAt);
                } catch (Exception e) {
                    Log.e(TAG, "检查更新失败", e);
                    return null;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
            
            @Override
            protected void onPostExecute(UpdateInfo updateInfo) {
                if (!silent) {
                    dismissProgressDialog();
                }
                
                setLastUpdateCheck(System.currentTimeMillis());
                
                if (updateInfo != null) {
                    String currentVersion = getInstalledVersion();
                    Log.d(TAG, "Current version: " + currentVersion + ", Latest version: " + updateInfo.version);
                    if (isNewerVersion(updateInfo.version, currentVersion)) {
                        showUpdateAvailableDialog(context, updateInfo);
                    } else {
                        if (!silent) {
                            Toast.makeText(context, "已经是最新版本", Toast.LENGTH_SHORT).show();
                        }
                        if (silent && currentInstallCallback != null) {
                            currentInstallCallback.onInstallFinished();
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get update info");
                    if (!silent) {
                        Toast.makeText(context, "检查更新失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                    if (silent && currentInstallCallback != null) {
                        currentInstallCallback.onInstallFinished();
                    }
                }
            }
        }.execute();
    }

    private static void showInstallOptionsDialog(Context context) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity");
            if (currentInstallCallback != null) {
                currentInstallCallback.onInstallFailed("Context is not an Activity");
            }
            return;
        }

        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(context)
                .setTitle("环境安装")
                .setMessage("需要安装POSIX环境才能使用终端功能")
                .setPositiveButton("自动下载安装", (d, w) -> 
                    checkLatestVersionAndInstall(context))
                .setNegativeButton("手动安装", (d, w) -> 
                    showFilePathInputDialog(context))
                .setNeutralButton("取消", (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed("用户取消安装");
                    }
                })
                .show();
        });
    }

    private static void checkLatestVersionAndInstall(Context context) {
        showProgressDialog(context, "获取版本信息", "正在连接至 GitHub...", 0);
        
        new AsyncTask<Void, Void, UpdateInfo>() {
            @Override
            protected UpdateInfo doInBackground(Void... params) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(GITHUB_RELEASE_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    conn.setRequestProperty("User-Agent", "SystemShellBox-App");
                    
                    int responseCode = conn.getResponseCode();
                    Log.d(TAG, "GitHub API response code: " + responseCode);
                    
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return null;
                    }
                    
                    InputStream is = new BufferedInputStream(conn.getInputStream());
                    StringBuilder response = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }
                    
                    JSONObject json = new JSONObject(response.toString());
                    String version = json.getString("tag_name");
                    String releaseNotes = json.getString("body");
                    String publishedAt = json.getString("published_at");
                    
                    // 缓存最新版本号
                    latestVersionCache = version;
                    
                    return new UpdateInfo(version, releaseNotes, publishedAt);
                } catch (Exception e) {
                    Log.e(TAG, "获取版本信息失败", e);
                    return null;
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
            
            @Override
            protected void onPostExecute(UpdateInfo updateInfo) {
                dismissProgressDialog();
                
                if (updateInfo != null) {
                    showVersionInfoDialog(context, updateInfo);
                } else {
                    showErrorDialog(context, "无法获取版本信息，是否继续下载？", 
                        (dialog, which) -> installFromNetwork(context));
                }
            }
        }.execute();
    }

    private static void showVersionInfoDialog(Context context, UpdateInfo updateInfo) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            String shortNotes = updateInfo.releaseNotes.length() > 200 ? 
                updateInfo.releaseNotes.substring(0, 200) + "..." : updateInfo.releaseNotes;
                
            new MaterialAlertDialogBuilder(context)
                .setTitle("版本 " + updateInfo.version)
                .setMessage("发布日期: " + updateInfo.publishedAt + "\n\n" + shortNotes)
                .setPositiveButton("下载安装", (d, w) -> installFromNetwork(context))
                .setNegativeButton("取消", (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed("用户取消安装");
                    }
                })
                .show();
        });
    }

    private static void showUpdateAvailableDialog(Context context, UpdateInfo updateInfo) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            String shortNotes = updateInfo.releaseNotes.length() > 150 ? 
                updateInfo.releaseNotes.substring(0, 150) + "..." : updateInfo.releaseNotes;
                
            new MaterialAlertDialogBuilder(context)
                .setTitle("发现新版本 " + updateInfo.version)
                .setMessage(shortNotes)
                .setPositiveButton("更新", (d, w) -> installFromNetwork(context))
                .setNegativeButton("忽略", (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFinished();
                    }
                })
                .show();
        });
    }

    private static void showErrorDialog(Context context, String message, 
            android.content.DialogInterface.OnClickListener positiveListener) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(context)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("继续", positiveListener)
                .setNegativeButton("取消", (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed("用户取消操作");
                    }
                })
                .show();
        });
    }

    private static void showFilePathInputDialog(Context context) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity");
            return;
        }

        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            TextInputLayout textInputLayout = new TextInputLayout(context);
            TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
            editText.setHint("请输入ZIP文件完整路径");
            textInputLayout.addView(editText);
            
            new MaterialAlertDialogBuilder(context)
                .setTitle("手动安装")
                .setView(textInputLayout)
                .setPositiveButton("安装", (d, w) -> {
                    String path = editText.getText().toString().trim();
                    if (!path.isEmpty()) {
                        installFromFile(context, path);
                    } else {
                        Toast.makeText(context, "路径不能为空", Toast.LENGTH_SHORT).show();
                        if (currentInstallCallback != null) {
                            currentInstallCallback.onInstallFailed("文件路径为空");
                        }
                    }
                })
                .setNegativeButton("取消", (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed("用户取消安装");
                    }
                })
                .show();
        });
    }

    private static void installFromFile(Context context, String filePath) {
        new FileInstallerTask(context, new File(filePath)).execute();
    }

    // ================== 文件安装任务 ==================
    private static class FileInstallerTask extends AsyncTask<Void, Integer, Boolean> {
        private final Context context;
        private final File zipFile;

        FileInstallerTask(Context context, File zipFile) {
            this.context = context;
            this.zipFile = zipFile;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(context, "安装环境", "正在准备解压文件...", 0);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (!zipFile.exists()) {
                    Log.e(TAG, "ZIP file does not exist: " + zipFile.getAbsolutePath());
                    return false;
                }
                
                updateProgress("正在解压文件...", 10);
                Log.d(TAG, "Starting extraction from: " + zipFile.getAbsolutePath());
                
                boolean result = extractZip(new FileInputStream(zipFile), 
                    new File(context.getFilesDir(), "usr"), 
                    zipFile.length());
                
                Log.d(TAG, "Extraction result: " + result);
                return result;
            } catch (Exception e) {
                Log.e(TAG, "文件安装失败", e);
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // 进度更新已经在doInBackground中通过updateProgress处理
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                updateProgress("正在设置权限...", 90);
                boolean permSuccess = grantExecutePermissions();
                Log.d(TAG, "Permission grant result: " + permSuccess);
                setInstalledVersion("manual");
                updateProgress("安装完成", 100);
                // 延迟关闭对话框，让用户看到完成状态
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dismissProgressDialog();
                    notifySuccess();
                }, 500);
            } else {
                dismissProgressDialog();
                notifyFailure("文件解压失败，请检查文件路径和权限");
            }
        }

        private boolean grantExecutePermissions() {
            try {
                File binDir = new File(context.getFilesDir(), "usr/bin");
                if (binDir.exists() && binDir.isDirectory()) {
                    File[] files = binDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                boolean result = file.setExecutable(true);
                                Log.d(TAG, "Set executable for " + file.getName() + ": " + result);
                            }
                        }
                    }
                    return true;
                }
                return false;
            } catch (Exception e) {
                Log.e(TAG, "设置权限失败", e);
                return false;
            }
        }
    }

    // ================== 网络安装 ================== 
    private static void installFromNetwork(Context context) {
        new NetworkInstallerTask(context, DOWNLOAD_URL).execute();
    }

    private static class NetworkInstallerTask extends AsyncTask<Void, Integer, String> {
        private final Context context;
        private final String url;
        private File tempFile;

        NetworkInstallerTask(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(context, "安装环境", "正在准备下载...", 0);
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection conn = null;
            try {
                updateProgress("正在连接服务器...", 5);
                
                Log.d(TAG, "Starting download from: " + url);
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "SystemShellBox-App");
                conn.connect();
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Download response code: " + responseCode);
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error response: " + responseCode);
                    return "HTTP_ERROR";
                }

                int contentLength = conn.getContentLength();
                Log.d(TAG, "Content length: " + contentLength);
                if (contentLength <= 0) {
                    Log.e(TAG, "无法获取文件大小");
                    return "INVALID_SIZE";
                }

                InputStream is = new BufferedInputStream(conn.getInputStream());
                
                // 保存到临时文件
                tempFile = new File(context.getCacheDir(), "temp_env.zip");
                Log.d(TAG, "Temp file path: " + tempFile.getAbsolutePath());
                
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    long downloaded = 0;
                    long lastUpdateTime = System.currentTimeMillis();
                    int lastProgress = 5;
                    
                    while ((len = is.read(buffer)) != -1) {
                        if (isCancelled()) return "CANCELLED";
                        os.write(buffer, 0, len);
                        downloaded += len;
                        
                        // 限制进度更新频率，避免过于频繁
                        long currentTime = System.currentTimeMillis();
                        int progress = 5 + (int) (downloaded * 85 / contentLength);
                        
                        if (currentTime - lastUpdateTime > PROGRESS_UPDATE_INTERVAL || progress - lastProgress >= 5) {
                            updateProgress("正在下载环境文件...", progress);
                            lastUpdateTime = currentTime;
                            lastProgress = progress;
                        }
                    }
                }
                
                Log.d(TAG, "Download completed, file size: " + tempFile.length());
                updateProgress("正在解压文件...", 90);
                
                // 解压文件 - 使用新的解压方法，不传递进度更新器
                boolean success = extractZip(new FileInputStream(tempFile), 
                    new File(context.getFilesDir(), "usr"), 
                    tempFile.length());
                
                Log.d(TAG, "Extraction result: " + success);
                return success ? "SUCCESS" : "EXTRACTION_FAILED";
            } catch (Exception e) {
                Log.e(TAG, "网络安装失败", e);
                return "EXCEPTION: " + e.getMessage();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // 进度更新已经在doInBackground中通过updateProgress处理
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Installation result: " + result);
            
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                Log.d(TAG, "Temp file deleted: " + deleted);
            }
            
            if ("SUCCESS".equals(result)) {
                updateProgress("正在设置权限...", 95);
                boolean permSuccess = grantExecutePermissions();
                Log.d(TAG, "Permission grant result: " + permSuccess);
                
                // 使用缓存的最新版本号
                if (latestVersionCache != null) {
                    setInstalledVersion(latestVersionCache);
                } else {
                    setInstalledVersion("auto_unknown");
                }
                updateProgress("安装完成", 100);
                
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dismissProgressDialog();
                    notifySuccess();
                }, 800);
            } else {
                dismissProgressDialog();
                String errorMsg = "安装失败: ";
                if (result.startsWith("HTTP_ERROR")) {
                    errorMsg += "网络连接错误";
                } else if (result.startsWith("EXTRACTION_FAILED")) {
                    errorMsg += "文件解压失败";
                } else {
                    errorMsg += result;
                }
                notifyFailure(errorMsg);
            }
        }

        private boolean grantExecutePermissions() {
            try {
                File binDir = new File(context.getFilesDir(), "usr/bin");
                if (binDir.exists() && binDir.isDirectory()) {
                    File[] files = binDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                file.setExecutable(true);
                            }
                        }
                    }
                    return true;
                }
                return false;
            } catch (Exception e) {
                Log.e(TAG, "设置权限失败", e);
                return false;
            }
        }
    }

    // ================== 优化的解压方法 ==================
    private static boolean extractZip(InputStream is, File targetDir, long totalSize) {
        return extractZip(is, targetDir, totalSize, null);
    }

    private static boolean extractZip(InputStream is, File targetDir, long totalSize, ProgressUpdater progressUpdater) {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            // 确保目标目录存在
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                Log.d(TAG, "Target directory created: " + created);
            }

            byte[] buffer = new byte[8192];
            long extracted = 0;
            ZipEntry entry;
            int fileCount = 0;
            long lastProgressUpdate = 0;

            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(targetDir, entry.getName());
                fileCount++;

                if (entry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }

                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                        extracted += len;
                        
                        // 计算并更新进度，但限制更新频率
                        if (totalSize > 0 && progressUpdater != null) {
                            int progress = (int) (extracted * 100 / totalSize);
                            
                            // 限制进度不超过100%
                            if (progress > 100) progress = 100;
                            
                            // 限制更新频率，避免过于频繁
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL) {
                                progressUpdater.update(progress);
                                lastProgressUpdate = currentTime;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to extract file: " + file.getAbsolutePath(), e);
                    return false;
                }
                zis.closeEntry();
                
                // 每解压10个文件记录一次日志，避免过于频繁
                if (fileCount % 10 == 0) {
                    Log.d(TAG, "Extracted " + fileCount + " files, progress: " + 
                          (totalSize > 0 ? (extracted * 100 / totalSize) + "%" : "unknown"));
                }
            }
            Log.d(TAG, "Extraction completed successfully, total files: " + fileCount);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "解压失败", e);
            return false;
        }
    }

    // ================== 优化的进度更新方法 ==================
    private static void showProgressDialog(Context context, String title, String status, int progress) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Cannot show progress dialog: context is not an Activity");
            return;
        }
        
        Activity activity = (Activity) context;
        if (activity.isFinishing()) {
            Log.e(TAG, "Activity is finishing, cannot show dialog");
            return;
        }
        
        activity.runOnUiThread(() -> {
            try {
                // 先关闭之前的对话框
                dismissProgressDialog();
                
                // 创建自定义布局
                LayoutInflater inflater = LayoutInflater.from(context);
                View progressView = inflater.inflate(R.layout.dialog_progress_term, null);
                
                progressTitle = progressView.findViewById(R.id.progress_title);
                progressPercentage = progressView.findViewById(R.id.progress_percentage);
                progressStatus = progressView.findViewById(R.id.progress_status);
                progressIndicator = progressView.findViewById(R.id.progress_indicator);
                
                // 设置初始值
                progressTitle.setText(title);
                progressStatus.setText(status);
                progressIndicator.setProgress(progress);
                progressPercentage.setText(progress + "%");
                
                // 重置进度控制变量
                currentProgress.set(progress);
                lastProgressUpdateTime = System.currentTimeMillis();
                isUpdatingProgress.set(false);
                
                // 创建对话框
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                    .setView(progressView)
                    .setCancelable(false);
                
                progressDialog = builder.show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to show progress dialog", e);
            }
        });
    }
    
    private static void updateProgress(String status, int progress) {
        // 确保进度在0-100范围内
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        
        // 如果进度没有变化，不需要更新
        if (progress == currentProgress.get()) {
            return;
        }
        
        currentProgress.set(progress);
        
        // 限制更新频率
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProgressUpdateTime < PROGRESS_UPDATE_INTERVAL && 
            !isUpdatingProgress.get()) {
            return;
        }
        
        // 标记正在更新，避免重复更新
        if (isUpdatingProgress.compareAndSet(false, true)) {
            lastProgressUpdateTime = currentTime;
            
            // 创建final变量供lambda使用
            final String finalStatus = status;
            final int finalProgress = progress;
            
            // 使用主线程Handler来更新UI
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        if (progressStatus != null) {
                            progressStatus.setText(finalStatus);
                        }
                        if (progressIndicator != null) {
                            progressIndicator.setProgress(finalProgress);
                        }
                        if (progressPercentage != null) {
                            progressPercentage.setText(finalProgress + "%");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to update progress", e);
                } finally {
                    // 重置更新标志
                    isUpdatingProgress.set(false);
                }
            });
        }
    }

    private static void dismissProgressDialog() {
        if (progressDialog != null) {
            // 使用主线程Handler来关闭对话框
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to dismiss progress dialog", e);
                }
                progressDialog = null;
                progressIndicator = null;
                progressTitle = null;
                progressPercentage = null;
                progressStatus = null;
                
                // 重置状态变量
                currentProgress.set(0);
                isUpdatingProgress.set(false);
            });
        }
    }

    private static void notifySuccess() {
        if (currentInstallCallback != null) {
            currentInstallCallback.onInstallFinished();
        }
    }

    private static void notifyFailure(String reason) {
        if (currentInstallCallback != null) {
            currentInstallCallback.onInstallFailed(reason);
        }
    }

    // ================== 版本管理 ==================
    private static class UpdateInfo {
        String version;
        String releaseNotes;
        String publishedAt;

        UpdateInfo(String version, String releaseNotes, String publishedAt) {
            this.version = version;
            this.releaseNotes = releaseNotes;
            this.publishedAt = publishedAt;
        }
    }

    private static String getInstalledVersion() {
        return Config.get("term.version", "v1.0.0");
    }

    private static void setInstalledVersion(String version) {
        Config.set("term.version", version);
        Log.i(TAG, "设置安装版本: " + version);
    }

    private static long getLastUpdateCheck() {
        return Config.get("term.lastUpdateCheck", 0);
    }

    private static void setLastUpdateCheck(long time) {
        Config.set("term.lastUpdateCheck", time);
    }

    private static boolean isNewerVersion(String newVersion, String currentVersion) {
        if (currentVersion == null) {
            return true;
        }
        
        if ("manual".equals(currentVersion) || "auto_unknown".equals(currentVersion)) {
            return true;
        }
        
        try {
            String cleanNew = newVersion.startsWith("v") ? newVersion.substring(1) : newVersion;
            String cleanCurrent = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;
            
            return !cleanNew.equals(cleanCurrent);
        } catch (Exception e) {
            Log.e(TAG, "版本比较失败", e);
            return true;
        }
    }

    // 手动检查更新（从设置中调用）
    public static void manualCheckForUpdates(Context context) {
        checkForUpdates(context, false);
    }
    
    // 获取当前安装版本信息
    public static String getCurrentVersionInfo() {
        String installedVersion = getInstalledVersion();
        if (installedVersion == null) {
            return "未安装";
        } else if ("manual".equals(installedVersion)) {
            return "手动安装";
        } else if ("auto_unknown".equals(installedVersion)) {
            return "自动安装（版本未知）";
        } else {
            return installedVersion;
        }
    }
    
    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    // 进度更新接口（保持向后兼容）
    private interface ProgressUpdater {
        void update(int progress);
    }
}
