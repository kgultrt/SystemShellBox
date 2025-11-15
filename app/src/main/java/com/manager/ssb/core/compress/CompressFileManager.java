/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.manager.ssb.core.compress;

import android.content.Context;
import android.widget.Toast;

import com.manager.ssb.MainActivity;
import com.manager.ssb.R;
import com.manager.ssb.util.ZipUtils;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.enums.Sence;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.adapter.FileAdapter;

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
    private String currentCompressFile = null;
    private String currentCompressPath = "";
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
                List<String> entries = ZipUtils.zipcat(compressFilePath);
                activity.runOnUiThread(() -> {
                    CompressPanelState state = panelStates.get(panel);
                    state.compressFilePath = compressFilePath;
                    state.currentPath = "";
                    state.entries.clear();
                    state.entries.addAll(entries);
                    
                    // 更新场景
                    setPanelSence(panel, Sence.IN_COMPRESS_FILE);
                    loadCompressContents(panel);
                    activity.updatePathDisplay();
                    
                    Toast.makeText(activity, "已进入压缩文件浏览模式", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
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
        state.entries.clear();
        
        setPanelSence(panel, Sence.FILE);
        activity.loadDirectory(activity.getCurrentDir(), panel);
    }
    
    /**
     * 加载压缩文件内容
     */
    public void loadCompressContents(ActivePanel panel) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) return;
        
        executorService.submit(() -> {
            List<FileItem> newItems = new ArrayList<>();
            String internalPath = state.currentPath;
            
            // 添加返回上级目录项（如果不是根目录）
            if (!internalPath.isEmpty()) {
                newItems.add(createParentDirectoryItem());
            }
            
            // 过滤当前路径下的内容
            for (String entry : state.entries) {
                if (entry.startsWith(internalPath) && !entry.equals(internalPath)) {
                    String relativePath = entry.substring(internalPath.length());
                    
                    // 处理目录和文件
                    int slashIndex = relativePath.indexOf('/');
                    if (slashIndex > 0) {
                        String dirName = relativePath.substring(0, slashIndex);
                        if (!containsName(newItems, dirName)) {
                            newItems.add(new CompressFileItem(dirName, true, 0, 0, entry));
                        }
                    } else if (slashIndex == -1) {
                        // 文件
                        newItems.add(new CompressFileItem(relativePath, false, 0, System.currentTimeMillis(), entry));
                    }
                }
            }
            
            // 排序
            Collections.sort(newItems, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            
            activity.runOnUiThread(() -> {
                activity.updateFileList(newItems, panel);
                activity.updatePathDisplay();
            });
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
                newPath = lastSlash > 0 ? state.currentPath.substring(0, lastSlash) : "";
            } else {
                // 进入子目录
                newPath = state.currentPath + item.getName() + "/";
            }
            
            state.currentPath = newPath;
            loadCompressContents(panel);
        } else {
            // 处理文件点击（解压单个文件等）
            handleCompressFileClick(item, panel);
        }
    }
    
    /**
     * 处理压缩文件内的文件点击
     */
    private void handleCompressFileClick(FileItem item, ActivePanel panel) {
        // 这里可以实现解压单个文件或预览文件内容
        Toast.makeText(activity, "点击了压缩文件内的: " + item.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 解压当前压缩文件
     */
    public void extractCurrentCompressFile(ActivePanel panel, String destPath) {
        CompressPanelState state = panelStates.get(panel);
        if (state.compressFilePath == null) {
            Toast.makeText(activity, "当前不在压缩文件浏览模式", Toast.LENGTH_SHORT).show();
            return;
        }
        
        executorService.submit(() -> {
            try {
                ZipUtils.unzip(state.compressFilePath, destPath);
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
     * 获取当前压缩文件路径
     */
    public String getCurrentCompressFilePath(ActivePanel panel) {
        return panelStates.get(panel).compressFilePath;
    }
    
    /**
     * 获取当前压缩文件内部路径
     */
    public String getCurrentCompressPath(ActivePanel panel) {
        return panelStates.get(panel).currentPath;
    }
    
    /**
     * 检查是否在压缩文件浏览模式
     */
    public boolean isInCompressMode(ActivePanel panel) {
        return panelStates.get(panel).compressFilePath != null;
    }
    
    // 私有辅助方法
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
        };
    }
    
    private boolean containsName(List<FileItem> items, String name) {
        for (FileItem item : items) {
            if (item.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    private void setPanelSence(ActivePanel panel, Sence sence) {
        if (panel == ActivePanel.LEFT) {
            activity.leftPanelSence = sence;
            activity.adapterLeft.setSence(sence);
        } else {
            activity.rightPanelSence = sence;
            activity.adapterRight.setSence(sence);
        }
    }
    
    /**
     * 压缩文件面板状态类
     */
    private static class CompressPanelState {
        String compressFilePath = null;
        String currentPath = "";
        List<String> entries = new ArrayList<>();
    }
    
    /**
     * 压缩文件条目类
     */
    private static class CompressFileItem extends FileItem {
        private final String entryPath;
        
        public CompressFileItem(String name, boolean isDirectory, long size, long lastModified, String entryPath) {
            super(new File(name));
            this.entryPath = entryPath;
        }
        
        @Override
        public String getPath() {
            return entryPath;
        }
        
        @Override
        public boolean isDirectory() {
            return getName().endsWith("/") || super.isDirectory();
        }
    }
}