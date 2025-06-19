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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.manager.ssb.adapter.FileAdapter;
import com.manager.ssb.core.FileOpener;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.core.task.TaskNotificationManager;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.databinding.ActivityMainBinding;
import com.manager.ssb.model.FileItem;
import com.manager.ssb.core.dialog.SettingsDialogFragment;

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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private enum ActivePanel { LEFT, RIGHT }
    private ActivePanel activePanel = ActivePanel.LEFT;
    private File currentDirectoryLeft;
    private File currentDirectoryRight;
    private final List<FileItem> fileListLeft = new ArrayList<>();
    private final List<FileItem> fileListRight = new ArrayList<>();
    private FileAdapter adapterLeft;
    private FileAdapter adapterRight;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private NotifyingExecutorService executorService;
    private boolean storageInfoLoaded = false;
    private TaskNotificationManager notificationManager;

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;
    
    // 声明菜单操作映射表
    private final Map<Integer, Runnable> menuActionMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        notificationManager = new TaskNotificationManager(this);
        executorService = new NotifyingExecutorService(
            Executors.newFixedThreadPool(4),
            notificationManager,
            TaskTypes.MONITORED_TASKS
        );

        if (checkPermissions()) {
            initApp();
        } else {
            requestPermissions();
        }
    }

    /**
     * 检查权限状态
     */
    private boolean checkPermissions() {
        List<String> missingPermissions = new ArrayList<>();

        // 原有存储权限检查逻辑
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Android 11+ 需要特殊文件权限
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // 新增通知权限检查（仅Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // 如果有缺失权限需要请求
        return missingPermissions.isEmpty();
    }

    /**
     * 动态请求权限
     */
    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // 处理存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 跳转系统设置页申请 MANAGE_EXTERNAL_STORAGE
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
            showToast(getString(R.string.high_android_permission_request));
            return;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 请求 READ_EXTERNAL_STORAGE
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        // 添加通知权限请求（仅Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            requestPermissions(
                permissionsToRequest.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
            );
            showToast(getString(R.string.request_appect_android_permission_request));
        } else {
            // 不需要请求权限的情况（Android 5.1及以下）
            initApp();
        }
    }

    /**
     * 处理权限请求结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initApp();
                } else {
                    handlePermissionDenied();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
        
            // 检查所有请求的权限是否被授予
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                
                    // 特别处理通知权限
                    if (Manifest.permission.POST_NOTIFICATIONS.equals(permissions[i])) {
                        // 记录用户拒绝通知权限
                        this.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("notifications_denied", true)
                            .apply();
                    }
                }
            }

            if (allGranted) {
                initApp();
            } else {
                handlePermissionDenied();
            }
        }
    }


    /**
     * 权限被拒绝后的处理
     */
    private void handlePermissionDenied() {
        boolean shouldShowRationale = false;
    
        // 检查是否有权限需要显示解释
        for (String permission : getRequiredPermissions()) {
            if (shouldShowRequestPermissionRationale(permission)) {
                shouldShowRationale = true;
                break;
            }
        }

        if (shouldShowRationale) {
            showRationaleDialog();
        } else {
            showSettingsDialog();
        }
    }

    private List<String> getRequiredPermissions() {
        List<String> required = new ArrayList<>();
    
        // 存储权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            required.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    
        // 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS);
        }
    
        return required;
    }

    private void showRationaleDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_desc_dialog1)
            .setMessage(getRationaleMessage())
            .setPositiveButton(R.string.retry_request_android_permission, (dialog, which) -> {
                // 重新请求权限（排除用户永久拒绝的）
                List<String> toRequest = new ArrayList<>();
                for (String perm : getRequiredPermissions()) {
                    if (!isPermissionPermanentlyDenied(perm)) {
                        toRequest.add(perm);
                    }
                }
                if (!toRequest.isEmpty()) {
                    requestPermissions(toRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                } else {
                    showSettingsDialog();
                }
            })
            .setNegativeButton(R.string.exit, (dialog, which) -> finish())
            .show();
    }

    private boolean isPermissionPermanentlyDenied(String permission) {
        return !shouldShowRequestPermissionRationale(permission) &&
               (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }

    private String getRationaleMessage() {
        StringBuilder message = new StringBuilder(getString(R.string.permission_desc_dialog2));
    
        // 添加通知权限说明（如果被拒绝）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            isPermissionPermanentlyDenied(Manifest.permission.POST_NOTIFICATIONS)) {
            message.append("\n\n")
                   .append(getString(R.string.notification_permission_rationale));
        }
    
        return message.toString();
    }

    /**
     * 跳转应用设置页
     */
    private void showSettingsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reject_permanently_android_permission_request)
                .setMessage(R.string.go_to_setting_open)
                .setPositiveButton(R.string.go_set, (dialog, which) -> openAppSettings())
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void initApp() {
        Config.refresh();
        currentDirectoryLeft = Environment.getExternalStorageDirectory();
        currentDirectoryRight = Environment.getExternalStorageDirectory();
        
        setupRecyclerViews();
        loadBothPanels();
        
        if (!storageInfoLoaded) {
            updateStorageInfo();
            storageInfoLoaded = true;
        }
        
        initMenuActions();
    }
    
    private void initMenuActions() {
        menuActionMap.put(R.id.action_refresh, this::refreshCurrentDirectory);
        menuActionMap.put(R.id.action_settings, this::openSettings);
        menuActionMap.put(R.id.action_storage_info, this::showStorageDetails);
        menuActionMap.put(R.id.action_about, this::showAboutDialog);
        menuActionMap.put(R.id.action_terminal, this::startTerminal);
        menuActionMap.put(R.id.action_exit, this::finish);
    }

    private void setupRecyclerViews() {
        // 左侧适配器
        adapterLeft = new FileAdapter(fileListLeft, item -> handleItemClick(item, ActivePanel.LEFT),
                executorService, mainHandler);
        binding.rvFilesLeft.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFilesLeft.setAdapter(adapterLeft);

        // 右侧适配器
        adapterRight = new FileAdapter(fileListRight, item -> handleItemClick(item, ActivePanel.RIGHT),
                executorService, mainHandler);
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
    }
    
    private void setActivePanel(ActivePanel panel) {
        activePanel = panel;
        updatePathDisplay();
    }
    
    private void updatePathDisplay() {
        binding.tvCurrentPath.setText(getCurrentDirectory().getAbsolutePath());
    }

    private File getCurrentDirectory() {
        return activePanel == ActivePanel.LEFT ? currentDirectoryLeft : currentDirectoryRight;
    }

    private void loadBothPanels() {
        loadDirectory(currentDirectoryLeft, ActivePanel.LEFT);
        loadDirectory(currentDirectoryRight, ActivePanel.RIGHT);
    }


    private void loadDirectory(File directory, ActivePanel panel) {
        showLoading(true, panel);
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
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            mainHandler.post(() -> {
                showLoading(false, panel);
                updateFileList(newItems, panel);
                updatePanelDirectory(directory, panel);
            });
        }, TaskTypes.LOAD_FILES);
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

    private void updateFileList(List<FileItem> newItems, ActivePanel panel) {
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

    private void showLoading(boolean show, ActivePanel panel) {
        mainHandler.post(() -> {
            switch (panel) {
                case LEFT:
                    binding.progressBarLeft.setVisibility(show ? View.VISIBLE : View.GONE);
                    break;
                case RIGHT:
                    binding.progressBarRight.setVisibility(show ? View.VISIBLE : View.GONE);
                    break;
            }
        });
    }
    
    private void handleItemClick(FileItem item, ActivePanel panel) {
        if (item.isDirectory()) {
            File newDir = item.getFile();
            loadDirectory(newDir, panel);
        } else {
            FileOpener.openFile(this, item.getPath(), item.getName());
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
        SettingsDialogFragment settingsDialog = new SettingsDialogFragment();
        settingsDialog.show(getSupportFragmentManager(), "SettingsDialog");
    }

    private void startTerminal() {
        showToast(getString(R.string.wip_back));
        
        // try {
            // Intent intent = new Intent(MainActivity.this, Class.forName("com.termoneplus.TermActivity"));
            // startActivity(intent);
        // } catch (ClassNotFoundException e) {
            // showToast(getString(R.string.error));
        // }
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
                        if (isValid) {
                            switch (activePanel) {
                                case LEFT:
                                    currentDirectoryLeft = targetDir;
                                    loadDirectory(targetDir, ActivePanel.LEFT);
                                    break;
                                case RIGHT:
                                    currentDirectoryRight = targetDir;
                                    loadDirectory(targetDir, ActivePanel.RIGHT);
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
        StringBuilder sb = new StringBuilder("System Shell Box (C) 2025 by kgultrt\n\n");
    
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        executorService.shutdownNow(); // 关闭线程池
    }

}