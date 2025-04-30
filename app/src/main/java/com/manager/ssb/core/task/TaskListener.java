// TaskListener.java (通知接口)
package com.manager.ssb.core.task;

public interface TaskListener {
    void onTaskStarted(String taskName);
    void onTaskFinished(String taskName);
}