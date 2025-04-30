// NotifyingExecutorService.java (自定义的 ExecutorService 包装类)
package com.manager.ssb.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NotifyingExecutorService implements ExecutorService {

    private final ExecutorService executorService;
    private final TaskListener taskListener;

    public NotifyingExecutorService(ExecutorService executorService, TaskListener taskListener) {
        this.executorService = executorService;
        this.taskListener = taskListener;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public java.util.List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        // 没有任务名称，不通知 TaskListener
        return executorService.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task, String taskName) {
        taskListener.onTaskStarted(taskName);
        return executorService.submit(() -> {
            try {
                T result = task.call();
                taskListener.onTaskFinished(taskName);
                return result;
            } catch (Exception e) {
                taskListener.onTaskFinished(taskName);
                throw e;
            }
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        // 没有任务名称，不通知 TaskListener
        return executorService.submit(task);
    }

    public Future<?> submit(Runnable task, String taskName) {
        taskListener.onTaskStarted(taskName);
        return executorService.submit(() -> {
            try {
                task.run();
                taskListener.onTaskFinished(taskName);
            } catch (Exception e) {
                taskListener.onTaskFinished(taskName);
                throw new RuntimeException(e); // Changed to RuntimeException
            }
        });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        // 没有任务名称，不通知 TaskListener
        return executorService.submit(task, result);
    }

    public <T> Future<T> submit(Runnable task, String taskName, T result) {
         taskListener.onTaskStarted(taskName);

        Callable<T> callable = () -> {
            try {
                task.run();
                taskListener.onTaskFinished(taskName);
                return result;
            } catch (Exception e) {
                taskListener.onTaskFinished(taskName);
                throw new RuntimeException(e);
            }
        };
        return executorService.submit(callable);
    }

    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> java.util.List<Future<T>> invokeAll(java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        execute(command, command.toString()); // 使用默认的任务名称
    }

    public void execute(Runnable command, String taskName) {
        submit(command, taskName);
    }
}