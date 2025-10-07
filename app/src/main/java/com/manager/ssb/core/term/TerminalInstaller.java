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

// Warning: Don't touch this shit!
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
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import com.manager.ssb.R;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.Application;

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
    private static final long PROGRESS_UPDATE_INTERVAL = 1; // 毫秒
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
            showProgressDialog(context, g(R.string.term_install_check), g(R.string.term_install_connecting), 0);
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
                    Log.e(TAG, "Failed to check for updates", e);
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
                            Toast.makeText(context, g(R.string.term_install_already), Toast.LENGTH_SHORT).show();
                        }
                        if (silent && currentInstallCallback != null) {
                            currentInstallCallback.onInstallFinished();
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get update info");
                    if (!silent) {
                        Toast.makeText(context, g(R.string.term_install_network), Toast.LENGTH_SHORT).show();
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
                .setTitle(g(R.string.term_install_title))
                .setMessage(g(R.string.term_install_msg))
                .setPositiveButton(g(R.string.term_install_auto), (d, w) -> 
                    checkLatestVersionAndInstall(context))
                .setNegativeButton(g(R.string.term_install_file), (d, w) -> 
                    showFilePathInputDialog(context))
                .setNeutralButton(g(R.string.cancel), (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed(g(R.string.term_install_failed));
                    }
                })
                .show();
        });
    }

    private static void checkLatestVersionAndInstall(Context context) {
        showProgressDialog(context, g(R.string.term_install_get_ver), g(R.string.term_install_get_ver2), 0);
        
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
                    Log.e(TAG, "Failed to get version information", e);
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
                    showErrorDialog(context, g(R.string.term_install_failed2), 
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
                .setTitle(g(R.string.ver) + " " + updateInfo.version)
                .setMessage(g(R.string.term_install_date) + updateInfo.publishedAt + "\n\n" + shortNotes)
                .setPositiveButton(g(R.string.term_install_download), (d, w) -> installFromNetwork(context))
                .setNegativeButton(g(R.string.cancel), (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed(g(R.string.term_install_failed));
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
                .setTitle(g(R.string.term_install_found_new) + updateInfo.version)
                .setMessage(shortNotes)
                .setPositiveButton(g(R.string.term_install_update), (d, w) -> installFromNetwork(context))
                .setNegativeButton(g(R.string.term_install_ignore), (d, w) -> {
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
                .setTitle(g(R.string.term_install_tip))
                .setMessage(message)
                .setPositiveButton(g(R.string.term_install_continue), positiveListener)
                .setNegativeButton(g(R.string.cancel), (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed(g(R.string.term_install_failed));
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
            editText.setHint(g(R.string.term_install_file_hint));
            textInputLayout.addView(editText);
            
            new MaterialAlertDialogBuilder(context)
                .setTitle(g(R.string.term_install_file))
                .setView(textInputLayout)
                .setPositiveButton(g(R.string.installing), (d, w) -> {
                    String path = editText.getText().toString().trim();
                    if (!path.isEmpty()) {
                        installFromFile(context, path);
                    } else {
                        if (currentInstallCallback != null) {
                            currentInstallCallback.onInstallFailed(g(R.string.term_install_empty));
                        }
                    }
                })
                .setNegativeButton(g(R.string.cancel), (d, w) -> {
                    if (currentInstallCallback != null) {
                        currentInstallCallback.onInstallFailed(g(R.string.term_install_failed));
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
            showProgressDialog(context, g(R.string.installing), g(R.string.term_install_pree), 0);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (!zipFile.exists()) {
                    Log.e(TAG, "ZIP file does not exist: " + zipFile.getAbsolutePath());
                    return false;
                }
                
                updateProgress(g(R.string.term_install_e), 10);
                Log.d(TAG, "Starting extraction from: " + zipFile.getAbsolutePath());
                
                boolean result = extractZip(new FileInputStream(zipFile), 
                    new File(context.getFilesDir(), "usr"), 
                    zipFile.length());
                
                Log.d(TAG, "Extraction result: " + result);
                return result;
            } catch (Exception e) {
                Log.e(TAG, "File installation failed", e);
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
                updateProgress(g(R.string.term_install_setting_permissions), 90);
                boolean permSuccess = grantExecutePermissions();
                Log.d(TAG, "Permission grant result: " + permSuccess);
                setInstalledVersion("manual");
                updateProgress(g(R.string.term_install_complete), 100);
                // 延迟关闭对话框，让用户看到完成状态
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dismissProgressDialog();
                    notifySuccess();
                }, 500);
            } else {
                dismissProgressDialog();
                notifyFailure(g(R.string.term_install_failed3));
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
                Log.e(TAG, "Failed to set permission", e);
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
            showProgressDialog(context, g(R.string.installing), g(R.string.term_install_pred), 0);
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection conn = null;
            try {
                updateProgress(g(R.string.term_install_connecting), 5);
                
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
                    Log.e(TAG, "Unable to get file sizes");
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
                            updateProgress(g(R.string.term_install_d), progress);
                            lastUpdateTime = currentTime;
                            lastProgress = progress;
                        }
                    }
                }
                
                Log.d(TAG, "Download completed, file size: " + tempFile.length());
                updateProgress(g(R.string.term_install_e), 90);
                
                // 解压文件 - 使用新的解压方法，不传递进度更新器
                boolean success = extractZip(new FileInputStream(tempFile), 
                    new File(context.getFilesDir(), "usr"), 
                    tempFile.length());
                
                Log.d(TAG, "Extraction result: " + success);
                return success ? "SUCCESS" : "EXTRACTION_FAILED";
            } catch (Exception e) {
                Log.e(TAG, "Network installation failed", e);
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
                updateProgress(g(R.string.term_install_setting_permissions), 95);
                boolean permSuccess = grantExecutePermissions();
                Log.d(TAG, "Permission grant result: " + permSuccess);
                
                // 使用缓存的最新版本号
                if (latestVersionCache != null) {
                    setInstalledVersion(latestVersionCache);
                } else {
                    setInstalledVersion("auto_unknown");
                }
                updateProgress(g(R.string.term_install_complete), 100);
                
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dismissProgressDialog();
                    notifySuccess();
                }, 800);
            } else {
                dismissProgressDialog();
                String errorMsg = g(R.string.term_install_failed4);
                if (result.startsWith("HTTP_ERROR")) {
                    errorMsg += g(R.string.term_install_failed5);
                } else if (result.startsWith("EXTRACTION_FAILED")) {
                    errorMsg += g(R.string.term_install_failed6);
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
                Log.e(TAG, "Failed to set permission", e);
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
            Log.e(TAG, "Decompression failed", e);
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
        return Config.get("term.version", "v0.0.0");
    }

    private static void setInstalledVersion(String version) {
        Config.set("term.version", version);
        Log.i(TAG, "Set the installation version: " + version);
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
            Log.e(TAG, "Version comparison failed", e);
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
        if (installedVersion == "v0.0.0") {
            return g(R.string.term_install_not);
        } else if ("manual".equals(installedVersion)) {
            return g(R.string.term_install_file);
        } else if ("auto_unknown".equals(installedVersion)) {
            return g(R.string.term_install_auto_unknow);
        } else {
            return g(R.string.term_install_auto_version) + installedVersion;
        }
    }
    
    private static String g(@StringRes int stringRes) {
        return Application.get(stringRes);
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
