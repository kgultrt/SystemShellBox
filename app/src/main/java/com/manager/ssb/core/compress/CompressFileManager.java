/*
 * System Shell Box
 * Copyright (C) 2025-2026 kgultrt
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

package com.manager.ssb.core.compress;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.manager.ssb.MainActivity;
import com.manager.ssb.R;
import com.manager.ssb.adapter.FileAdapter;
import com.manager.ssb.util.ArchiveUtils;
import com.manager.ssb.util.ArchiveUtils.ArchiveEntryInfo;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.enums.Sence;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.model.FileItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompressFileManager {
    private final MainActivity activity;
    private final NotifyingExecutorService executorService;
    private final Map<ActivePanel, CompressPanelState> panelStates = new HashMap<>();

    public CompressFileManager(MainActivity activity, NotifyingExecutorService executorService) {
        this.activity = activity;
        this.executorService = executorService;
        panelStates.put(ActivePanel.LEFT, new CompressPanelState());
        panelStates.put(ActivePanel.RIGHT, new CompressPanelState());
    }

    // ---------- 浏览 ----------
    public void enterCompressFile(String compressFilePath, ActivePanel panel) {
        executorService.submit(() -> {
            try {
                List<String> entries = ArchiveUtils.listAllEntries(compressFilePath);
                activity.runOnUiThread(() -> {
                    CompressPanelState state = panelStates.get(panel);
                    state.compressFilePath = compressFilePath;
                    state.currentPath = "";
                    state.allEntries.clear();
                    state.allEntries.addAll(entries);
                    setPanelSence(panel, Sence.IN_COMPRESS_FILE);
                    loadCompressContents(panel);
                    activity.updatePathDisplay();
                    Toast.makeText(activity, "已进入压缩文件浏览模式", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "无法打开压缩文件: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, TaskTypes.LOAD_FILES);
    }

    public void exitCompressFile(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        state.compressFilePath = null;
        state.currentPath = "";
        state.allEntries.clear();
        setPanelSence(panel, Sence.FILE);
        activity.loadDirectory(activity.getCurrentDir(), panel);
    }

    public void loadCompressContents(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return;
        executorService.submit(() -> {
            try {
                List<ArchiveEntryInfo> details = ArchiveUtils.listDetailedChildren(
                        state.compressFilePath, state.currentPath);
                List<FileItem> newItems = new ArrayList<>();
                newItems.add(createParentDirectoryItem());
                for (ArchiveEntryInfo info : details) {
                    String displayName = info.isDirectory ?
                            info.name.substring(0, info.name.length() - 1) : info.name;
                    String fullPath = state.currentPath + info.name;
                    newItems.add(new CompressFileItem(displayName, info.isDirectory,
                            info.size, info.lastModified * 1000, fullPath));
                }
                List<FileItem> sortedItems = new ArrayList<>();
                sortedItems.add(newItems.get(0));
                List<FileItem> rest = new ArrayList<>(newItems.subList(1, newItems.size()));
                Collections.sort(rest, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                sortedItems.addAll(rest);
                activity.runOnUiThread(() -> {
                    activity.updateFileList(sortedItems, panel);
                    activity.updatePathDisplay();
                });
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, TaskTypes.LOAD_FILES);
    }

    public void handleCompressItemClick(FileItem item, ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return;
        if (item.isDirectory()) {
            if ("..".equals(item.getName())) {
                if (state.currentPath.isEmpty()) {
                    exitCompressFile(panel);
                } else {
                    state.currentPath = getParentPath(state.currentPath);
                    loadCompressContents(panel);
                }
                return;
            }
            state.currentPath = state.currentPath + item.getName() + "/";
            loadCompressContents(panel);
        } else {
            handleCompressFileClick(item, panel);
        }
    }

    private void handleCompressFileClick(FileItem item, ActivePanel panel) {
        String sizeStr = item.getSize() > 0 ? " (" + FileAdapter.formatSize(activity, item.getSize()) + ")" : "";
        Toast.makeText(activity, "点击了: " + item.getName() + sizeStr, Toast.LENGTH_SHORT).show();
    }

    // ---------- 长按：区分文件/目录解压 ----------
    public void handleItemLongClick(FileItem item, View view, ActivePanel panel) {
        if ("..".equals(item.getName()) || !(item instanceof CompressFileItem)) return;
        final CompressFileItem compressItem = (CompressFileItem) item;

        new MaterialAlertDialogBuilder(activity)
                .setTitle(item.getName())
                .setItems(new CharSequence[]{"解压", "详细信息"}, (dialog, which) -> {
                    if (which == 1) {
                        showCompressItemInfoDialog(compressItem);
                        return;
                    }
                    String destDir = new File(getCurrentCompressFilePath(panel)).getParent();
                    if (destDir == null) destDir = "/";
                    if (compressItem.isDirectory()) {
                        extractDirectoryRecursively(compressItem, panel, destDir);
                    } else {
                        extractSingleEntry(compressItem, panel, destDir);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCompressItemInfoDialog(CompressFileItem item) {
        String message = "名称: " + item.getName() +
                "\n类型: " + (item.isDirectory() ? "文件夹" : "文件") +
                "\n大小: " + FileAdapter.formatSize(activity, item.getSize()) +
                "\n修改时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(item.getLastModified()));
        new MaterialAlertDialogBuilder(activity)
                .setTitle("详细信息")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // ---------- 解压实现 ----------
    public void extractCurrentCompressFile(ActivePanel panel, String destPath) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) {
            Toast.makeText(activity, "当前不在压缩文件浏览模式", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialogHelper progress = new ProgressDialogHelper(activity, "解压中...");
        progress.show();
        executorService.submit(() -> {
            try {
                ArchiveUtils.extractAll(state.compressFilePath, destPath, (percent, currentFile) ->
                        activity.runOnUiThread(() -> progress.updateProgress(percent, currentFile)));
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("完成")
                            .setMessage("全部解压完成")
                            .setPositiveButton(android.R.string.ok, (d, w) -> exitCompressFile(panel))
                            .show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("错误")
                            .setMessage("解压失败: " + e.getMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        }, TaskTypes.FILE_OPERATION);
    }

    private void extractSingleEntry(CompressFileItem item, ActivePanel panel, String destPath) {
        ProgressDialogHelper progress = new ProgressDialogHelper(activity, "解压单个文件...");
        progress.showIndeterminate();
        executorService.submit(() -> {
            try {
                ArchiveUtils.extractEntry(panelStates.get(panel).compressFilePath, item.getPath(), destPath);
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("完成")
                            .setMessage("文件已解压")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    activity.refreshAllPanels();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("错误")
                            .setMessage("解压失败: " + e.getMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    activity.refreshAllPanels();
                });
            }
        }, TaskTypes.FILE_OPERATION);
    }

    private void extractDirectoryRecursively(CompressFileItem item, ActivePanel panel, String destPath) {
        ProgressDialogHelper progress = new ProgressDialogHelper(activity, "递归解压目录...");
        progress.show();
        executorService.submit(() -> {
            try {
                ArchiveUtils.extractDirectory(panelStates.get(panel).compressFilePath, item.getPath(), destPath,
                        (percent, currentFile) -> activity.runOnUiThread(() -> progress.updateProgress(percent, currentFile)));
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("完成")
                            .setMessage("目录解压完成")
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
                activity.refreshAllPanels();
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("错误")
                            .setMessage("解压失败: " + e.getMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
                activity.refreshAllPanels();
            }
        }, TaskTypes.FILE_OPERATION);
    }

    // ---------- 压缩 ----------
    public void compressFiles(List<FileItem> sourceFiles, String outputPath, int formatCode, int filterCode, ActivePanel panel) {
        List<File> files = new ArrayList<>();
        for (FileItem item : sourceFiles) {
            File file = new File(item.getPath());
            if (file.exists()) files.add(file);
        }
        if (files.isEmpty()) {
            Toast.makeText(activity, "没有可压缩的文件", Toast.LENGTH_SHORT).show();
            return;
        }
        String baseDir = files.get(0).getParent();
        final String effectiveBaseDir = baseDir != null ? baseDir : "/";
        final String finalOutputPath = outputPath;
        final List<File> finalFiles = files;
        final int finalFormatCode = formatCode;
        final int finalFilterCode = filterCode;

        ProgressDialogHelper progress = new ProgressDialogHelper(activity, "压缩中...");
        progress.show();
        executorService.submit(() -> {
            try {
                ArchiveUtils.createArchive(finalOutputPath, effectiveBaseDir, finalFiles,
                        finalFormatCode, finalFilterCode,
                        (percent, currentFile) -> activity.runOnUiThread(() -> progress.updateProgress(percent, currentFile)));
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("完成")
                            .setMessage("压缩完成: " + finalOutputPath)
                            .setPositiveButton(android.R.string.ok, (d, w) -> {
                                if (!isInCompressMode(panel)) activity.loadDirectory(activity.getCurrentDir(), panel);
                            })
                            .show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("错误")
                            .setMessage("压缩失败: " + e.getMessage())
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        }, TaskTypes.FILE_OPERATION);
    }

    // ---------- 路径 ----------
    public String getCurrentCompressFilePath(ActivePanel panel) {
        return panelStates.get(panel).compressFilePath;
    }

    public String getCurrentCompressPath(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return "";
        return state.compressFilePath + ":" + state.currentPath;
    }

    public boolean isInCompressMode(ActivePanel panel) {
        return panelStates.get(panel).compressFilePath != null;
    }

    public String getDisplayPath(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return "";
        if (state.currentPath.isEmpty()) return "/";
        String path = state.currentPath;
        if (!path.startsWith("/")) path = "/" + path;
        return path;
    }

    // ---------- 内部辅助 ----------
    private FileItem createParentDirectoryItem() {
        return new FileItem(new File("..")) {
            @Override public String getName() { return ".."; }
            @Override public boolean isDirectory() { return true; }
            @Override public String getPath() { return ".."; }
            @Override public long getSize() { return 0; }
            @Override public long getLastModified() { return 0; }
        };
    }

    private void setPanelSence(ActivePanel panel, Sence sence) {
        if (panel == ActivePanel.LEFT) {
            activity.leftPanelSence = sence;
            if (activity.adapterLeft != null) activity.adapterLeft.setSence(sence);
        } else {
            activity.rightPanelSence = sence;
            if (activity.adapterRight != null) activity.adapterRight.setSence(sence);
        }
    }

    private String getParentPath(String currentPath) {
        if (currentPath == null || currentPath.isEmpty()) return "";
        String path = currentPath.endsWith("/") ? currentPath.substring(0, currentPath.length() - 1) : currentPath;
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash + 1) : "";
    }

    // 进度对话框内部类（使用 AppCompat AlertDialog）
    private static class ProgressDialogHelper {
        private final AlertDialog dialog;
        private final TextView tvMessage;
        private final ProgressBar progressBar;
        private final TextView tvPercent;

        ProgressDialogHelper(MainActivity activity, String title) {
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_progress_cm, null);
            tvMessage = view.findViewById(R.id.tv_message);
            progressBar = view.findViewById(R.id.progress_bar);
            tvPercent = view.findViewById(R.id.tv_percent);
            dialog = new MaterialAlertDialogBuilder(activity)
                    .setTitle(title)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
        void show() { dialog.show(); }
        void showIndeterminate() {
            progressBar.setIndeterminate(true);
            tvPercent.setVisibility(View.GONE);
            dialog.show();
        }
        void updateProgress(int percent, String message) {
            if (progressBar.isIndeterminate()) {
                progressBar.setIndeterminate(false);
                tvPercent.setVisibility(View.VISIBLE);
            }
            progressBar.setProgress(percent);
            tvPercent.setText(percent + "%");
            if (message != null) tvMessage.setText(message);
        }
        void dismiss() { dialog.dismiss(); }
    }

    // 状态类
    private static class CompressPanelState {
        String compressFilePath = null;
        String currentPath = "";
        List<String> allEntries = new ArrayList<>();
    }

    public static class CompressFileItem extends FileItem {
        private final boolean isDirectory;
        private final String entryPath;
        private final long size;
        private final long lastModified;

        public CompressFileItem(String name, boolean isDirectory, long size, long lastModified, String entryPath) {
            super(new File(name));
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
            this.entryPath = entryPath;
        }

        @Override public String getPath() { return entryPath; }
        @Override public boolean isDirectory() { return isDirectory; }
        @Override public long getSize() { return size; }
        @Override public long getLastModified() { return lastModified; }
    }
}