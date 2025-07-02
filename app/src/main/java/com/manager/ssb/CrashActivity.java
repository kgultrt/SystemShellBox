// 修改后的 CrashActivity.java
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

public class CrashActivity extends Activity {

    private String crashReport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Log.e("CrashActivity", "崩溃处理器自身崩溃", ex);
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
            Log.e("CrashActivity", "创建崩溃界面失败", ex);
            finishAndExit();
        }
    }
    
    private String extractCrashInfo() {
        try {
            if (getIntent() != null && getIntent().hasExtra(Application.EXTRA_CRASH_INFO)) {
                return getIntent().getStringExtra(Application.EXTRA_CRASH_INFO);
            }
        } catch (Throwable t) {
            Log.w("CrashActivity", "获取崩溃信息失败", t);
        }
        return "无法获取崩溃信息\n请通过日志工具查看详细错误";
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
        header.setText("还有这种操作？");
        header.setTextSize(20);
        header.setTextColor(Color.BLACK);
        header.setGravity(Gravity.CENTER);
        header.setPadding(16, 40, 16, 16);
        container.addView(header);
        
        // 提示信息
        TextView suggestion = new TextView(this);
        suggestion.setText("发生了意外错误，请将此错误信息发送给开发者以便在未来的版本修复这个问题：");
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
        buttonContainer.addView(createActionButton("重启应用", this::restartApp));
        buttonContainer.addView(createActionButton("复制错误", this::copyCrashInfo));
        buttonContainer.addView(createActionButton("退出应用", v -> finishAndExit()));
        
        container.addView(buttonContainer);
        
        // 开发者联系信息
        TextView contact = new TextView(this);
        contact.setText("遇到问题请联系开发者：apple_1145141919@outlook.com");
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
            showToast("无法重启应用");
        } finally {
            finishAndExit();
        }
    }
    
    private void copyCrashInfo(View view) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("崩溃报告", crashReport);
            clipboard.setPrimaryClip(clip);
            showToast("错误信息已复制到剪贴板");
        } catch (Exception e) {
            showToast("复制失败，请手动选择文本复制");
        }
    }
    
    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w("CrashActivity", "无法显示Toast", e);
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
        showToast("请选择一个操作：重启、复制或退出");
    }
}