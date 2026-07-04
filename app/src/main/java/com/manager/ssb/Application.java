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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.StringRes;

import com.manager.ssb.core.config.Config;
import com.manager.ssb.core.HandlerInitializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;

public class Application extends android.app.Application {

    private static final String TAG = "CrashHandler";
    public static final String EXTRA_CRASH_INFO = "crash_info";
    private static Application instance;

    public static Context getAppContext() {
        return instance;
    }

    public static String getStringQuick(@StringRes int stringRes) {
        return instance.getString(stringRes);
    }

    public static String getStringQuick(@StringRes int stringRes, Object... formatArgs) {
        return instance.getString(stringRes, formatArgs);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 应用已保存的语言偏好
        applySavedLanguage();
        // 初始化文件处理
        HandlerInitializer.init();

        // 设置全局异常捕获
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            String crashInfo = getCrashReport(ex);
            Log.e(TAG, "Application crash:\n" + crashInfo);

            Intent intent = new Intent();
            intent.setClassName("com.manager.ssb", "com.manager.ssb.CrashActivity");
            intent.putExtra(EXTRA_CRASH_INFO, crashInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.setPackage(getPackageName());

            try {
                startActivity(intent);
            } catch (Exception ignored) {
            }

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }

    /** 从 Config 读取语言设置并应用到全局 */
    private void applySavedLanguage() {
        String lang = Config.get("general.language", "system");
        if (!"system".equals(lang)) {
            setAppLocale(lang);
        }
        // 如果是 "system"，则不干预，跟随系统
    }

    // Application.java 中替换原来的 setAppLocale 和 clearAppLocale

    public static void setAppLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        updateAppResources(instance);
    }

    public static void clearAppLocale() {
        // 获取真正的系统默认 Locale（不受应用设置影响）
        Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        Locale.setDefault(systemLocale);
        updateAppResources(instance);
    }

    /** 强制更新 Application 的全局资源 */
    private static void updateAppResources(Context context) {
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        // 兼容 API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new android.os.LocaleList(Locale.getDefault()));
        } else {
            config.locale = Locale.getDefault();
        }
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    // ========= 崩溃报告相关 =========

    private String getCrashReport(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("====== Fatal app crash ======");
        pw.println("Time: " + new Date());
        pw.println();

        pw.println("====== Device information ======");
        pw.println("Application Version: " + getAppVersion());
        pw.println("Android Version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        pw.println("Equipment model: " + Build.MANUFACTURER + " " + Build.MODEL);
        pw.println("CPU architecture: " + Build.SUPPORTED_ABIS[0]);
        pw.println("Available Memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
        pw.println();

        printStackTraceRecursive(pw, ex, 0);

        return sw.toString();
    }

    private void printStackTraceRecursive(PrintWriter pw, Throwable ex, int depth) {
        if (ex == null) return;

        String indent = getIndent(depth);
        pw.print(indent);
        pw.println("====== [" + ex.getClass().getSimpleName() + "] ======");

        pw.print(indent);
        pw.println(ex.getClass().getName() + ": " + ex.getMessage());

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

        for (Throwable suppressed : ex.getSuppressed()) {
            pw.print(indent);
            pw.println("Suppressed: ");
            printStackTraceRecursive(pw, suppressed, depth + 1);
        }

        Throwable cause = ex.getCause();
        if (cause != null) {
            pw.print(indent);
            pw.println("Caused by: ");
            printStackTraceRecursive(pw, cause, depth + 1);
        }
    }

    private String getIndent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}