// TaskNotificationManager.java (通知管理类)
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskNotificationManager implements TaskListener {
    private static final String CHANNEL_ID = "task_channel";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final ConcurrentHashMap<String, Integer> taskNotificationIds = new ConcurrentHashMap<>();
    private final AtomicInteger notificationCounter = new AtomicInteger(NOTIFICATION_ID_BASE);

    public TaskNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "后台任务通知",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("显示后台任务执行状态");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTaskStarted(String taskId, String taskName) {
        int notificationId = notificationCounter.incrementAndGet();
        taskNotificationIds.put(taskId, notificationId);

        Notification notification = buildProgressNotification(taskName, "任务开始执行", 0)
            .setOngoing(true)
            .build();

        notificationManager.notify(notificationId, notification);
    }

    @Override
    public void onTaskFinished(String taskId, String taskName, TaskStatus status, Throwable exception) {
        Integer notificationId = taskNotificationIds.remove(taskId);
        if (notificationId == null) return;

        String statusText = getStatusText(status, exception);
        Notification notification = buildStatusNotification(taskName, statusText, status)
            .setAutoCancel(true)
            .build();

        notificationManager.notify(notificationId, notification);
    }

    private NotificationCompat.Builder buildProgressNotification(String title, String text, int progress) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_task)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .setContentIntent(getDefaultPendingIntent());
    }

    private NotificationCompat.Builder buildStatusNotification(String title, String text, TaskStatus status) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(getStatusIcon(status))
            .setContentIntent(getDefaultPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private int getStatusIcon(TaskStatus status) {
        switch (status) {
            case COMPLETED:
                return R.drawable.ic_task_success;
            case FAILED:
                return R.drawable.ic_task_failed;
            case CANCELLED:
                return R.drawable.ic_task_cancelled;
            default:
                return R.drawable.ic_task;
        }
    }

    private String getStatusText(TaskStatus status, Throwable exception) {
        switch (status) {
            case COMPLETED:
                return "任务已完成";
            case FAILED:
                return "任务失败: " + (exception != null ? exception.getMessage() : "未知错误");
            case CANCELLED:
                return "任务已取消";
            default:
                return "任务状态未知";
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

    // 进度更新方法（可选）
    public void updateTaskProgress(String taskId, int progress) {
        Integer notificationId = taskNotificationIds.get(taskId);
        if (notificationId != null) {
            Notification notification = buildProgressNotification(
                "任务进行中", 
                "当前进度: " + progress + "%",
                progress
            ).build();
            
            notificationManager.notify(notificationId, notification);
        }
    }
}