// NotifyingExecutorService.java
package com.manager.ssb.core.task;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NotifyingExecutorService extends AbstractExecutorService {
    private static final AtomicLong TASK_ID_GEN = new AtomicLong(0);

    private final ExecutorService delegate;
    private final TaskListener taskListener;
    private final ConcurrentMap<String, TrackedTask<?>> activeTasks = new ConcurrentHashMap<>();
    private final List<TrackedTask<?>> completedTasks = new CopyOnWriteArrayList<>();

    public NotifyingExecutorService(ExecutorService delegate, TaskListener taskListener) {
        this.delegate = delegate;
        this.taskListener = taskListener;
    }

    // 核心改进：带任务追踪的提交方法
    public <T> Future<T> submit(Callable<T> task, String taskName) {
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

    // 新增状态查询方法
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

    // 任务包装基类
    private abstract class TrackedTask<V> {
        protected final TaskInfo taskInfo;
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
            long endTime = taskInfo.endTime() > 0 ? taskInfo.endTime() : 
                (taskInfo.status() == TaskStatus.RUNNING ? -1L : System.currentTimeMillis());
            
            return new TaskInfo(
                taskInfo.taskId(),
                taskInfo.taskName(),
                taskInfo.status(),
                taskInfo.submitTime(),
                taskInfo.startTime(),
                endTime,
                getException()
            );
        }

        protected abstract Throwable getException();
    }

    // 可调用任务包装
    private class TrackedCallable<V> extends TrackedTask<V> implements Callable<V> {
        private final Callable<V> delegate;
        private volatile Throwable exception;

        TrackedCallable(String taskId, String taskName, Callable<V> delegate) {
            super(taskId, taskName);
            this.delegate = delegate;
        }

        @Override
        public V call() throws Exception {
            TaskInfo current = getTaskInfo().withStatus(TaskStatus.RUNNING).withStartTime();
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
            synchronized (this) {
                this.exception = ex;
                this.taskInfo = taskInfo.withStatus(status)
                    .withEndTime()
                    .withException(ex);
            }

            activeTasks.remove(taskInfo.taskId());
            completedTasks.add(this);
            taskListener.onTaskFinished(taskInfo.taskId(), taskInfo.taskName(), status, ex);
        }

        @Override
        protected Throwable getException() {
            return exception;
        }
    }

    // 委托方法实现（保持原有功能）
    @Override public void shutdown() { delegate.shutdown(); }
    @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
    @Override public boolean isShutdown() { return delegate.isShutdown(); }
    @Override public boolean isTerminated() { return delegate.isTerminated(); }
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
    @Override public void execute(Runnable command) {
        submit(command, "Unnamed-" + command.getClass().getSimpleName());
    }
    

    public void execute(Runnable command, String taskName) {
        submit(command, taskName);
    }

    private static String generateTaskId(String taskName) {
        return taskName + "-" + TASK_ID_GEN.incrementAndGet();
    }

    // 状态记录对象
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

    public enum TaskStatus {
        CREATED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public interface TaskListener {
        default void onTaskStarted(String taskId, String taskName) {}
        default void onTaskFinished(String taskId, String taskName, TaskStatus status, Throwable exception) {}
    }
}