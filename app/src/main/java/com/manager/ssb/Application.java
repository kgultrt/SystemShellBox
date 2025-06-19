package com.manager.ssb;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import java.io.File;


public class Application extends android.app.Application {

    private static Application instance;  // 新增单例实例

    // 新增获取全局上下文方法
    public static Context getAppContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;  // 初始化单例实例
    }
}