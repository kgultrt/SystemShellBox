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

// SettingsFragment.java
package com.manager.ssb.core.settings;

import android.os.Bundle;
import androidx.preference.*;
import com.manager.ssb.R;
import com.manager.ssb.Application;
import com.manager.ssb.core.config.Config;
import com.manager.ssb.core.settings.SettingsKeys;

public class SettingsFragment extends PreferenceFragmentCompat 
    implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        setupPreferences();
    }

    private void setupPreferences() {
        // 绑定所有设置项的值和监听器
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_THEME));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_LANGUAGE));
        
        // 使用辅助方法设置开关监听器
        setupSwitchPreference(SettingsKeys.KEY_NOTIFICATIONS);
    }

    /**
     * 设置开关类型偏好设置的辅助方法
     */
    private void setupSwitchPreference(String key) {
        Preference preference = findPreference(key);
        
        if (preference instanceof SwitchPreferenceCompat) {
            SwitchPreferenceCompat switchPreference = (SwitchPreferenceCompat) preference;
            switchPreference.setOnPreferenceChangeListener(this);
            
            // 初始化摘要
            boolean value = Config.get(key, false);
            onPreferenceChange(switchPreference, value);
        }
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        if (preference == null) return;
        
        preference.setOnPreferenceChangeListener(this);
        
        // 立即用当前值更新摘要
        String key = preference.getKey();
        
        if (preference instanceof ListPreference) {
            String value = Config.get(key, getDefaultValueForKey(key));
            onPreferenceChange(preference, value);
        } else if (preference instanceof EditTextPreference) {
            String value = Config.get(key, getDefaultValueForKey(key));
            onPreferenceChange(preference, value);
        } else if (preference instanceof SeekBarPreference) {
            int value = Config.get(key, 0);
            onPreferenceChange(preference, value);
        } else if (preference instanceof SwitchPreferenceCompat) {
            boolean value = Config.get(key, true);
            onPreferenceChange(preference, value);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String stringValue = newValue.toString();
        String key = preference.getKey();

        // 保存到配置
        if (newValue instanceof Boolean) {
            Config.set(key, (Boolean) newValue);
        } else if (newValue instanceof String) {
            Config.set(key, stringValue);
        } else if (newValue instanceof Integer) {
            Config.set(key, (Integer) newValue);
        }

        // 更新摘要
        updatePreferenceSummary(preference, newValue);
        
        return true;
    }
    
    private String getDefaultValueForKey(String key) {
        switch (key) {
            case SettingsKeys.KEY_APP_NAME:
                return SettingsKeys.DEFAULT_APP_NAME;
            case SettingsKeys.KEY_THEME:
                return SettingsKeys.DEFAULT_THEME;
            case SettingsKeys.KEY_LANGUAGE:
                return SettingsKeys.DEFAULT_LANGUAGE;
            default:
                return "";
        }
    }
    /**
     * 更新偏好设置摘要的辅助方法
     */
    private void updatePreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();
        
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(stringValue);
        } else if (preference instanceof SeekBarPreference) {
            preference.setSummary(stringValue);
        } else if (preference instanceof SwitchPreferenceCompat) {
            boolean enabled = (Boolean) value;
            preference.setSummary(enabled ? Application.getAppContext().getString(R.string.settings_enable) : Application.getAppContext().getString(R.string.settings_disable));
        }
    }
}