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
import java.util.Arrays;

public class BottomMenuClickListener implements View.OnClickListener {

    private final Context context;

    public BottomMenuClickListener(Context context) {
        this.context = context;
    }

    @Override
    public void onClick(View v) {
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
                showBookmarkHistoryDialog(which == 1); // 0 = 书签，1 = 历史
            })
            .show();
    }
    
    private void showBookmarkHistoryDialog(boolean isHistory) {
        List<String> items = isHistory ? getHistory() : getBookmark();
        
        // 检查是否有数据
        if (items.isEmpty()) {
            Toast.makeText(context, 
                context.getString(isHistory ? R.string.no_history : R.string.no_bookmarks),
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        CharSequence[] itemsArray = items.toArray(new String[0]);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
            .setTitle(isHistory ? context.getString(R.string.his_dialog) : context.getString(R.string.bookmark_dialog))
            .setItems(itemsArray, (dialog, which) -> {
                String selectedPath = items.get(which);
                handleHistoryItemClick(selectedPath);
            })
            .setNeutralButton(context.getString(R.string.manage), (d, w) -> {
                // 管理按钮
                showManagementDialog(isHistory);
            })
            .setNegativeButton(context.getString(R.string.cancel), null);
            
        builder.show();
    }
    
    // 显示删除确认对话框
    private void showDeleteConfirmationDialog(boolean isHistory, int position, String path) {
        new MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.confirm_delete_title))
            .setMessage(context.getString(R.string.confirm_delete_message, new File(path).getName()))
            .setPositiveButton(context.getString(R.string.delete), (dialog, which) -> {
                // 执行删除
                deleteItem(isHistory, position);
                // 显示更新后的列表
                showBookmarkHistoryDialog(isHistory);
            })
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show();
    }
    
    // 删除单个项
    private void deleteItem(boolean isHistory, int position) {
        if (isHistory) {
            List<String> history = getHistory();
            if (position >= 0 && position < history.size()) {
                history.remove(position);
                Config.set("his.item", history);
                Config.set("his.length.num", history.size());
                Toast.makeText(context, context.getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
            }
        } else {
            List<String> bookmark = getBookmark();
            if (position >= 0 && position < bookmark.size()) {
                bookmark.remove(position);
                Config.set("bookmark.item", bookmark);
                Config.set("bookmark.length.num", bookmark.size());
                Toast.makeText(context, context.getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // 显示管理对话框
    private void showManagementDialog(boolean isHistory) {
        List<String> items = isHistory ? getHistory() : getBookmark();
        
        // 如果没有数据，提示用户
        if (items.isEmpty()) {
            Toast.makeText(context, 
                context.getString(isHistory ? R.string.no_history_to_manage : R.string.no_bookmarks_to_manage), 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建可勾选的选项数组
        boolean[] checkedItems = new boolean[items.size()];
        Arrays.fill(checkedItems, false);
        
        new MaterialAlertDialogBuilder(context)
            .setTitle(isHistory ? 
                     context.getString(R.string.manage_history) : 
                     context.getString(R.string.manage_bookmarks))
            .setMultiChoiceItems(items.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                // 更新选中状态
                checkedItems[which] = isChecked;
            })
            .setPositiveButton(context.getString(R.string.delete_selected), (dialog, which) -> {
                // 删除选中的项目
                // 从后往前删除避免索引问题
                List<Integer> positionsToDelete = new ArrayList<>();
                for (int i = checkedItems.length - 1; i >= 0; i--) {
                    if (checkedItems[i]) {
                        deleteItem(isHistory, i);
                    }
                }
                // 显示更新后的列表
                showBookmarkHistoryDialog(isHistory);
            })
            .setNeutralButton(context.getString(R.string.clear_all), (dialog, which) -> {
                // 清空所有
                showClearAllConfirmationDialog(isHistory);
            })
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show();
    }
    
    // 显示清空所有确认对话框
    private void showClearAllConfirmationDialog(boolean isHistory) {
        new MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.confirm_clear_title))
            .setMessage(context.getString(
                isHistory ? R.string.confirm_clear_history : R.string.confirm_clear_bookmarks))
            .setPositiveButton(context.getString(R.string.clear_all), (dialog, which) -> { // 使用 clear_all 字符串
                clearAllItems(isHistory);
                // 显示更新后的列表（会显示空状态提示）
                showBookmarkHistoryDialog(isHistory);
            })
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show();
    }
    
    // 清空所有项目
    private void clearAllItems(boolean isHistory) {
        if (isHistory) {
            Config.set("his.item", new JsonArray());
            Config.set("his.length.num", 0);
            Toast.makeText(context, context.getString(R.string.history_cleared), Toast.LENGTH_SHORT).show();
        } else {
            Config.set("bookmark.item", new JsonArray());
            Config.set("bookmark.length.num", 0);
            Toast.makeText(context, context.getString(R.string.bookmarks_cleared), Toast.LENGTH_SHORT).show();
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
            if (willLoad != null) {
                activity.loadDirectory(willLoad, ActivePanel.LEFT);
            }
        } else {
            File willLoad = activity.getRightDir().getParentFile();
            if (willLoad != null) {
                activity.loadDirectory(willLoad, ActivePanel.RIGHT);
            }
        }
    }
}