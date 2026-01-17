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

package com.manager.ssb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.manager.ssb.adapter.WelcomePagerAdapter;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.model.WelcomePage;
import com.manager.ssb.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private boolean hasEntrance = false;
    private Handler animationHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        
        Config.initialize();
        
        // 检查是否已经完成向导
        if (isWelcomeCompleted()) {
            startMainActivity();
            return;
        }
        
        initViews();
        setupPermissions();
        setupViewPager();
        updateNavigation();
        // 移除了 applyInitialAnimations() 调用，关闭初始动画
        
        hasEntrance = true;
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

    // 删除或注释掉整个 applyInitialAnimations 方法，因为它包含初始动画
    /*
    private void applyInitialAnimations() {
        // 初始进入动画
        View rootLayout = findViewById(R.id.rootLayout);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        rootLayout.startAnimation(fadeIn);
        
        // 延迟显示内容动画
        animationHandler.postDelayed(() -> {
            animateViewSequentially(findViewById(R.id.viewPager), 200);
            animateViewSequentially(findViewById(R.id.indicatorLayout), 300);
            animateViewSequentially(findViewById(R.id.tvStepInfo), 400);
            animateViewSequentially(findViewById(R.id.progressBar), 500);
            animateViewSequentially(findViewById(R.id.btnPrevious), 600);
            animateViewSequentially(findViewById(R.id.btnNext), 700);
        }, 300);
    }

    private void animateViewSequentially(View view, long delay) {
        if (view != null && view.getVisibility() == View.VISIBLE) {
            animationHandler.postDelayed(() -> {
                Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                slideUp.setInterpolator(new DecelerateInterpolator());
                view.startAnimation(slideUp);
            }, delay);
        }
    }
    */

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
            R.drawable.ic_emoji_people,
            getString(R.string.welcome_title_1),
            getString(R.string.welcome_desc_1),
            WelcomePage.TYPE_STANDARD
        ));
        
        // GPLv3 协议页面
        pages.add(new WelcomePage(
            R.drawable.ic_text,
            getString(R.string.license_title),
            "",
            WelcomePage.TYPE_LICENSE
        ));
        
        // 权限说明页面
        pages.add(new WelcomePage(
            R.drawable.ic_info,
            getString(R.string.permission_title),
            getString(R.string.permission_desc),
            WelcomePage.TYPE_PERMISSION
        ));
        
        // 完成页面
        pages.add(new WelcomePage(
            R.drawable.ic_task_success,
            getString(R.string.complete_title),
            getString(R.string.complete_desc),
            WelcomePage.TYPE_COMPLETE
        ));
        
        adapter = new WelcomePagerAdapter(pages, this::onLicenseViewCreated, this::onPermissionViewCreated);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // 禁用滑动
        
        setupIndicators(pages.size());
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int previousStep = currentStep;
                currentStep = position;
                
                // 页面切换动画
                if (position > previousStep) {
                    viewPager.setPageTransformer(new SlideForwardTransformer());
                } else {
                    viewPager.setPageTransformer(new SlideBackwardTransformer());
                }
                
                updateNavigation();
                updateIndicators(position);
                
                // 应用页面进入动画
                applyPageEnterAnimation();
            }
        });
    }

    private void applyPageEnterAnimation() {
        View currentPage = viewPager.getChildAt(0);
        if (currentPage != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            currentPage.startAnimation(fadeIn);
        }
    }

    private void onLicenseViewCreated(View licenseView) {
        if (licenseView == null) return;
        
        ScrollView scrollView = licenseView.findViewById(R.id.licenseScrollView);
        TextView tvLicenseText = licenseView.findViewById(R.id.tvLicenseText);
        RadioGroup radioGroupLanguage = licenseView.findViewById(R.id.radioGroupLanguage);
        RadioButton radioEnglish = licenseView.findViewById(R.id.radioEnglish);
        RadioButton radioChinese = licenseView.findViewById(R.id.radioChinese);
        
        // 默认加载英文版本
        loadLicenseText(tvLicenseText, "LICENSE/LICENSE.txt");
        
        radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            
            tvLicenseText.startAnimation(fadeOut);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (checkedId == R.id.radioEnglish) {
                        loadLicenseText(tvLicenseText, "LICENSE/LICENSE.txt");
                    } else {
                        loadLicenseText(tvLicenseText, "LICENSE/LICENSE_CN.txt");
                    }
                    tvLicenseText.startAnimation(fadeIn);
                }
                
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });
        
        // 滚动到顶部
        scrollView.post(() -> scrollView.scrollTo(0, 0));
    }

    private void onPermissionViewCreated(View permissionView) {
        if (permissionView == null) return;
        
        LinearLayout permissionsContainer = permissionView.findViewById(R.id.permissionsContainer);
        MaterialButton btnGrantPermissions = permissionView.findViewById(R.id.btnGrantPermissions);
        
        // 清空现有权限项
        permissionsContainer.removeAllViews();
        
        // 添加权限说明卡片
        addPermissionCard(permissionsContainer, 
            getString(R.string.storage_permission_title),
            getString(R.string.storage_permission_desc),
            R.drawable.ic_hard_drive);
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermissionCard(permissionsContainer,
                getString(R.string.notification_permission_title),
                getString(R.string.notification_permission_desc),
                R.drawable.ic_info);
        }
        
        btnGrantPermissions.setOnClickListener(v -> {
            // 移除按钮点击动画
            requestPermissions();
        });
        
        // 检查权限状态并更新UI
        updatePermissionStatus(permissionView);
    }

    private void addPermissionCard(LinearLayout container, String title, String description, int iconRes) {
        MaterialCardView cardView = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(dpToPx(2));
        cardView.setRadius(dpToPx(12));
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.cardBackground));
        
        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        
        // 图标
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconRes);
        iconView.setPadding(0, 0, dpToPx(16), 0);
        // 设置图标大小
        int iconSize = dpToPx(120); // 120dp
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconView.setLayoutParams(iconParams);
        
        // 文本内容
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        );
        textLayout.setLayoutParams(textParams);
        
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        
        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descView.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        descView.setLineSpacing(dpToPx(4), 1);
        
        textLayout.addView(titleView);
        textLayout.addView(descView);
        
        cardContent.addView(iconView);
        cardContent.addView(textLayout);
        cardView.addView(cardContent);
        
        container.addView(cardView);
        
        // 卡片进入动画（保留这个动画）
        Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideInRight.setStartOffset(container.getChildCount() * 100L);
        cardView.startAnimation(slideInRight);
    }

    private void updatePermissionStatus(View permissionView) {
        TextView tvPermissionStatus = permissionView.findViewById(R.id.tvPermissionStatus);
        MaterialButton btnGrantPermissions = permissionView.findViewById(R.id.btnGrantPermissions);
        
        if (areAllPermissionsGranted()) {
            tvPermissionStatus.setText(R.string.permission_granted);
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnGrantPermissions.setEnabled(false);
            btnGrantPermissions.setText(R.string.permission_granted);
            allPermissionsGranted = true;
        } else {
            tvPermissionStatus.setText(R.string.permission_required);
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning));
            btnGrantPermissions.setEnabled(true);
            btnGrantPermissions.setText(R.string.grant_permissions);
            allPermissionsGranted = false;
        }
    }

    private boolean areAllPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return false;
            }
        }
        
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        return true;
    }

    private void loadLicenseText(TextView textView, String assetPath) {
        try {
            InputStream inputStream = getAssets().open(assetPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            reader.close();
            
            String licenseText = stringBuilder.toString();
            SpannableString spannableString = new SpannableString(licenseText);
            
            // 为标题添加粗体
            if (licenseText.contains("GNU GENERAL PUBLIC LICENSE")) {
                int titleStart = licenseText.indexOf("GNU GENERAL PUBLIC LICENSE");
                int titleEnd = titleStart + "GNU GENERAL PUBLIC LICENSE".length();
                spannableString.setSpan(new StyleSpan(Typeface.BOLD), titleStart, titleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            textView.setText(spannableString);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            
        } catch (IOException e) {
            textView.setText(R.string.license_load_error);
        }
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
            
            // 保留指示器进入动画
            Animation bounceIn = AnimationUtils.loadAnimation(this, R.anim.bounce_in);
            bounceIn.setStartOffset(i * 150L);
            indicator.startAnimation(bounceIn);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicatorLayout.getChildCount(); i++) {
            View indicator = indicatorLayout.getChildAt(i);
            boolean isSelected = i == position;
            indicator.setSelected(isSelected);
            
            // 保留选中动画
            if (isSelected) {
                Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
                indicator.startAnimation(scaleUp);
            }
        }
    }

    private void updateNavigation() {
        boolean isLastPage = currentStep == adapter.getItemCount() - 1;
        boolean isFirstPage = currentStep == 0;
        
        // 移除所有按钮动画，直接设置可见性
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
            // 移除按钮点击动画，直接切换页面
            viewPager.setCurrentItem(currentStep + 1);
        }
    }

    private void navigatePrevious() {
        if (currentStep > 0) {
            // 移除按钮点击动画，直接切换页面
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
            updatePermissionStatus(viewPager.getChildAt(0));
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
                updatePermissionStatus(viewPager.getChildAt(0));
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
                    updatePermissionStatus(viewPager.getChildAt(0));
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
                updatePermissionStatus(viewPager.getChildAt(0));
            })
            .setCancelable(false)
            .show();
    }

    private void onGetStartedClicked() {
        Config.set("isWelcomeCompleted", true);
        
        // 保留退出动画
        View rootLayout = findViewById(R.id.rootLayout);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        rootLayout.startAnimation(fadeOut);
        
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                startMainActivity();
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("permissions_granted", allPermissionsGranted);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private boolean isWelcomeCompleted() {
        return Config.get("isWelcomeCompleted", false);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // 自定义页面切换动画（保留这些动画）
    private static class SlideForwardTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 0) {
                page.setAlpha(1f);
                page.setTranslationX(0f);
            } else if (position <= 1) {
                page.setAlpha(1f - position);
                page.setTranslationX(-page.getWidth() * position);
            } else {
                page.setAlpha(0f);
            }
        }
    }

    private static class SlideBackwardTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            if (position < -1) {
                page.setAlpha(0f);
            } else if (position <= 0) {
                page.setAlpha(1f + position);
                page.setTranslationX(-page.getWidth() * position);
            } else if (position <= 1) {
                page.setAlpha(1f);
                page.setTranslationX(0f);
            } else {
                page.setAlpha(0f);
            }
        }
    }
}