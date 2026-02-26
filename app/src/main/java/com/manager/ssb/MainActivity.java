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

// MainActivity.java
package com.manager.ssb;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.manager.ssb.Application;
import com.manager.ssb.adapter.FileAdapter;
import com.manager.ssb.enums.ActivePanel;
import com.manager.ssb.enums.Sence;
import com.manager.ssb.core.FileOpener;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.core.task.TaskNotificationManager;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.core.function.FileLongClickHandler;
import com.manager.ssb.core.function.BottomMenuClickListener;
import com.manager.ssb.core.compress.CompressFileManager;
import com.manager.ssb.databinding.ActivityMainBinding;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.settings.SettingsActivity;
import com.manager.ssb.core.term.TerminalInstaller;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MainActivity extends AppCompatActivity {

    //整理
    private ActivityMainBinding binding;
    private File currentDirectoryLeft;
    private File currentDirectoryRight;
    private final List<FileItem> fileListLeft = new ArrayList<>();
    private final List<FileItem> fileListRight = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private NotifyingExecutorService executorService;
    private boolean storageInfoLoaded = false;
    private TaskNotificationManager notificationManager;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;
    private final Map<Integer, Runnable> menuActionMap = new HashMap<>();
    private CompressFileManager compressFileManager;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    
    public ActivePanel activePanel = ActivePanel.LEFT;
    public Sence leftPanelSence = Sence.FILE;
    public Sence rightPanelSence = Sence.FILE;
    public FileAdapter adapterLeft;
    public FileAdapter adapterRight;
    public boolean canSwichActivePanel = true;
    public final Handler disableHandler = new Handler();
    public final Runnable enableClicksRunnable = () -> {
        adapterLeft.setClickEnabled(true);
        adapterLeft.setLongClickEnabled(true);
        adapterRight.setClickEnabled(true);
        adapterRight.setLongClickEnabled(true);
        
        canSwichActivePanel = true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        Config.initialize();
        
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        notificationManager = new TaskNotificationManager(this);
        executorService = new NotifyingExecutorService(
            Executors.newFixedThreadPool(4),
            notificationManager,
            TaskTypes.MONITORED_TASKS
        );
        
        compressFileManager = new CompressFileManager(this, executorService);

        initApp();
    }

    private void initApp() {
        Config.initialize();
        
        currentDirectoryLeft = Environment.getExternalStorageDirectory();
        currentDirectoryRight = Environment.getExternalStorageDirectory();
        
        setupRecyclerViews();
        loadBothPanels();
        
        if (!storageInfoLoaded) {
            updateStorageInfo();
            storageInfoLoaded = true;
        }
        
        initMenuActions();
        
        boolean isFirst = Config.get("isFirst", true);
        int lastBuildNumber = Config.get("lastBuildNumber", 0);
        int currentBuildNumber = extractBuildNumber(getCurrentVersion());
        
        if (currentBuildNumber > lastBuildNumber) {
            if (lastBuildNumber == 0) {
                //do nothing
            } else {
                // 检测到新版本显示更新日志
                showUpdateDialog(currentBuildNumber);
                // 设置当前版本号
                Config.set("lastBuildNumber", currentBuildNumber);
            }
        }
        
        //test
        locateAndHighlightItem("/storage/emulated/0/aaa_123", ActivePanel.LEFT);
    }
    
    // 提取构建号的核心方法
    private int extractBuildNumber(String version) {
        // 使用正则提取末尾的数字
        Pattern pattern = Pattern.compile("build(\\d+)");
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0; // 默认值
    }

    // 获取当前版本号
    private String getCurrentVersion() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            return "0.0.0-build0";
        }
    }
    
    // 显示更新日志对话框
    private void showUpdateDialog(int buildNumber) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_title, buildNumber))
            .setMessage(R.string.update_log_content) // 使用单一更新日志资源
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }
    
    private void showWarningDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.warning_title)
            .setMessage(R.string.warning_content)
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(false)
            .show();
    }
    
    private void initMenuActions() {
        menuActionMap.put(R.id.action_refresh, this::refreshCurrentDirectory);
        menuActionMap.put(R.id.action_settings, this::openSettings);
        menuActionMap.put(R.id.action_jump_to_directory, this::showDirectoryInputDialog);
        menuActionMap.put(R.id.action_storage_info, this::showStorageDetails);
        menuActionMap.put(R.id.action_about, this::showAboutDialog);
        menuActionMap.put(R.id.action_terminal, this::startTerminal);
        menuActionMap.put(R.id.action_exit, this::exitTheApp);
    }

    private void setupRecyclerViews() {
        
        FileLongClickHandler longClickHandler = new FileLongClickHandler(this, executorService, activePanel);

        // 左侧适配器
        adapterLeft = new FileAdapter(
            fileListLeft,
            item -> handleItemClick(item, ActivePanel.LEFT),
            (item, view) -> longClickHandler.handle(item, view, ActivePanel.LEFT),
            "left",
            Sence.FILE,
            executorService,
            mainHandler
        );

        binding.rvFilesLeft.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFilesLeft.setAdapter(adapterLeft);

        // 右侧适配器
        adapterRight = new FileAdapter(
            fileListRight,
            item -> handleItemClick(item, ActivePanel.RIGHT),
            (item, view) -> longClickHandler.handle(item, view, ActivePanel.RIGHT),
            "right",
            Sence.FILE,
            executorService,
            mainHandler
        );
        binding.rvFilesRight.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFilesRight.setAdapter(adapterRight);

        // 面板点击监听
        binding.rvFilesLeft.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    setActivePanel(ActivePanel.LEFT);
                }
                return false; // 不拦截事件，事件继续往下传递
            }
        });

        binding.rvFilesRight.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    setActivePanel(ActivePanel.RIGHT);
                }
                return false; // 不拦截事件，事件继续往下传递
            }
        });
        
        binding.btnMenu.setOnClickListener(v -> showPopupMenu());
        binding.tvCurrentPath.setOnClickListener(v -> showDirectoryInputDialog());
        binding.tvStorage.setOnClickListener(v -> showStorageDetails());
        
        // 创建 BottomMenuClickListener 的实例
        BottomMenuClickListener bottomMenuClickListener = new BottomMenuClickListener(this);

        // 为按钮设置监听器
        binding.btnSync.setOnClickListener(bottomMenuClickListener);
        binding.btnCreate.setOnClickListener(bottomMenuClickListener);
        binding.btnBookmarkHistory.setOnClickListener(bottomMenuClickListener);
        binding.btnBack.setOnClickListener(bottomMenuClickListener);
        
        // 设置侧边栏按钮点击事件
        binding.btnDrawer.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        
        // 初始化侧边栏
        initDrawer();
    }
    
    private void initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                
                if (id == R.id.nav_root) {
                    // 跳转到根目录
                    loadDirectory(new File("/"), activePanel);
                } else if (id == R.id.nav_storage) {
                    // 跳转到内部存储
                    loadDirectory(Environment.getExternalStorageDirectory(), activePanel);
                }
                
                // 关闭侧边栏
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }
    }
    
    private void setActivePanel(ActivePanel panel) {
        if (canSwichActivePanel) {
            activePanel = panel;
            updatePathDisplay();
        }
    }
    
    public void updatePathDisplay() {
        String displayText;
        
        // 检查是否在压缩文件浏览模式
        if (compressFileManager.isInCompressMode(activePanel)) {
            String compressPath = compressFileManager.getCurrentCompressPath(activePanel);
            String compressFile = compressFileManager.getCurrentCompressFilePath(activePanel);
            displayText = "zip:" + compressFile + (compressPath.isEmpty() ? "" : "/" + compressPath);
        } else {
            displayText = getCurrentDirectory().getAbsolutePath();
        }
        
        binding.tvCurrentPath.setText(displayText);
    }

    private File getCurrentDirectory() {
        return activePanel == ActivePanel.LEFT ? currentDirectoryLeft : currentDirectoryRight;
    }

    private void loadBothPanels() {
        loadDirectory(currentDirectoryLeft, ActivePanel.LEFT);
        loadDirectory(currentDirectoryRight, ActivePanel.RIGHT);
    }

    private void updatePanelDirectory(File directory, ActivePanel panel) {
        switch (panel) {
            case LEFT:
                currentDirectoryLeft = directory;
                break;
            case RIGHT:
                currentDirectoryRight = directory;
                break;
        }
        if (panel == activePanel) updatePathDisplay();
    }

    public void updateFileList(List<FileItem> newItems, ActivePanel panel) {
        switch (panel) {
            case LEFT:
                updateSingleList(fileListLeft, newItems, adapterLeft);
                break;
            case RIGHT:
                updateSingleList(fileListRight, newItems, adapterRight);
                break;
        }
    }
    
    private void updateSingleList(List<FileItem> targetList, List<FileItem> newItems, FileAdapter adapter) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new FileDiffCallback(targetList, newItems));
        targetList.clear();
        targetList.addAll(newItems);
        result.dispatchUpdatesTo(adapter);
    }
    
    private void handleItemClick(FileItem item, ActivePanel panel) {
        // 检查是否在压缩文件浏览模式
        if (compressFileManager.isInCompressMode(panel)) {
            compressFileManager.handleCompressItemClick(item, panel);
            return;
        }
        
        if (item.isDirectory()) {
            File newDir = item.getFile();
            loadDirectory(newDir, panel);
            BottomMenuClickListener hs = new BottomMenuClickListener(this);
            hs.addToHistory(newDir.getAbsolutePath());
        } else {
            FileOpener.openFile(this, item.getPath(), item.getName());
            BottomMenuClickListener hs = new BottomMenuClickListener(this);
            hs.addToHistory(item.getPath());
        }
    }


    private FileItem createParentDirectoryItem(File currentDir) {
        return new FileItem(currentDir.getParentFile()) {
            @Override
            public String getName() {
                return "..";
            }

            @Override
            public boolean isDirectory() {
                return true;
            }
        };
    }

    // 兼容低版本的存储信息获取
    private void updateStorageInfo() {
        executorService.submit(() -> {
            try {
                StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
                long total = stat.getTotalBytes();
                long free = stat.getFreeBytes();
                long used = total - free;

                String info = String.format(getString(R.string.disk_info_used) + ": %s / " + 
                        getString(R.string.disk_info_all) + ": %s",
                        FileAdapter.formatSize(this, used),
                        FileAdapter.formatSize(this, total));

                mainHandler.post(() -> {
                    binding.tvStorage.setText(info);
                    binding.progressBarStorage.setMax((int) (total / 1024));
                    binding.progressBarStorage.setProgress((int) (used / 1024));
                });
            } catch (Exception e) {
                mainHandler.post(() -> binding.tvStorage.setText(R.string.disk_info_unavailable));
            }
        }, TaskTypes.UP_STOR_INF);
    }

    private boolean isRootDirectory(File directory) {
        return directory.getParent() == null || directory.getPath().equals("/");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    static class FileDiffCallback extends DiffUtil.Callback {
        private final List<FileItem> oldList;
        private final List<FileItem> newList;

        FileDiffCallback(List<FileItem> oldList, List<FileItem> newList) {
            this.oldList = new ArrayList<>(oldList);
            this.newList = new ArrayList<>(newList);
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getPath().equals(newList.get(newItemPosition).getPath());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            FileItem oldItem = oldList.get(oldItemPosition);
            FileItem newItem = newList.get(newItemPosition);
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getSize() == newItem.getSize()
                    && oldItem.getLastModified() == newItem.getLastModified()
                    && oldItem.isDirectory() == newItem.isDirectory();
        }
    }

    // 添加弹出菜单实现
    private void showPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(this, binding.btnMenu);
        popupMenu.inflate(R.menu.main_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            Runnable action = menuActionMap.get(item.getItemId());
            if (action != null) {
                action.run(); // 执行对应的操作
                return true;  // 事件已处理
            }
            return false; // 未处理的事件
        });

        popupMenu.show();
    }
    
    // 相关功能方法
    private void refreshCurrentDirectory() {
        loadBothPanels();
        showToast(getString(R.string.refresh));
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void startTerminal() {
        // 启动终端逻辑
        try {
            Intent intent = new Intent(MainActivity.this, Class.forName("com.manager.ssb.termux.TermuxActivity"));
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            showToast(getString(R.string.error));
        }
    }

    private void showStorageDetails() {
        executorService.execute(() -> {
            final String message = getStorageDetailsMessage();
            mainHandler.post(() -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.storage_info)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            });
        }, TaskTypes.STOR_DIALOG);
    }

    private String getStorageDetailsMessage() {
        try {
            StatFs stat = new StatFs(currentDirectoryLeft.getPath());
            long total = stat.getTotalBytes();
            long free = stat.getFreeBytes();
            long used = total - free;

            return String.format(
                    "%s: %s\n%s: %s\n%s: %s",
                    getString(R.string.disk_info_all),
                    FileAdapter.formatSize(this, total),
                    getString(R.string.disk_info_used),
                    FileAdapter.formatSize(this, used),
                    getString(R.string.disk_info_available),
                    FileAdapter.formatSize(this, free)
            );
        } catch (Exception e) {
            return getString(R.string.disk_info_unavailable);
        }
    }

    private void showDirectoryInputDialog() {
        TextInputLayout textInputLayout = new TextInputLayout(this);
        TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
        textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        textInputLayout.setHint(getString(R.string.input_directory_hint));
        textInputLayout.addView(editText);

        editText.setText(getCurrentDirectory().getAbsolutePath());
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.jump_to_directory)
                .setView(textInputLayout)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String newPath = editText.getText().toString().trim();
                executorService.execute(() -> {
                    File targetDir = new File(newPath);
                    boolean isValid = targetDir.isDirectory() && targetDir.canRead();
                    mainHandler.post(() -> {
                        BottomMenuClickListener hs = new BottomMenuClickListener(this);
                        
                        if (isValid) {
                            switch (activePanel) {
                                case LEFT:
                                    currentDirectoryLeft = targetDir;
                                    loadDirectory(targetDir, ActivePanel.LEFT);
                                    hs.addToHistory(targetDir.getAbsolutePath());
                                    break;
                                case RIGHT:
                                    currentDirectoryRight = targetDir;
                                    loadDirectory(targetDir, ActivePanel.RIGHT);
                                    hs.addToHistory(targetDir.getAbsolutePath());
                                    break;
                            }
                            dialog.dismiss();
                        } else {
                            showToast(getString(R.string.invalid_directory));
                        }
                    });
                });
            });
        });
        dialog.show();
    }
    
    
    
    private void showAboutDialog() {
        StringBuilder sb = new StringBuilder("System Shell Box (C) 2025-2026 by kgultrt\n");
        sb.append("Handle all documents.\nDevelop on MT manager Text Editor and Termux.\nmade on android\n\n");
    
        // 应用信息
        try {
            PackageInfo pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
            sb.append(getString(R.string.ver)).append(": ").append(pkg.versionName)
            .append(" (").append(pkg.versionCode).append(")\n");
        } catch (Exception e) { sb.append("(Unknown Version)\n"); }

        // 构建信息
        try {
            long timestamp = Long.parseLong(BuildConfig.BUILD_TIME);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sb.append(getString(R.string.compilation_time)).append(": ")
            .append(sdf.format(new Date(timestamp))).append("\n");
        } catch (Exception e) { sb.append("(Unknown Time)\n"); }

        try {
            String buildType = BuildConfig.BUILD_TYPE.toLowerCase(Locale.US);
            String type = buildType.contains("debug") ? "Debug" : 
                         buildType.contains("release") ? "Release" : "Unknown";
            sb.append(getString(R.string.build_type)).append(": ").append(type).append("\n");
        } catch (Exception e) { sb.append("(Unknown Build Type)\n"); }
        
        try { sb.append(getString(R.string.term_install_status)).append(": ")
                .append(TerminalInstaller.getCurrentVersionInfo()).append("\n"); } 
        catch (Exception e) { /* 忽略 */ }

        // Git信息
        try { sb.append(getString(R.string.git_commit_short_hash)).append(": ")
                .append(BuildConfig.GIT_COMMIT_SHORT_HASH).append("\n"); } 
        catch (Exception e) { /* 忽略 */ }
    
        try { sb.append(getString(R.string.git_commit_author)).append(": ")
                .append(BuildConfig.GIT_COMMIT_AUTHOR).append("\n"); } 
        catch (Exception e) { /* 忽略 */ }
    
        try { sb.append(getString(R.string.git_branch_name)).append(": ")
                .append(BuildConfig.GIT_BRANCH_NAME); } 
        catch (Exception e) { /* 忽略 */ }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about)
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    // 一些公共方法
    public void refreshAllPanels() {
        loadBothPanels();
    }

    public boolean getActivePanel() {
        if (activePanel == ActivePanel.LEFT) {
            return true;
        } else {
            return false;
        }
    }

    public File getLeftDir() {
        return currentDirectoryLeft;
    }
    
    public File getRightDir() {
        return currentDirectoryRight;
    }
    
    public File getCurrentDir() {
        if (activePanel == ActivePanel.LEFT) {
            return currentDirectoryLeft;
        } else {
            return currentDirectoryRight;
        }
    }
    
    public Sence getCurrentSence() {
        if (activePanel == ActivePanel.LEFT) {
            return adapterLeft.getSence();
        } else {
            return adapterRight.getSence();
        }
    }
    
    public CompressFileManager getCompressFileManager() {
        return compressFileManager;
    }
    
    public void loadDirectory(File directory, ActivePanel panel) {
        executorService.submit(() -> {
            List<FileItem> newItems = new ArrayList<>();

            if (!isRootDirectory(directory)) {
                newItems.add(createParentDirectoryItem(directory));
            }

            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    newItems.add(new FileItem(file));
                }
            }

            Collections.sort(newItems, (a, b) -> {
                // 优先处理".."目录
                boolean aIsParent = "..".equals(a.getName());
                boolean bIsParent = "..".equals(b.getName());
            
                if (aIsParent && bIsParent) return 0;
                if (aIsParent) return -1;  // a是".."则排在前面
                if (bIsParent) return 1;   // b是".."则a排在后面
            
                // 原始排序规则
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            mainHandler.post(() -> {
                updateFileList(newItems, panel);
                updatePanelDirectory(directory, panel);
            });
        }, TaskTypes.LOAD_FILES);
    }
    
    public void onBackPressedCall() {
        
        // 如果侧边栏是打开的，先关闭侧边栏
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        
        // 如果当前在压缩文件浏览模式，先退出压缩文件
        if (compressFileManager.isInCompressMode(activePanel)) {
            compressFileManager.exitCompressFile(activePanel);
            return;
        }
        
        File willLoad;
        switch (activePanel) {
            case LEFT:
                willLoad = currentDirectoryLeft.getParentFile();
                if (willLoad != null) {
                    loadDirectory(willLoad, ActivePanel.LEFT);
                } else {
                    showExitDialog(); // 根目录处理：显示退出对话框
                }
                break;
            case RIGHT:
                willLoad = currentDirectoryRight.getParentFile();
                if (willLoad != null) {
                    loadDirectory(willLoad, ActivePanel.RIGHT);
                } else {
                    showExitDialog(); // 根目录处理：显示退出对话框
                }
                break;
        }
    }
    
    // 定位并高亮项目
    public void locateAndHighlightItem(String itemPath, ActivePanel panel) {
        // 清除之前的高亮
        clearAllHighlights();
    
        // 根据面板设置高亮
        switch (panel) {
            case LEFT:
                adapterLeft.highlightItem(itemPath);
                break;
            case RIGHT:
                adapterRight.highlightItem(itemPath);
                break;
        }
    }

    // 清除所有高亮
    public void clearAllHighlights() {
        adapterLeft.clearHighlight();
        adapterRight.clearHighlight();
    }

    // 检查项目是否在当前目录中
    public boolean isItemInCurrentDirectory(String itemPath, ActivePanel panel) {
        File currentDir = getCurrentDirectory();
        File itemFile = new File(itemPath);
    
        // 检查项目是否在当前目录中
        return itemFile.getParent() != null && 
               itemFile.getParent().equals(currentDir.getAbsolutePath());
    }

    private void showExitDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.exit_dialog))
               .setMessage(getString(R.string.exit_dialog_c))
               .setPositiveButton(R.string.ok, (dialog, which) -> exitTheApp())
               .setNegativeButton(R.string.cancel, null)
               .show();
    }
    
    public void exitTheApp() {
        notificationManager.clearAll(); //清除通知
        finish(); //完成退出
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        executorService.shutdownNow(); // 关闭线程池
    }
    
    @Override
    public void onBackPressed() {
        onBackPressedCall();
    }
}