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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TerminalInstaller {
    private static final String TAG = "TerminalInstaller";
    private static final String POSIX_FILES_DIR = "Env";
    private static ProgressDialog progressDialog;
    private static InstallCallback currentInstallCallback;

    public interface InstallCallback {
        void onInstallFinished();
        void onInstallFailed(String reason);
    }

    // 主入口方法
    public static void installCheck(Context context, InstallCallback callback) {
        File shFile = new File(context.getFilesDir(), "usr/bin/sh");
        if (shFile.exists()) {
            Log.i(TAG, "Environment already installed");
            callback.onInstallFinished();
            return;
        }
        currentInstallCallback = callback;
        showInstallOptionsDialog(context);
    }
    

    private static void showFilePathInputDialog(Context context) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity");
            return;
        }

        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            EditText input = new EditText(context);
            new AlertDialog.Builder(context)
                .setTitle("输入ZIP文件路径")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String path = input.getText().toString().trim();
                    if (!path.isEmpty()) {
                        installFromFile(context, path);
                    } else {
                        Toast.makeText(context, "路径不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        });
    }

    private static void installFromFile(Context context, String filePath) {
        new FileInstallerTask(context, new File(filePath)).execute();
    }

    // 添加文件安装任务类
    private static class FileInstallerTask extends AsyncTask<Void, Integer, Boolean> {
        private final Context context;
        private final File zipFile;

        FileInstallerTask(Context context, File zipFile) {
            this.context = context;
            this.zipFile = zipFile;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(context, "正在解压文件...");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return extractZip(new FileInputStream(zipFile), 
                    new File(context.getFilesDir(), "usr"), 
                    zipFile.length());
            } catch (Exception e) {
                Log.e(TAG, "文件安装失败", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dismissProgressDialog();
            if (success) notifySuccess(); 
            else notifyFailure("文件解压失败");
        }
    }

    private static void showInstallOptionsDialog(Context context) {
        if (!(context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity");
            return;
        }

        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            new AlertDialog.Builder(context)
                .setTitle("选择安装方式")
                .setItems(new String[]{"应用内置 ZIP", "从网络获取", "手动输入文件路径"}, (dialog, which) -> {
                    switch (which) {
                        case 0: installFromAssets(context); break;
                        case 1: installFromNetwork(context); break;
                        case 2: showFilePathInputDialog(context); break;
                    }
                })
                .show();
        });
    }

    // ================== 资源文件安装 ==================
    private static void installFromAssets(Context context) {
        new AssetsInstallerTask(context).execute();
    }

    private static class AssetsInstallerTask extends AsyncTask<Void, Integer, Boolean> {
        private final Context context;
        private long totalSize;

        AssetsInstallerTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(context, "正在解压内置资源...");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                AssetManager am = context.getAssets();
                String zipName = getZipFileName();
                InputStream is = am.open(POSIX_FILES_DIR + "/" + zipName);
                
                // 获取文件大小
                totalSize = am.openFd(POSIX_FILES_DIR + "/" + zipName).getLength();
                
                return extractZip(is, new File(context.getFilesDir(), "usr"), totalSize);
            } catch (Exception e) {
                Log.e(TAG, "Assets安装失败", e);
                return false;
            }
        }

        private String getZipFileName() {
            String arch = System.getProperty("os.arch");
            return arch.contains("arm") ? "env_arm.zip" : "env_aarch64.zip";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (progressDialog != null) {
                progressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dismissProgressDialog();
            if (success) {
                grantExecutePermissions();
                notifySuccess();
            } else {
                notifyFailure("内置资源解压失败");
            }
        }

        private void grantExecutePermissions() {
            File binDir = new File(context.getFilesDir(), "usr/bin");
            if (binDir.exists()) {
                for (File file : binDir.listFiles()) {
                    if (file.isFile()) file.setExecutable(true);
                }
            }
        }
    }

    // ================== 网络安装 ================== 
    private static void installFromNetwork(Context context) {
        Activity activity = (Activity) context;
        activity.runOnUiThread(() -> {
            EditText input = new EditText(context);
            new AlertDialog.Builder(context)
                .setTitle("输入下载地址")
                .setView(input)
                .setPositiveButton("下载", (d, w) -> {
                    String url = input.getText().toString();
                    if (!url.isEmpty()) new NetworkInstallerTask(context, url).execute();
                })
                .show();
        });
    }

    private static class NetworkInstallerTask extends AsyncTask<Void, Integer, Boolean> {
        private final Context context;
        private final String url;
        private File tempFile;
        private long totalSize;

        NetworkInstallerTask(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(context, "正在下载...");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                URLConnection conn = new URL(url).openConnection();
                totalSize = conn.getContentLength();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                
                // 保存到临时文件
                tempFile = new File(context.getCacheDir(), "temp.zip");
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    long downloaded = 0;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                        downloaded += len;
                        publishProgress((int) (downloaded * 100 / totalSize));
                    }
                }
                
                // 解压文件
                return extractZip(new FileInputStream(tempFile), 
                    new File(context.getFilesDir(), "usr"), 
                    totalSize);
            } catch (Exception e) {
                Log.e(TAG, "网络安装失败", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (tempFile != null) tempFile.delete();
            dismissProgressDialog();
            if (success) notifySuccess(); else notifyFailure("下载失败");
        }
    }

    // ================== 通用工具方法 ==================
    private interface ProgressUpdater {
        void update(int progress);
    }

    private static boolean extractZip(InputStream is, File targetDir, long totalSize) {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            byte[] buffer = new byte[4096];
            long extracted = 0;
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(targetDir, entry.getName());
                Log.d(TAG, "Extracting: " + file.getAbsolutePath());

                if (entry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }

                File parent = file.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                        extracted += len;
                        int progress = (int) (extracted * 100 / totalSize);
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "解压失败", e);
            return false;
        }
    }

    private static void showProgressDialog(Context context, String message) {
        if (!(context instanceof Activity)) return;
        
        ((Activity) context).runOnUiThread(() -> {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(message);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setMax(100);
            progressDialog.show();
        });
    }

    private static void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
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
}