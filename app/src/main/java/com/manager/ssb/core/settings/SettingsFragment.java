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

    @Override
    public void onViewCreated(android.view.View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 设置列表视图的填充
        getListView().setPadding(0, 0, 0, 0);
        getListView().setClipToPadding(false);
    }

    private void setupPreferences() {
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_THEME));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_LANGUAGE));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_TIMEOUT));
        bindPreferenceSummaryToValue(findPreference(SettingsKeys.KEY_CACHE_SIZE));
        
        setupSwitchPreference(SettingsKeys.KEY_NOTIFICATIONS);
        setupSwitchPreference(SettingsKeys.KEY_VIBRATION);
        setupSwitchPreference(SettingsKeys.KEY_SOUND);
        setupSwitchPreference(SettingsKeys.KEY_AUTO_START);
        setupSwitchPreference(SettingsKeys.KEY_SHOW_TIPS);
    }

    private void setupSwitchPreference(String key) {
        Preference preference = findPreference(key);
        
        if (preference != null) {
            preference.setOnPreferenceChangeListener(this);
            
            boolean value = Config.get(key, getDefaultBooleanForKey(key));
            onPreferenceChange(preference, value);
        }
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        if (preference == null) return;
        
        preference.setOnPreferenceChangeListener(this);
        
        String key = preference.getKey();
        
        if (preference instanceof ListPreference) {
            String value = Config.get(key, getDefaultStringForKey(key));
            onPreferenceChange(preference, value);
        } else if (preference instanceof EditTextPreference) {
            String value = Config.get(key, getDefaultStringForKey(key));
            onPreferenceChange(preference, value);
        } else if (preference instanceof SeekBarPreference) {
            int value = Config.get(key, getDefaultIntForKey(key));
            onPreferenceChange(preference, value);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (newValue instanceof Boolean) {
            Config.set(key, (Boolean) newValue);
        } else if (newValue instanceof String) {
            Config.set(key, (String) newValue);
        } else if (newValue instanceof Integer) {
            Config.set(key, (Integer) newValue);
        }

        updatePreferenceSummary(preference, newValue);
        return true;
    }
    
    private String getDefaultStringForKey(String key) {
        switch (key) {
            case SettingsKeys.KEY_THEME:
                return SettingsKeys.DEFAULT_THEME;
            case SettingsKeys.KEY_LANGUAGE:
                return SettingsKeys.DEFAULT_LANGUAGE;
            case SettingsKeys.KEY_CACHE_SIZE:
                return SettingsKeys.DEFAULT_CACHE_SIZE;
            default:
                return "";
        }
    }
    
    private int getDefaultIntForKey(String key) {
        switch (key) {
            case SettingsKeys.KEY_TIMEOUT:
                return SettingsKeys.DEFAULT_TIMEOUT;
            default:
                return 0;
        }
    }
    
    private boolean getDefaultBooleanForKey(String key) {
        switch (key) {
            case SettingsKeys.KEY_NOTIFICATIONS:
                return SettingsKeys.DEFAULT_NOTIFICATIONS;
            case SettingsKeys.KEY_VIBRATION:
                return SettingsKeys.DEFAULT_VIBRATION;
            case SettingsKeys.KEY_SOUND:
                return SettingsKeys.DEFAULT_SOUND;
            case SettingsKeys.KEY_AUTO_START:
                return SettingsKeys.DEFAULT_AUTO_START;
            case SettingsKeys.KEY_SHOW_TIPS:
                return SettingsKeys.DEFAULT_SHOW_TIPS;
            default:
                return true;
        }
    }
    
    private void updatePreferenceSummary(Preference preference, Object value) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(value.toString());
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
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
}