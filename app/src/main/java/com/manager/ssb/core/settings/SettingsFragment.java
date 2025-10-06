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
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_APP_NAME));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_THEME));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_DARK_MODE));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_LANGUAGE));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_TIMEOUT));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_CACHE_SIZE));
        
        // 使用辅助方法设置开关监听器
        setupSwitchPreference(SettingsKeys.KEY_NOTIFICATIONS);
        setupSwitchPreference(SettingsKeys.KEY_AUTO_START);
        setupSwitchPreference(SettingsKeys.KEY_SHOW_TIPS);
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
            String value = Config.get(key, "");
            onPreferenceChange(preference, value);
        } else if (preference instanceof EditTextPreference) {
            String value = Config.get(key, "");
            onPreferenceChange(preference, value);
        } else if (preference instanceof SeekBarPreference) {
            int value = Config.get(key, 0);
            onPreferenceChange(preference, value);
        } else if (preference instanceof SwitchPreferenceCompat) {
            boolean value = Config.get(key, false);
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