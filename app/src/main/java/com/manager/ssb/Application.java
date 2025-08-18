package com.manager.ssb;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Application extends android.app.Application {

    private static final String TAG = "CrashHandler";
    public static final String EXTRA_CRASH_INFO = "crash_info";
    private static Application instance;

    public static Context getAppContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            // 1. 将崩溃信息写入日志文件（即使后面所有步骤都失败，我们仍有备份）
            String crashInfo = getCrashReport(ex);
            Log.e(TAG, "Application crash:\n" + crashInfo);
            
            // 2. 启动崩溃显示Activity（使用最低限度的系统功能）
            Intent intent = new Intent();
            intent.setClassName("com.manager.ssb", "com.manager.ssb.CrashActivity");
            intent.putExtra(EXTRA_CRASH_INFO, crashInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TASK |
                           Intent.FLAG_ACTIVITY_NO_ANIMATION);
            
            // 避免任何可能的资源访问错误
            intent.setPackage(getPackageName());
            
            try {
                startActivity(intent);
            } catch (Exception e) {
                
            }
            
            // 3. 结束当前进程
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }

    // 获取崩溃报告（更加详细）
private String getCrashReport(Throwable ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    // 基本信息
    pw.println("====== Fatal app crash ======");
    pw.println("Time: " + new Date());
    pw.println();

    // 设备信息
    pw.println("====== Device information ======");
    pw.println("Application Version: " + getAppVersion());
    pw.println("Android Version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
    pw.println("Equipment model: " + Build.MANUFACTURER + " " + Build.MODEL);
    pw.println("CPU architecture: " + Build.SUPPORTED_ABIS[0]);
    pw.println("Available Memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
    pw.println();

    // 处理主异常及其相关异常
    printStackTraceRecursive(pw, ex, 0);

    return sw.toString();
}

// 递归打印所有Caused by和Suppressed异常（已修复）
private void printStackTraceRecursive(PrintWriter pw, Throwable ex, int depth) {
    if (ex == null) return;
    
    // 打印异常头（带深度缩进）
    String indent = getIndent(depth);
    pw.print(indent);
    pw.println("====== [" + ex.getClass().getSimpleName() + "] ======");
    
    // 打印异常信息
    pw.print(indent);
    pw.println(ex.getClass().getName() + ": " + ex.getMessage());
    
    // 打印堆栈跟踪（带缩进）
    StackTraceElement[] stackTrace = ex.getStackTrace();
    for (int i = 0; i < Math.min(stackTrace.length, 30); i++) {
        pw.print(indent);
        pw.println("    at " + stackTrace[i].toString());
    }
    if (stackTrace.length > 30) {
        pw.print(indent);
        pw.println("    ... (additional " + (stackTrace.length - 30) + " frames)");
    }
    pw.println();
    
    // 打印Suppressed异常（递归处理）
    for (Throwable suppressed : ex.getSuppressed()) {
        pw.print(indent);
        pw.println("Suppressed: ");
        printStackTraceRecursive(pw, suppressed, depth + 1);
    }
    
    // 递归处理Cause（正确实现）
    Throwable cause = ex.getCause();
    if (cause != null) {
        pw.print(indent);
        pw.println("Caused by: ");
        printStackTraceRecursive(pw, cause, depth + 1);
    }
}

// 生成缩进字符串
private String getIndent(int depth) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < depth; i++) {
        sb.append("  ");  // 每级缩进2个空格
    }
    return sb.toString();
}


    
    // 获取应用版本信息
    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknow";
        }
    }
}