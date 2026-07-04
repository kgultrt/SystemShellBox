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

package com.manager.ssb.core;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerRegistry {
    private static final String TAG = "HandlerRegistry";

    // FileType -> 处理器列表（一个类型可对应多个处理器，取第一个为默认）
    private static final Map<FileType, List<FileHandler>> typeHandlers = new HashMap<>();

    // 所有已注册的处理器（去重，用于“打开方式”对话框）
    private static final List<FileHandler> allHandlers = new ArrayList<>();

    /**
     * 注册处理器到指定文件类型。
     * 如果同一个处理器实例已注册过，不会重复添加。
     */
    public static void register(FileType type, FileHandler handler) {
        if (type == null || handler == null) {
            Log.w(TAG, "register: type or handler is null");
            return;
        }

        List<FileHandler> list = typeHandlers.computeIfAbsent(type, k -> new ArrayList<>());
        if (!list.contains(handler)) {
            list.add(handler);
        }

        if (!allHandlers.contains(handler)) {
            allHandlers.add(handler);
        }

        Log.d(TAG, "Registered handler for type " + type + ": " + handler.getClass().getSimpleName());
    }

    /**
     * 获取指定类型的默认处理器（列表中的第一个）。
     * 如果没有注册处理器，返回 null。
     */
    public static FileHandler getHandlerForType(FileType type) {
        List<FileHandler> list = typeHandlers.get(type);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 获取所有已注册的处理器（去重），用于动态构建“打开方式”菜单。
     * 返回的列表是副本，避免外部修改影响内部状态。
     */
    public static List<FileHandler> getAllHandlers() {
        return new ArrayList<>(allHandlers);
    }

    /**
     * 清空所有注册信息（主要用于测试或重置）。
     */
    public static void clear() {
        typeHandlers.clear();
        allHandlers.clear();
    }
}