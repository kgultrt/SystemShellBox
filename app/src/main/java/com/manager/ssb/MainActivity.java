// MainActivity.java
package com.manager.ssb;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.content.Context;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.manager.ssb.adapter.FileAdapter;
import com.manager.ssb.core.FileOpener;
import com.manager.ssb.core.task.TaskListener;
import com.manager.ssb.core.task.NotifyingExecutorService;
import com.manager.ssb.core.task.TaskNotificationManager;
import com.manager.ssb.core.task.TaskTypes;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.core.dialog.SettingsDialogFragment;
import com.manager.ssb.databinding.ActivityMainBinding;
import com.manager.ssb.model.FileItem;

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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private File currentDirectory;
    private final List<FileItem> fileList = new ArrayList<>();
    private FileAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private NotifyingExecutorService executorService; // 使用 NotifyingExecutorService
    private boolean storageInfoLoaded = false; // 避免重复加载存储信息
    private TaskNotificationManager notificationManager; // 新增通知管理器

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        notificationManager = new TaskNotificationManager(this);
        executorService = new NotifyingExecutorService(
            Executors.newFixedThreadPool(4), 
            notificationManager, // 直接使用通知管理器作为监听器
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
        showToast(Config.get("appName", "No Name"));
        currentDirectory = Environment.getExternalStorageDirectory();
        setupRecyclerView();
        loadFiles(currentDirectory);
        if (!storageInfoLoaded) {
            updateStorageInfo();
            storageInfoLoaded = true;
        }
    }

    private void setupRecyclerView() {
        adapter = new FileAdapter(fileList, item -> {
            if (item.isDirectory()) {
                currentDirectory = item.getFile();
                loadFiles(currentDirectory);
            } else {
                //  使用新的文件打开方式
                FileOpener.openFile(this, item.getPath(), item.getName());
            }
        }, executorService, mainHandler); // 传递线程池和 Handler
        binding.rvFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFiles.setAdapter(adapter);

        binding.btnMenu.setOnClickListener(v -> showPopupMenu());

        binding.tvCurrentPath.setOnClickListener(v -> showDirectoryInputDialog());
        binding.tvStorage.setOnClickListener(v -> showStorageDetails());
    }

    private void loadFiles(File directory) {
        showLoading(true);
        executorService.submit(() -> {
            List<FileItem> newItems = new ArrayList<>();

            // 添加父目录项
            if (!isRootDirectory(directory)) {
                newItems.add(createParentDirectoryItem(directory));
            }

            // 获取并处理文件列表
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    FileItem item = new FileItem(file);
                    newItems.add(item);
                }
            }

            // 排序（目录优先）
            Collections.sort(newItems, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            // 更新UI
            mainHandler.post(() -> {
                showLoading(false);
                updateFileList(newItems);
                binding.tvCurrentPath.setText(directory.getAbsolutePath());
            });
        }, TaskTypes.LOAD_FILES);
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

    private void updateFileList(List<FileItem> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new FileDiffCallback(fileList, newItems));
        fileList.clear();
        fileList.addAll(newItems);
        result.dispatchUpdatesTo(adapter);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.progressBar.setIndeterminate(show);
    }

    // 兼容低版本的存储信息获取
    private void updateStorageInfo() {
        executorService.submit(() -> {
            try {
                File path = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long total = stat.getTotalBytes();
                long free = stat.getFreeBytes();
                long used = total - free;

                String info = String.format(getString(R.string.disk_info_used) + ": %s / " + getString(R.string.disk_info_all) + ": %s",
                        FileAdapter.formatSize(this, used),
                        FileAdapter.formatSize(this, total));

                mainHandler.post(() -> {
                    binding.tvStorage.setText(info);
                    binding.progressBar.setMax((int) (total / 1024));
                    binding.progressBar.setProgress((int) (used / 1024));
                });
            } catch (IllegalArgumentException e) {
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

        // 添加菜单项点击监听
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_refresh) {
                refreshCurrentDirectory();
                return true;
            } else if (id == R.id.action_settings) {
                openSettings();
                return true;
            } else if (id == R.id.action_storage_info) {
                showStorageDetails();
                return true;
            } else if (id == R.id.action_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.action_terminal) {
                startTerminal();
                return true;
            } else if (id == R.id.action_exit) {
                finish();
                return true;
            }

            return false;
        });

        // 显示菜单
        popupMenu.show();
    }
    
    // 相关功能方法
    private void refreshCurrentDirectory() {
        loadFiles(currentDirectory);
        showToast(getString(R.string.refresh));
    }

    private void openSettings() {
        SettingsDialogFragment settingsDialog = new SettingsDialogFragment();
        settingsDialog.show(getSupportFragmentManager(), "SettingsDialog");
    }

    private void startTerminal() {
        try {
            Intent intent = new Intent(this, Class.forName("com.rk.terminal.ui.activities.terminal.MainActivity"));
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
            StatFs stat = new StatFs(currentDirectory.getPath());
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
        // 使用 Material 设计的 TextInputLayout 包裹 TextInputEditText
        TextInputLayout textInputLayout = new TextInputLayout(this);
        TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());

        // 设置 MD3 样式属性
        textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE); // 边框样式
        textInputLayout.setHint(getString(R.string.input_directory_hint));            // 输入提示
        textInputLayout.setPadding(
                getResources().getDimensionPixelSize(R.dimen.dialog_padding),  // 左边距
                0,  // 上边距
                getResources().getDimensionPixelSize(R.dimen.dialog_padding),  // 右边距
                0   // 下边距
        );

        // 设置输入框参数
        editText.setSingleLine(true);
        editText.setText(currentDirectory.getAbsolutePath());
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); // 关闭自动建议

        // 将 EditText 添加到 TextInputLayout
        textInputLayout.addView(editText);

        // 构建对话框
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.jump_to_directory)
                .setView(textInputLayout)
                .setPositiveButton(R.string.confirm, null) // 先设置为空
                .setNegativeButton(R.string.cancel, null); // 先设置为空

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String newPath = editText.getText().toString().trim();
                executorService.execute(() -> { // 后台线程验证
                    File targetDir = new File(newPath);
                    final boolean isValid = targetDir.isDirectory() && targetDir.canRead();
                    mainHandler.post(() -> {
                        if (isValid) {
                            currentDirectory = targetDir;
                            loadFiles(targetDir);
                            dialog.dismiss(); // 验证通过后关闭对话框
                        } else {
                            showToast(getString(R.string.invalid_directory));
                        }
                    });
                });
            });

            MaterialButton negativeButton = (MaterialButton) dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> dialog.dismiss());
        });

        dialog.show();
    }
    
    
    
    
    private void showAboutDialog() {
        String aboutMessage = String.format(
                "System Shell Box (C) 2025 by kgultrt\n\n%s\n%s\n%s\n%s\n%s\n%s",
                getAppVersion(),
                getBuildTime(),
                getBuildType(),
                getGitCommitShortHash(),
                getGitCommitAuthor(),
                getGitBranchName()
        );

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about)
                .setMessage(aboutMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String getGitCommitShortHash() {
        try {
            String commitHash = BuildConfig.GIT_COMMIT_SHORT_HASH;
            String commitText = getString(R.string.git_commit_short_hash);
            return commitText + ": " + commitHash;
        } catch (Exception e) {
            return "(Unknown Commit Hash)";
        }
    }


    private String getGitCommitAuthor() {
        try {
            String commitAuthor = BuildConfig.GIT_COMMIT_AUTHOR;
            String authorText = getString(R.string.git_commit_author);
            return authorText + ": " + commitAuthor;
        } catch (Exception e) {
            return "(Unknown Commit Author)";
        }
    }


    private String getGitBranchName() {
        try {
            String branchName = BuildConfig.GIT_BRANCH_NAME;
            String branchText = getString(R.string.git_branch_name);
            return branchText + ": " + branchName;
        } catch (Exception e) {
            return "(Unknown Branch Name)";
        }
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionText = getString(R.string.ver);
            return versionText + ": " + pInfo.versionName + " (Version Code " + pInfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "(Unknow Version)";
        }
    }

    private String getBuildTime() {
        try {
            long timestamp = Long.parseLong(BuildConfig.BUILD_TIME);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            String timeText = getString(R.string.compilation_time);
            return timeText + ": " + sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "(Unknow Time)";
        }
    }

    private String getBuildType() {
        try {
            String typeText = getString(R.string.build_type); // 例如："构建类型"
            String buildType = BuildConfig.BUILD_TYPE.toLowerCase(Locale.US);
            String type;
            if (buildType.contains("debug")) {
                type = getString(R.string.build_debug);
            } else if (buildType.contains("release")) {
                type = getString(R.string.build_release);
            } else {
                type = getString(R.string.build_unknown); // 其他类型
            }
            return typeText + ": " + type;
        } catch (Exception e) {
            return "(Unknow Build Type)";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        executorService.shutdownNow(); // 关闭线程池
    }

}