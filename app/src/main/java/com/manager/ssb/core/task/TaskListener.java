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

// TaskListener.java
package com.manager.ssb.core.task;

public interface TaskListener {
    /**
     * 任务开始通知（兼容旧版本）
     */
    default void onTaskStarted(String taskName) {
        // 默认实现保持向后兼容
    }

    /**
     * 任务结束通知（兼容旧版本）
     */
    default void onTaskFinished(String taskName) {
        // 默认实现保持向后兼容
    }

    /**
     * 增强版任务开始通知
     * @param taskId 系统生成的唯一任务ID
     * @param taskName 开发者定义的任务名称
     */
    default void onTaskStarted(String taskId, String taskName) {
        // 默认调用旧版方法保持兼容
        onTaskStarted(taskName);
    }

    /**
     * 增强版任务结束通知
     * @param taskId 系统生成的唯一任务ID
     * @param taskName 开发者定义的任务名称
     * @param status 任务最终状态（COMPLETED/FAILED/CANCELLED）
     * @param exception 失败时抛出的异常（可为null）
     */
    default void onTaskFinished(String taskId, String taskName, 
                              TaskStatus status, Throwable exception) {
        // 默认调用旧版方法保持兼容
        onTaskFinished(taskName);
        
        // 可添加默认的错误处理逻辑
        if (status == TaskStatus.FAILED && exception != null) {
            System.err.printf("Task %s failed: %s%n", taskId, exception.getMessage());
        }
    }
}