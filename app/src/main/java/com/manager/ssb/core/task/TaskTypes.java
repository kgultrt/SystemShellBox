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

// TaskTypes.java
package com.manager.ssb.core.task;

import java.util.Set;

public final class TaskTypes {
    // 预定义需要监控的任务名称（保持与老代码兼容）
    public static final String LOAD_FILES  = "task_LoadFiles";
    public static final String UP_STOR_INF = "task_UpdateStorageInfo";
    public static final String STOR_DIALOG = "task_ShowStorageDetails";
    public static final String RENAME_FILE = "task_RenameFile";
    public static final String DELETE_FILE = "task_DeleteFile";
    public static final String MOVE_FILE = "task_MoveFile";
    public static final String COPY_FILE = "task_CopyFile";

    // 监控任务白名单
    public static final Set<String> MONITORED_TASKS = Set.of(
        LOAD_FILES,
        UP_STOR_INF,
        STOR_DIALOG,
        RENAME_FILE,
        DELETE_FILE,
        MOVE_FILE,
        COPY_FILE
    );

    private TaskTypes() {
        throw new AssertionError("No TaskTypes instances for you!");
    }
}