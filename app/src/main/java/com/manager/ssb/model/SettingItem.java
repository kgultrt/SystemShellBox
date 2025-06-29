/*
 * System Shell Box
 * Copyright (C) 2025 kgultrt
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

package com.manager.ssb.model;

public class SettingItem {

    private String category; // 分类
    private String title; // 设置项标题
    private String description; // 设置项描述
    private int type; // 设置项类型 (例如：Switch, EditText, List)
    private boolean isChecked; // Switch 的状态
    // ... 其他属性 ...

    public static final int TYPE_SWITCH = 0;
    public static final int TYPE_TEXT = 1;
    //...

    public SettingItem(String category, String title, String description, int type) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getType() {
        return type;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    // ... Getters 和 Setters ...
}