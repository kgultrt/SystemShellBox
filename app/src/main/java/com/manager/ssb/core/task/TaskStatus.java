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