package com.manager.ssb.core.function;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.R;
import com.manager.ssb.MainActivity;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.Application;
import com.manager.ssb.core.config.Config;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class BottomMenuClickListener implements View.OnClickListener {

    private final Context context;

    public BottomMenuClickListener(Context context) {
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        // 使用 if-else
        if (v.getId() == R.id.btn_sync) {
            handleSync();
        } else if (v.getId() == R.id.btn_create) {
            handleCreate();
        } else if (v.getId() == R.id.btn_bookmark_history) {
            handleBookmarkHistory();
        } else if (v.getId() == R.id.btn_back) {
            handleBack();
        }
    }

    private void handleSync() {
        MainActivity activity = (MainActivity) context;
        
        boolean isLeftPanel = activity.getActivePanel();
        
        if (isLeftPanel) {
            File willLoad = activity.getLeftDir();
            activity.loadDirectory(willLoad, ActivePanel.RIGHT);
        } else {
            File willLoad = activity.getRightDir();
            activity.loadDirectory(willLoad, ActivePanel.LEFT);
        }
    }

    private void handleCreate() {
        // 使用 MD3 风格的对话框
        new MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.create_dialog))
            .setItems(new CharSequence[]{context.getString(R.string.create_dir), context.getString(R.string.create_file)}, (dialog, which) -> {
                // 根据选择类型执行不同操作
                showCreateDialog(which == 0); // 0 = 文件夹，1 = 文件
            })
            .show();
    }

    private void showCreateDialog(boolean isDirectory) {
        // 提示用户输入名称
        final EditText input = new EditText(context);
        input.setHint(isDirectory ? context.getString(R.string.input_dir) : context.getString(R.string.input_file));

        // 使用 MD3 风格的对话框
        new MaterialAlertDialogBuilder(context)
            .setTitle(isDirectory ? context.getString(R.string.create_dir) : context.getString(R.string.create_file))
            .setView(input)
            .setPositiveButton(context.getString(R.string.create_dialog_ok), (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.name_is_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                // 根据当前面板，获取相应的目录
                MainActivity activity = (MainActivity) context;
                boolean isLeftPanel = activity.getActivePanel();
                File parentDir = isLeftPanel ? activity.getLeftDir() : activity.getRightDir();

                // 创建文件夹或文件
                File newFile;
                if (isDirectory) {
                    newFile = new File(parentDir, name);
                    if (!newFile.exists()) {
                        newFile.mkdir();
                    } else {
                        Toast.makeText(context, context.getString(R.string.direx), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    newFile = new File(parentDir, name);
                    try {
                        if (!newFile.exists()) {
                            newFile.createNewFile();
                        } else {
                            Toast.makeText(context, context.getString(R.string.fileex), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, context.getString(R.string.create_fail), Toast.LENGTH_SHORT).show();
                    }
                }

                // 创建完成后刷新面板
                activity.refreshAllPanels();
            })
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show();
    }

    public void addToHistory(String path) {
        List<String> history = getHistory();
        // 如果路径存在，移除旧的记录
        history.remove(path);
        // 如果超过最大限制，删除最早的记录
        if (history.size() >= 100) {
            history.remove(0);
        }
        // 添加新路径
        history.add(path);
        // 更新配置
        Config.set("his.item", history);
        Config.set("his.length.num", history.size());
    }
    
    public void addToBookmark(String path) {
        List<String> bookmark = getBookmark();
        // 如果路径存在，移除旧的记录
        bookmark.remove(path);
        // 添加新路径
        bookmark.add(path);
        // 更新配置
        Config.set("bookmark.item", bookmark);
        Config.set("bookmark.length.num", bookmark.size());
    }

    private List<String> getHistory() {
        // 从 Config 获取历史记录的 JSON 数据
        JsonElement historyElement = Config.get("his.item", new JsonArray());

        List<String> history = new ArrayList<>();
    
        // 检查返回的 JsonElement 是否是 JsonArray 类型
        if (historyElement.isJsonArray()) {
            JsonArray jsonArray = historyElement.getAsJsonArray();
        
            // 将 JsonArray 转换为 List<String>
            for (JsonElement element : jsonArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    history.add(element.getAsString());
                }
            }
        }

        return history;
    }
    
    private List<String> getBookmark() {
        // 从 Config 获取历史记录的 JSON 数据
        JsonElement bookmarkElement = Config.get("bookmark.item", new JsonArray());

        List<String> bookmark = new ArrayList<>();
    
        // 检查返回的 JsonElement 是否是 JsonArray 类型
        if (bookmarkElement.isJsonArray()) {
            JsonArray jsonArray = bookmarkElement.getAsJsonArray();
        
            // 将 JsonArray 转换为 List<String>
            for (JsonElement element : jsonArray) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    bookmark.add(element.getAsString());
                }
            }
        }

        return bookmark;
    }


    private void handleBookmarkHistory() {        
        new MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.bookmark))
            .setItems(new CharSequence[]{context.getString(R.string.bookmark_dialog), context.getString(R.string.his_dialog)}, (dialog, which) -> {
                // 根据选择类型执行不同操作
                showBookmarkHistoryDialog(which == 0); // 0 = HIS，1 = BOOK
            })
            .show();
    }
    
    private void showBookmarkHistoryDialog(boolean isHistory) {
        List<String> history = getHistory();
        
        if (isHistory == false) {
            new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.his_dialog))
                .setItems(history.toArray(new String[0]), (dialog, which) -> {
                    String selectedPath = history.get(which);
                    handleHistoryItemClick(selectedPath); // 进入历史记录目录
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
        } else {
            List<String> bookmark = getBookmark();
            
            new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.bookmark_dialog))
                .setItems(bookmark.toArray(new String[0]), (dialog, which) -> {
                    String selectedPath = bookmark.get(which);
                    handleHistoryItemClick(selectedPath);
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
        }
    }

    private void handleHistoryItemClick(String path) {
        MainActivity activity = (MainActivity) context;
        File target = new File(path);
        File directoryToOpen;

        if (target.exists()) {
            if (target.isDirectory()) {
                directoryToOpen = target;
            } else {
                directoryToOpen = target.getParentFile();
            }

            if (directoryToOpen != null && directoryToOpen.exists() && directoryToOpen.isDirectory()) {
                boolean isLeftPanel = activity.getActivePanel();
                if (isLeftPanel) {
                    activity.loadDirectory(directoryToOpen, ActivePanel.LEFT);
                } else {
                    activity.loadDirectory(directoryToOpen, ActivePanel.RIGHT);
                }
            } else {
                Toast.makeText(context, context.getString(R.string.his_dialog_fail), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, context.getString(R.string.his_dialog_fail), Toast.LENGTH_SHORT).show();
        }
    }


    private void handleBack() {
        MainActivity activity = (MainActivity) context;
        
        boolean isLeftPanel = activity.getActivePanel();
        
        if (isLeftPanel) {
            File willLoad = activity.getLeftDir().getParentFile();
            activity.loadDirectory(willLoad, ActivePanel.LEFT);
        } else {
            File willLoad = activity.getRightDir().getParentFile();
            activity.loadDirectory(willLoad, ActivePanel.RIGHT);
        }
    }
}

