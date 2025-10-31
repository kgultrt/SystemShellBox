package com.manager.ssb;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.adapter.WelcomePagerAdapter;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.model.WelcomePage;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext, btnPrevious, btnGetStarted;
    private ProgressBar progressBar;
    private LinearLayout indicatorLayout;
    private TextView tvStepInfo;
    
    private WelcomePagerAdapter adapter;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002;
    
    private String[] requiredPermissions;
    private int currentStep = 0;
    private boolean allPermissionsGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        
        // 检查是否已经完成向导
        if (isWelcomeCompleted()) {
            startMainActivity();
            return;
        }
        
        initViews();
        setupPermissions();
        setupViewPager();
        updateNavigation();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        progressBar = findViewById(R.id.progressBar);
        indicatorLayout = findViewById(R.id.indicatorLayout);
        tvStepInfo = findViewById(R.id.tvStepInfo);
        
        btnNext.setOnClickListener(v -> navigateNext());
        btnPrevious.setOnClickListener(v -> navigatePrevious());
        btnGetStarted.setOnClickListener(v -> onGetStartedClicked());
    }

    private void setupPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // 存储权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要特殊文件权限，通过 Intent 处理
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        
        // 通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        requiredPermissions = permissions.toArray(new String[0]);
    }

    private void setupViewPager() {
        List<WelcomePage> pages = new ArrayList<>();
        
        // 欢迎页面
        pages.add(new WelcomePage(
            android.R.drawable.ic_dialog_info,
            getString(R.string.welcome_title_1),
            getString(R.string.welcome_desc_1),
            false
        ));
        
        // 权限说明页面
        pages.add(new WelcomePage(
            android.R.drawable.ic_dialog_info,
            getString(R.string.welcome_title_2),
            getString(R.string.welcome_desc_2),
            false
        ));
        
        // 功能特性页面
        pages.add(new WelcomePage(
            android.R.drawable.ic_dialog_info,
            getString(R.string.welcome_title_3),
            getString(R.string.welcome_desc_3),
            false
        ));
        
        // 权限请求页面
        pages.add(new WelcomePage(
            android.R.drawable.ic_dialog_info,
            getString(R.string.welcome_title_4),
            getString(R.string.welcome_desc_4),
            true
        ));
        
        adapter = new WelcomePagerAdapter(pages);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // 禁用滑动
        
        setupIndicators(pages.size());
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentStep = position;
                updateNavigation();
                updateIndicators(position);
            }
        });
    }

    private void setupIndicators(int count) {
        indicatorLayout.removeAllViews();
        for (int i = 0; i < count; i++) {
            View indicator = new View(this);
            int size = getResources().getDimensionPixelSize(R.dimen.indicator_size);
            int margin = getResources().getDimensionPixelSize(R.dimen.indicator_margin);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(R.drawable.indicator_dot);
            indicatorLayout.addView(indicator);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicatorLayout.getChildCount(); i++) {
            View indicator = indicatorLayout.getChildAt(i);
            boolean isSelected = i == position;
            indicator.setSelected(isSelected);
        }
    }

    private void updateNavigation() {
        boolean isLastPage = currentStep == adapter.getItemCount() - 1;
        boolean isFirstPage = currentStep == 0;
        
        btnPrevious.setVisibility(isFirstPage ? View.INVISIBLE : View.VISIBLE);
        btnNext.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
        btnGetStarted.setVisibility(isLastPage ? View.VISIBLE : View.GONE);
        
        // 更新步骤信息
        String stepInfo = getString(R.string.step_info, currentStep + 1, adapter.getItemCount());
        tvStepInfo.setText(stepInfo);
        
        // 更新进度条
        float progress = (float) (currentStep + 1) / adapter.getItemCount() * 100;
        progressBar.setProgress((int) progress);
    }

    private void navigateNext() {
        if (currentStep < adapter.getItemCount() - 1) {
            // 如果是权限请求页面之前，直接跳转
            if (currentStep == adapter.getItemCount() - 2) {
                requestPermissions();
            } else {
                viewPager.setCurrentItem(currentStep + 1);
            }
        }
    }

    private void navigatePrevious() {
        if (currentStep > 0) {
            viewPager.setCurrentItem(currentStep - 1);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要特殊文件权限
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else if (requiredPermissions.length > 0) {
            requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE);
        } else {
            // 不需要权限的情况
            allPermissionsGranted = true;
            viewPager.setCurrentItem(adapter.getItemCount() - 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                allPermissionsGranted = true;
                viewPager.setCurrentItem(adapter.getItemCount() - 1);
            } else {
                showPermissionRationale();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    allPermissionsGranted = true;
                    viewPager.setCurrentItem(adapter.getItemCount() - 1);
                } else {
                    showPermissionRationale();
                }
            }
        }
    }

    private void showPermissionRationale() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_rationale_detail)
            .setPositiveButton(R.string.retry, (dialog, which) -> requestPermissions())
            .setNegativeButton(R.string.skip_for_now, (dialog, which) -> {
                allPermissionsGranted = false;
                viewPager.setCurrentItem(adapter.getItemCount() - 1);
            })
            .setCancelable(false)
            .show();
    }

    private void onGetStartedClicked() {
        markWelcomeCompleted();
        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("permissions_granted", allPermissionsGranted);
        startActivity(intent);
        finish();
    }

    private boolean isWelcomeCompleted() {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("welcome_completed", false);
    }

    private void markWelcomeCompleted() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("welcome_completed", true)
                .apply();
    }
}