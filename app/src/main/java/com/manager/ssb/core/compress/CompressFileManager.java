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

import android.widget.Toast;

import com.manager.ssb.MainActivity;
import com.manager.ssb.util.ArchiveUtils;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.enums.Sence;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.model.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CompressFileManager {
    private final MainActivity activity;
    private final NotifyingExecutorService executorService;

    // 压缩文件状态管理
    private final Map<ActivePanel, CompressPanelState> panelStates = new HashMap<>();

    public CompressFileManager(MainActivity activity, NotifyingExecutorService executorService) {
        this.activity = activity;
        this.executorService = executorService;

        // 初始化两个面板的状态
        panelStates.put(ActivePanel.LEFT, new CompressPanelState());
        panelStates.put(ActivePanel.RIGHT, new CompressPanelState());
    }

    /**
     * 进入压缩文件浏览模式
     */
    public void enterCompressFile(String compressFilePath, ActivePanel panel) {
        executorService.submit(() -> {
            try {
                // 替换：使用 ArchiveUtils 获取完整条目列表
                List<String> entries = ArchiveUtils.listAllEntries(compressFilePath);
                activity.runOnUiThread(() -> {
                    CompressPanelState state = panelStates.get(panel);
                    state.compressFilePath = compressFilePath;
                    state.currentPath = "";
                    state.allEntries.clear();
                    state.allEntries.addAll(entries);

                    // 更新场景
                    setPanelSence(panel, Sence.IN_COMPRESS_FILE);
                    loadCompressContents(panel);
                    activity.updatePathDisplay();

                    Toast.makeText(activity, "已进入压缩文件浏览模式", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "无法打开压缩文件: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }, TaskTypes.LOAD_FILES);
    }

    /**
     * 退出压缩文件浏览模式
     */
    public void exitCompressFile(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        state.compressFilePath = null;
        state.currentPath = "";
        state.allEntries.clear();

        setPanelSence(panel, Sence.FILE);
        activity.loadDirectory(activity.getCurrentDir(), panel);
    }

    /**
     * 加载压缩文件内容（目录浏览）
     */
    public void loadCompressContents(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return;

        executorService.submit(() -> {
            try {
                // 替换：使用 ArchiveUtils 列出直接子项
                List<String> directContents = ArchiveUtils.listDirectChildren(
                        state.compressFilePath,
                        state.currentPath
                );

                List<FileItem> newItems = new ArrayList<>();

                // 添加返回上级目录项（如果不是根目录）
                if (!state.currentPath.isEmpty()) {
                    newItems.add(createParentDirectoryItem());
                }

                // 将条目转换为 FileItem
                for (String itemName : directContents) {
                    boolean isDirectory = itemName.endsWith("/");
                    String displayName = isDirectory ?
                            itemName.substring(0, itemName.length() - 1) : itemName;

                    // 构建完整内部路径
                    String fullPath = state.currentPath + itemName;

                    newItems.add(new CompressFileItem(
                            displayName,
                            isDirectory,
                            0, // 大小可后续从 ArchiveEntry 获取，暂时设为 0
                            System.currentTimeMillis(),
                            fullPath
                    ));
                }

                // 排序：目录在前，文件在后，按名称排序
                Collections.sort(newItems, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });

                activity.runOnUiThread(() -> {
                    activity.updateFileList(newItems, panel);
                    activity.updatePathDisplay();
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }, TaskTypes.LOAD_FILES);
    }

    /**
     * 处理压缩文件内的项目点击
     */
    public void handleCompressItemClick(FileItem item, ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return;

        if (item.isDirectory()) {
            String newPath;
            if ("..".equals(item.getName())) {
                // 返回上级目录
                int lastSlash = state.currentPath.lastIndexOf('/');
                if (lastSlash > 0) {
                    newPath = state.currentPath.substring(0, lastSlash);
                    int prevSlash = newPath.lastIndexOf('/');
                    if (prevSlash >= 0) {
                        newPath = newPath.substring(0, prevSlash + 1);
                    } else {
                        newPath = "";
                    }
                } else {
                    newPath = "";
                }
            } else {
                // 进入子目录
                newPath = state.currentPath + item.getName() + "/";
            }

            state.currentPath = newPath;
            loadCompressContents(panel);
        } else {
            // 处理文件点击
            handleCompressFileClick(item, panel);
        }
    }

    /**
     * 处理压缩文件内的文件点击（可扩展预览、解压等）
     */
    private void handleCompressFileClick(FileItem item, ActivePanel panel) {
        // 暂时仅提示，实际可添加解压单文件或查看
        Toast.makeText(activity, "点击了压缩文件内的: " + item.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * 解压当前压缩包全部内容
     */
    public void extractCurrentCompressFile(ActivePanel panel, String destPath) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) {
            Toast.makeText(activity, "当前不在压缩文件浏览模式", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.submit(() -> {
            try {
                // 替换：使用 ArchiveUtils 解压全部，并显示进度
                ArchiveUtils.extractAll(state.compressFilePath, destPath, (percent, currentFile) -> {
                    activity.runOnUiThread(() -> {
                        // 可在此更新进度条 UI，这里简单用 Toast 演示（过于频繁会不好，实际用 ProgressBar）
                        if (percent % 20 == 0) { // 每20%显示一次
                            Toast.makeText(activity, "解压进度: " + percent + "%", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "解压完成", Toast.LENGTH_SHORT).show();
                    exitCompressFile(panel);
                });
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "解压失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }, TaskTypes.FILE_OPERATION);
    }

    /**
     * 解压压缩包内选中的单个文件
     */
    public void extractSelectedFile(FileItem item, ActivePanel panel, String destPath) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) {
            Toast.makeText(activity, "当前不在压缩文件浏览模式", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取文件对应的内部完整路径
        String entryPath = item.getPath();
        if (entryPath == null || entryPath.equals("..")) {
            Toast.makeText(activity, "无法解压此项", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.submit(() -> {
            try {
                // 替换：使用 ArchiveUtils 解压单个条目
                ArchiveUtils.extractEntry(state.compressFilePath, entryPath, destPath);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "文件已解压", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "解压失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }, TaskTypes.FILE_OPERATION);
    }

    /**
     * 获取当前压缩文件路径
     */
    public String getCurrentCompressFilePath(ActivePanel panel) {
        return panelStates.get(panel).compressFilePath;
    }

    /**
     * 获取当前压缩文件内部路径
     */
    public String getCurrentCompressPath(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) {
            return "";
        }
        return state.compressFilePath + ":" + state.currentPath;
    }

    /**
     * 检查是否在压缩文件浏览模式
     */
    public boolean isInCompressMode(ActivePanel panel) {
        return panelStates.get(panel).compressFilePath != null;
    }

    /**
     * 获取当前显示路径（用于界面显示）
     */
    public String getDisplayPath(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) {
            return "";
        }

        String zipName = new File(state.compressFilePath).getName();
        if (state.currentPath.isEmpty()) {
            return zipName + ":/";
        } else {
            return zipName + ":" + state.currentPath;
        }
    }

    // ---------- 私有辅助方法 ----------

    private FileItem createParentDirectoryItem() {
        return new FileItem(new File("..")) {
            @Override
            public String getName() {
                return "..";
            }

            @Override
            public boolean isDirectory() {
                return true;
            }

            @Override
            public String getPath() {
                return "..";
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public long getLastModified() {
                return 0;
            }
        };
    }

    private void setPanelSence(ActivePanel panel, Sence sence) {
        if (panel == ActivePanel.LEFT) {
            activity.leftPanelSence = sence;
            if (activity.adapterLeft != null) {
                activity.adapterLeft.setSence(sence);
            }
        } else {
            activity.rightPanelSence = sence;
            if (activity.adapterRight != null) {
                activity.adapterRight.setSence(sence);
            }
        }
    }

    /**
     * 压缩文件面板状态
     */
    private static class CompressPanelState {
        String compressFilePath = null;
        String currentPath = "";
        List<String> allEntries = new ArrayList<>();
    }

    /**
     * 压缩文件条目
     */
    public static class CompressFileItem extends FileItem {
        private final boolean isDirectory;
        private final String entryPath;

        public CompressFileItem(String name, boolean isDirectory, long size, long lastModified, String entryPath) {
            super(new File(name));
            this.isDirectory = isDirectory;
            this.entryPath = entryPath;
        }

        @Override
        public String getPath() {
            return entryPath;
        }

        @Override
        public boolean isDirectory() {
            return isDirectory;
        }

        @Override
        public long getSize() {
            return 0; // 可从 libarchive 获取
        }

        @Override
        public long getLastModified() {
            return super.getLastModified();
        }
    }
}