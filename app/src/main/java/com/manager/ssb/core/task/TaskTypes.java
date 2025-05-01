// TaskTypes.java
package com.manager.ssb.core.task;

import java.util.Set;

public final class TaskTypes {
    // 预定义需要监控的任务名称（保持与老代码兼容）
    public static final String LOAD_FILES = "task_LoadFiles";
    public static final String UP_STOR_INF = "task_UpdateStorageInfo";
    public static final String STOR_DIALOG = "task_ShowStorageDetails";

    // 监控任务白名单
    public static final Set<String> MONITORED_TASKS = Set.of(
        LOAD_FILES,
        UP_STOR_INF,
        STOR_DIALOG
    );

    private TaskTypes() {
        throw new AssertionError("No TaskTypes instances for you!");
    }
}