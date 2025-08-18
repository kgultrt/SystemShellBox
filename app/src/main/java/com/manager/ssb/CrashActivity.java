package com.manager.ssb;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.manager.ssb.R;

public class CrashActivity extends Activity {

    private String crashReport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Log.e("CrashActivity", "Crash processor self crash", ex);
            finishAndExit();
        });
        
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            
            super.onCreate(savedInstanceState);
            
            // 获取崩溃信息
            crashReport = extractCrashInfo();
            
            // 创建界面
            createUserFriendlyLayout();
            
        } catch (Throwable ex) {
            Log.e("CrashActivity", "Failed to create crash interface", ex);
            finishAndExit();
        }
    }
    
    private String extractCrashInfo() {
        try {
            if (getIntent() != null && getIntent().hasExtra(Application.EXTRA_CRASH_INFO)) {
                return getIntent().getStringExtra(Application.EXTRA_CRASH_INFO);
            }
        } catch (Throwable t) {
            Log.w("CrashActivity", "Failed to get crash information", t);
        }
        
        return getString(R.string.cannot_get_crash_info);
    }
    
    private void createUserFriendlyLayout() {
        // 主容器（垂直布局）
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        container.setBackgroundColor(Color.WHITE);
        
        // 标题区域
        TextView header = new TextView(this);
        header.setText(getString(R.string.crash_title));
        header.setTextSize(20);
        header.setTextColor(Color.BLACK);
        header.setGravity(Gravity.CENTER);
        header.setPadding(16, 40, 16, 16);
        container.addView(header);
        
        // 提示信息
        TextView suggestion = new TextView(this);
        suggestion.setText(getString(R.string.crash_what_happened));
        suggestion.setTextSize(14);
        suggestion.setTextColor(0xFF888888);
        suggestion.setPadding(32, 0, 32, 16);
        container.addView(suggestion);
        
        // 错误信息容器（带滚动条）
        ScrollView scrollContainer = new ScrollView(this);
        scrollContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            0, 
            1
        ));
        
        // 错误内容
        TextView errorView = new TextView(this);
        errorView.setText(crashReport);
        errorView.setTextSize(12);
        errorView.setTextColor(Color.BLACK);
        errorView.setPadding(32, 24, 32, 24);
        errorView.setBackgroundColor(0xFFF5F5F5);
        
        scrollContainer.addView(errorView);
        container.addView(scrollContainer);
        
        // 按钮容器（横向布局）
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        buttonContainer.setPadding(16, 16, 16, 24);
        buttonContainer.setGravity(Gravity.CENTER);
        
        // 添加操作按钮
        buttonContainer.addView(createActionButton(getString(R.string.crash_try_reboot), this::restartApp));
        buttonContainer.addView(createActionButton(getString(R.string.crash_copy), this::copyCrashInfo));
        buttonContainer.addView(createActionButton(getString(R.string.crash_exit), v -> finishAndExit()));
        
        container.addView(buttonContainer);
        
        // 开发者联系信息
        TextView contact = new TextView(this);
        contact.setText(getString(R.string.crash_tip) + "apple_1145141919@outlook.com  or  kgultrt (github)");
        contact.setTextSize(12);
        contact.setTextColor(0xFF555555);
        contact.setGravity(Gravity.CENTER);
        contact.setPadding(16, 0, 16, 16);
        container.addView(contact);
        
        setContentView(container);
    }
    
    private Button createActionButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setLayoutParams(new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        ));
        button.setPadding(8, 12, 8, 12);
        button.setBackgroundResource(android.R.drawable.btn_default);
        button.setTextColor(Color.BLACK);
        button.setOnClickListener(listener);
        
        // 设置按钮之间的间距
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMargins(8, 0, 8, 0);
        
        return button;
    }
    
    private void restartApp(View view) {
        try {
            // 启动应用的Launcher Activity
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {
            showToast(getString(R.string.crash_reboot_failed));
        } finally {
            finishAndExit();
        }
    }
    
    private void copyCrashInfo(View view) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Report", crashReport);
            clipboard.setPrimaryClip(clip);
            showToast(getString(R.string.crash_has_copyed));
        } catch (Exception e) {
            showToast(getString(R.string.crash_copy_failed));
        }
    }
    
    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w("CrashActivity", "Toast cannot be displayed", e);
        }
    }
    
    private void finishAndExit() {
        try {
            finish();
        } finally {
            // 确保进程被终止
            Process.killProcess(Process.myPid());
            System.exit(0);
        }
    }

    @Override
    public void onBackPressed() {
        // 提供选择而非完全禁用返回键
        showToast(getString(R.string.crash_how));
    }
}