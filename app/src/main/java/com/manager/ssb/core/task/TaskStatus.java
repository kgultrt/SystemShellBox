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

// TaskStatus.java
package com.manager.ssb.core.task;

public enum TaskStatus {
    /**
     * 任务已创建但尚未开始执行
     */
    CREATED,
    
    /**
     * 任务正在执行中
     */
    RUNNING,
    
    /**
     * 任务正常完成
     */
    COMPLETED,
    
    /**
     * 任务执行失败
     */
    FAILED,
    
    /**
     * 任务被取消
     */
    CANCELLED
}