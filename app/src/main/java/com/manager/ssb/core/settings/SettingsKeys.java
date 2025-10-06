// SettingsKeys.java
package com.manager.ssb.core.settings;

public class SettingsKeys {
    // 应用设置
    public static final String KEY_APP_NAME = "appName";
    public static final String KEY_FIRST_RUN = "isFirst";
    public static final String KEY_THEME = "appearance.theme";
    public static final String KEY_DARK_MODE = "appearance.darkMode";
    public static final String KEY_LANGUAGE = "general.language";
    public static final String KEY_NOTIFICATIONS = "notifications.enabled";
    public static final String KEY_VIBRATION = "notifications.vibration";
    public static final String KEY_SOUND = "notifications.sound";
    public static final String KEY_AUTO_START = "behavior.autoStart";
    public static final String KEY_SHOW_TIPS = "behavior.showTips";
    public static final String KEY_TIMEOUT = "behavior.timeout";
    public static final String KEY_CACHE_SIZE = "storage.cacheSize";
    
    // 默认值
    public static final String DEFAULT_APP_NAME = "System Shell Box";
    public static final boolean DEFAULT_FIRST_RUN = true;
    public static final String DEFAULT_THEME = "system";
    public static final int DEFAULT_DARK_MODE = 0;
    public static final String DEFAULT_LANGUAGE = "system";
    public static final boolean DEFAULT_NOTIFICATIONS = true;
    public static final boolean DEFAULT_VIBRATION = true;
    public static final boolean DEFAULT_SOUND = true;
    public static final boolean DEFAULT_AUTO_START = false;
    public static final boolean DEFAULT_SHOW_TIPS = true;
    public static final int DEFAULT_TIMEOUT = 30;
    public static final String DEFAULT_CACHE_SIZE = "100";
}
