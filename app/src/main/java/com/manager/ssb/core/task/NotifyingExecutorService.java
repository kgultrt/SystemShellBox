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

// NotifyingExecutorService.java
package com.manager.ssb.core.task;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NotifyingExecutorService extends AbstractExecutorService {
    private static final AtomicLong TASK_ID_GEN = new AtomicLong(0);
    private static final String UNNAMED_PREFIX = "Unnamed-";

    private final ExecutorService delegate;
    private final TaskListener taskListener;
    private final Set<String> monitoredTasks;
    private final ConcurrentMap<String, TrackedTask<?>> activeTasks = new ConcurrentHashMap<>();
    private final List<TrackedTask<?>> completedTasks = new CopyOnWriteArrayList<>();

    // 新增构造函数（带白名单）
    public NotifyingExecutorService(ExecutorService delegate, 
                                  TaskListener taskListener,
                                  Set<String> monitoredTasks) {
        this.delegate = delegate;
        this.taskListener = taskListener;
        this.monitoredTasks = Collections.unmodifiableSet(new HashSet<>(monitoredTasks));
    }

    // 兼容老代码的构造函数
    public NotifyingExecutorService(ExecutorService delegate, TaskListener taskListener) {
        this(delegate, taskListener, Collections.emptySet());
    }

    // 完整submit方法实现
    public <T> Future<T> submit(Callable<T> task, String taskName) {
        if (!shouldTrack(taskName)) {
            return delegate.submit(task);
        }

        final String taskId = generateTaskId(taskName);
        TrackedCallable<T> trackedTask = new TrackedCallable<>(taskId, taskName, task);
        
        activeTasks.put(taskId, trackedTask);
        taskListener.onTaskStarted(taskId, taskName);

        Future<T> future = delegate.submit(trackedTask);
        trackedTask.setFuture(future);
        return future;
    }

    public Future<?> submit(Runnable task, String taskName) {
        return submit(Executors.callable(task, null), taskName);
    }

    public <T> Future<T> submit(Runnable task, String taskName, T result) {
        return submit(Executors.callable(task, result), taskName);
    }

    // 完整的execute方法实现
    public void execute(Runnable command, String taskName) {
        if (!shouldTrack(taskName)) {
            delegate.execute(command);
            return;
        }

        submit(command, taskName);
    }

    @Override
    public void execute(Runnable command) {
        String generatedName = UNNAMED_PREFIX + command.getClass().getSimpleName();
        execute(command, generatedName);
    }

    // 完整的状态查询方法
    public List<TaskInfo> getActiveTasks() {
        return activeTasks.values().stream()
                .map(TrackedTask::getTaskInfo)
                .collect(Collectors.toList());
    }

    public List<TaskInfo> getCompletedTasks() {
        return completedTasks.stream()
                .map(TrackedTask::getTaskInfo)
                .collect(Collectors.toList());
    }

    public Optional<TaskInfo> getTaskInfo(String taskId) {
        TrackedTask<?> tracked = activeTasks.getOrDefault(taskId, 
            completedTasks.stream()
                .filter(t -> t.getTaskInfo().taskId().equals(taskId))
                .findFirst()
                .orElse(null));
        return Optional.ofNullable(tracked).map(TrackedTask::getTaskInfo);
    }

    // 完整的跟踪判断逻辑
    private boolean shouldTrack(String taskName) {
        // 空名称或自动生成的未命名任务直接跳过
        if (taskName == null || taskName.startsWith(UNNAMED_PREFIX)) {
            return false;
        }
        // 白名单为空时跟踪所有命名任务（兼容模式）
        // 白名单非空时只跟踪白名单内的任务
        return monitoredTasks.isEmpty() ? true : monitoredTasks.contains(taskName);
    }

    // 完整内部类实现
    private abstract class TrackedTask<V> {
        protected volatile TaskInfo taskInfo;
        protected Future<V> future;

        TrackedTask(String taskId, String taskName) {
            this.taskInfo = new TaskInfo(
                taskId,
                taskName,
                TaskStatus.CREATED,
                System.currentTimeMillis(),
                -1L,
                -1L,
                null
            );
        }

        void setFuture(Future<V> future) {
            this.future = future;
        }

        TaskInfo getTaskInfo() {
            TaskInfo current = this.taskInfo;
            
            long endTime = taskInfo.endTime() > 0 ? taskInfo.endTime() : 
                (taskInfo.status() == TaskStatus.RUNNING ? -1L : System.currentTimeMillis());
            
            return new TaskInfo(
                current.taskId(),
                current.taskName(),
                current.status(),
                current.submitTime(),
                current.startTime(),
                endTime,
                getException()
            );
        }

        protected abstract Throwable getException();
    }

    private class TrackedCallable<V> extends TrackedTask<V> implements Callable<V> {
        private final Callable<V> delegate;
        private volatile Throwable exception;

        TrackedCallable(String taskId, String taskName, Callable<V> delegate) {
            super(taskId, taskName);
            this.delegate = delegate;
        }

        @Override
        public V call() throws Exception {
            TaskInfo current = this.taskInfo.withStatus(TaskStatus.RUNNING).withStartTime();
            synchronized (this) {
                this.taskInfo = current;
            }

            try {
                V result = delegate.call();
                completeTask(TaskStatus.COMPLETED, null);
                return result;
            } catch (Exception e) {
                completeTask(TaskStatus.FAILED, e);
                throw e;
            } catch (Throwable t) {
                completeTask(TaskStatus.FAILED, t);
                throw new ExecutionException(t);
            }
        }

        private void completeTask(TaskStatus status, Throwable ex) {
            TaskInfo updated;
            synchronized (this) {
                updated = this.taskInfo
                    .withStatus(status)
                    .withEndTime()
                    .withException(ex);
            
                this.taskInfo = updated;
                this.exception = ex;
            }

            activeTasks.remove(taskInfo.taskId());
            completedTasks.add(this);
            taskListener.onTaskFinished(updated.taskId(), updated.taskName(), status, ex);
        }

        @Override
        protected Throwable getException() {
            return exception;
        }
    }

    // 完整的TaskInfo记录类
    public record TaskInfo(
        String taskId,
        String taskName,
        TaskStatus status,
        long submitTime,
        long startTime,
        long endTime,
        Throwable exception
    ) {
        TaskInfo withStatus(TaskStatus status) {
            return new TaskInfo(taskId, taskName, status, submitTime, startTime, endTime, exception);
        }

        TaskInfo withStartTime() {
            return new TaskInfo(taskId, taskName, status, submitTime, System.currentTimeMillis(), endTime, exception);
        }

        TaskInfo withEndTime() {
            return new TaskInfo(taskId, taskName, status, submitTime, startTime, System.currentTimeMillis(), exception);
        }

        TaskInfo withException(Throwable ex) {
            return new TaskInfo(taskId, taskName, status, submitTime, startTime, endTime, ex);
        }
    }

    // 完整委托方法实现
    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    private static String generateTaskId(String taskName) {
        return taskName + "-" + TASK_ID_GEN.incrementAndGet();
    }
}