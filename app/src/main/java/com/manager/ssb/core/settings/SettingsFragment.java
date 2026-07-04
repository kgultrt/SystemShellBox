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

package com.manager.ssb.core.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.*;

import com.manager.ssb.Application;
import com.manager.ssb.R;
import com.manager.ssb.core.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    private View loadingView;
    private View contentView;
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 后台等待 Config 就绪（只阻塞一次，避免多线程等待）
        bgExecutor.execute(() -> {
            Config.get("general.language", "system"); // 触发加载

            mainHandler.post(() -> {
                setupPreferences();
            });
        });
    }

    private void setupPreferences() {
        // 1. 静默设置所有摘要（不触发 onPreferenceChange，避免重启）
        initSummary(findPreference(SettingsKeys.KEY_THEME));
        initSummary(findPreference(SettingsKeys.KEY_LANGUAGE));
        initSummary(findPreference(SettingsKeys.KEY_TIMEOUT));
        initSummary(findPreference(SettingsKeys.KEY_CACHE_SIZE));

        // 2. 设置开关的初始摘要并注册监听
        setupSwitchPreference(SettingsKeys.KEY_NOTIFICATIONS);
        setupSwitchPreference(SettingsKeys.KEY_VIBRATION);
        setupSwitchPreference(SettingsKeys.KEY_SOUND);
        setupSwitchPreference(SettingsKeys.KEY_AUTO_START);
        setupSwitchPreference(SettingsKeys.KEY_SHOW_TIPS);

        // 3. 为 List/Edit/SeekBar 注册监听（不包括开关，它们已在上面注册）
        setListener(SettingsKeys.KEY_THEME);
        setListener(SettingsKeys.KEY_LANGUAGE);
        setListener(SettingsKeys.KEY_TIMEOUT);
        setListener(SettingsKeys.KEY_CACHE_SIZE);
    }

    /** 初始化摘要，不触发回调 */
    private void initSummary(Preference pref) {
        if (pref == null) return;
        String key = pref.getKey();
        if (pref instanceof ListPreference) {
            String value = Config.get(key, getDefaultStringForKey(key));
            updatePreferenceSummary(pref, value);
        } else if (pref instanceof EditTextPreference) {
            String value = Config.get(key, getDefaultStringForKey(key));
            updatePreferenceSummary(pref, value);
        } else if (pref instanceof SeekBarPreference) {
            int value = Config.get(key, getDefaultIntForKey(key));
            updatePreferenceSummary(pref, value);
        }
    }

    /** 为指定 key 的偏好注册 OnPreferenceChangeListener */
    private void setListener(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(this);
        }
    }

    private void setupSwitchPreference(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(this);
            boolean value = Config.get(key, getDefaultBooleanForKey(key));
            updatePreferenceSummary(pref, value); // 只更新摘要，不触发保存
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        // 1. 持久化
        if (newValue instanceof Boolean) {
            Config.set(key, (Boolean) newValue);
        } else if (newValue instanceof String) {
            Config.set(key, (String) newValue);
        } else if (newValue instanceof Integer) {
            Config.set(key, (Integer) newValue);
        }

        // 2. 特殊处理：语言实时生效
        if (SettingsKeys.KEY_LANGUAGE.equals(key)) {
            String lang = (String) newValue;
            if ("system".equals(lang)) {
                Application.clearAppLocale();
            } else {
                Application.setAppLocale(lang);
            }

            if (getActivity() != null) {
                Intent intent = getActivity().getIntent();
                getActivity().finish();
                startActivity(intent);
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
            // 重启后不需要再更新摘要
            return true;
        }

        // 3. 更新摘要
        updatePreferenceSummary(preference, newValue);
        return true;
    }

    // ---------- 默认值 ----------
    private String getDefaultStringForKey(String key) {
        switch (key) {
            case SettingsKeys.KEY_THEME: return SettingsKeys.DEFAULT_THEME;
            case SettingsKeys.KEY_LANGUAGE: return SettingsKeys.DEFAULT_LANGUAGE;
            case SettingsKeys.KEY_CACHE_SIZE: return SettingsKeys.DEFAULT_CACHE_SIZE;
            default: return "";
        }
    }

    private int getDefaultIntForKey(String key) {
        if (SettingsKeys.KEY_TIMEOUT.equals(key)) return SettingsKeys.DEFAULT_TIMEOUT;
        return 0;
    }

    private boolean getDefaultBooleanForKey(String key) {
        switch (key) {
            case SettingsKeys.KEY_NOTIFICATIONS: return SettingsKeys.DEFAULT_NOTIFICATIONS;
            case SettingsKeys.KEY_VIBRATION: return SettingsKeys.DEFAULT_VIBRATION;
            case SettingsKeys.KEY_SOUND: return SettingsKeys.DEFAULT_SOUND;
            case SettingsKeys.KEY_AUTO_START: return SettingsKeys.DEFAULT_AUTO_START;
            case SettingsKeys.KEY_SHOW_TIPS: return SettingsKeys.DEFAULT_SHOW_TIPS;
            default: return true;
        }
    }

    private void updatePreferenceSummary(Preference preference, Object value) {
        if (preference instanceof ListPreference) {
            ListPreference lp = (ListPreference) preference;
            int index = lp.findIndexOfValue(value.toString());
            preference.setSummary(index >= 0 ? lp.getEntries()[index] : null);
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(value.toString());
        } else if (preference instanceof SeekBarPreference) {
            preference.setSummary(String.valueOf(value));
        } else if (preference instanceof SwitchPreferenceCompat) {
            boolean enabled = (Boolean) value;
            preference.setSummary(enabled ?
                    Application.getAppContext().getString(R.string.settings_enable) :
                    Application.getAppContext().getString(R.string.settings_disable));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
    }
}