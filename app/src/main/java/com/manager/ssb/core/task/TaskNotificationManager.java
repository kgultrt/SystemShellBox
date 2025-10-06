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

// TaskNotificationManager.java
package com.manager.ssb.core.task;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.manager.ssb.R;
import com.manager.ssb.MainActivity;
import com.manager.ssb.Application;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

public class TaskNotificationManager implements TaskListener {
    private static final String CHANNEL_ID = "task_channel";
    private static final int PERSISTENT_NOTIFICATION_ID = 1000;
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final ConcurrentHashMap<String, TaskInfo> activeTasks = new ConcurrentHashMap<>();
    private final Queue<TaskResult> recentResults = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_RESULTS = 5;

    private static class TaskInfo {
        String taskName;
        int progress;
        
        TaskInfo(String taskName, int progress) {
            this.taskName = taskName;
            this.progress = progress;
        }
    }
    
    private static class TaskResult {
        String taskName;
        TaskStatus status;
        long timestamp;
        
        TaskResult(String taskName, TaskStatus status) {
            this.taskName = taskName;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public TaskNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                Application.getAppContext().getString(R.string.notification_channel_title),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(Application.getAppContext().getString(R.string.notification_channel_description));
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTaskStarted(String taskId, String taskName) {
        activeTasks.put(taskId, new TaskInfo(taskName, 0));
        updatePersistentNotification();
    }

    @Override
    public void onTaskFinished(String taskId, String taskName, TaskStatus status, Throwable exception) {
        TaskInfo removedTask = activeTasks.remove(taskId);
        
        // 添加到最近结果列表
        if (recentResults.size() >= MAX_RECENT_RESULTS) {
            recentResults.poll(); // 移除最旧的结果
        }
        recentResults.offer(new TaskResult(taskName, status));
        
        updatePersistentNotification();
    }

    private void updatePersistentNotification() {
        Notification notification = buildPersistentNotification();
        notificationManager.notify(PERSISTENT_NOTIFICATION_ID, notification);
    }

    private Notification buildPersistentNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(getNotificationTitle())
            .setContentText(getNotificationText())
            .setSmallIcon(getNotificationIcon())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(null)
            .setPriority(NotificationCompat.PRIORITY_LOW);

        // 如果有活动任务，添加进度条
        if (!activeTasks.isEmpty()) {
            builder.setProgress(100, getOverallProgress(), false);
        }

        // 添加操作按钮
        if (!activeTasks.isEmpty()) {
            // 可以添加取消所有任务等操作
            builder.addAction(R.drawable.ic_close, 
                Application.getAppContext().getString(R.string.notification_channel_hide), 
                getDismissPendingIntent());
        } else {
            // 没有活动任务时允许用户清除通知
            builder.setOngoing(false)
                   .setAutoCancel(true);
        }

        // 如果有多个任务或历史记录，使用展开样式
        if (activeTasks.size() > 1 || !recentResults.isEmpty()) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            
            // 添加活动任务
            if (!activeTasks.isEmpty()) {
                inboxStyle.setBigContentTitle(Application.getAppContext().getString(R.string.notification_channel_act) + activeTasks.size());
                for (TaskInfo task : activeTasks.values()) {
                    inboxStyle.addLine("• " + task.taskName + " [" + task.progress + "%]");
                }
            }
            
            // 添加最近完成的任务
            if (!recentResults.isEmpty()) {
                if (!activeTasks.isEmpty()) {
                    inboxStyle.addLine("");
                }
                inboxStyle.addLine(Application.getAppContext().getString(R.string.notification_channel_rec));
                for (TaskResult result : recentResults) {
                    String statusIcon = getStatus(result.status);
                    inboxStyle.addLine(statusIcon + " " + result.taskName);
                }
            }
            
            builder.setStyle(inboxStyle);
        }

        return builder.build();
    }

    private String getNotificationTitle() {
        if (activeTasks.isEmpty()) {
            return Application.getAppContext().getString(R.string.notification_channel_ready);
        } else if (activeTasks.size() == 1) {
            TaskInfo task = activeTasks.values().iterator().next();
            return task.taskName;
        } else {
            return Application.getAppContext().getString(R.string.notification_channel_multi) + activeTasks.size();
        }
    }

    private String getNotificationText() {
        if (activeTasks.isEmpty()) {
            if (recentResults.isEmpty()) {
                return Application.getAppContext().getString(R.string.notification_channel_idle);
            } else {
                return Application.getAppContext().getString(R.string.notification_channel_recent);
            }
        } else if (activeTasks.size() == 1) {
            TaskInfo task = activeTasks.values().iterator().next();
            return Application.getAppContext().getString(R.string.notification_channel_now) + task.progress + "%";
        } else {
            return Application.getAppContext().getString(R.string.notification_channel_tasks) + activeTasks.size();
        }
    }

    private int getNotificationIcon() {
        if (activeTasks.isEmpty()) {
            return R.drawable.ic_task;
        } else {
            return R.drawable.ic_task;
        }
    }

    private int getOverallProgress() {
        if (activeTasks.isEmpty()) {
            return 0;
        }
        int totalProgress = 0;
        for (TaskInfo task : activeTasks.values()) {
            totalProgress += task.progress;
        }
        return totalProgress / activeTasks.size();
    }

    private String getStatus(TaskStatus status) {
        switch (status) {
            case COMPLETED:
                return "(" + Application.getAppContext().getString(R.string.notification_channel_comp) + ")";
            case FAILED:
                return "(" + Application.getAppContext().getString(R.string.notification_channel_fail) + ")";
            case CANCELLED:
                return "(" + Application.getAppContext().getString(R.string.notification_channel_canc) + ")";
            default:
                return "(" + Application.getAppContext().getString(R.string.notification_channel_unkn) + ")";
        }
    }

    // 进度更新方法
    public void updateTaskProgress(String taskId, int progress) {
        TaskInfo taskInfo = activeTasks.get(taskId);
        if (taskInfo != null) {
            taskInfo.progress = progress;
            updatePersistentNotification();
        }
    }

    private PendingIntent getDefaultPendingIntent() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent getDismissPendingIntent() {
        Intent dismissIntent = new Intent(context, NotificationDismissReceiver.class);
        dismissIntent.setAction("DISMISS_NOTIFICATION");
        return PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    // 清除所有通知状态
    public void clearAll() {
        activeTasks.clear();
        recentResults.clear();
        notificationManager.cancel(PERSISTENT_NOTIFICATION_ID);
    }
}