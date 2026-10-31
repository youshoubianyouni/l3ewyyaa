package com.fongmi.quickjs.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Android 5/6 专用的简单 Future 实现。
 *
 * java.util.concurrent.CompletableFuture 是 API 24+ 才有的类，且不会被
 * coreLibraryDesugaring（desugar_jdk_libs_nio）改写，在低版本上会直接
 * NoClassDefFoundError，因此这里用 wait/notify 实现一个只依赖 API 1 的等价物。
 */
public class AsyncFuture<T> implements Future<T> {

    private final Object lock = new Object();
    private T value;
    private Throwable error;
    private boolean done;

    public void complete(T value) {
        synchronized (lock) {
            this.value = value;
            this.done = true;
            lock.notifyAll();
        }
    }

    public void completeExceptionally(Throwable error) {
        synchronized (lock) {
            this.error = error;
            this.done = true;
            lock.notifyAll();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        synchronized (lock) {
            return done;
        }
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        synchronized (lock) {
            while (!done) lock.wait();
            if (error != null) throw new ExecutionException(error);
            return value;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (lock) {
            while (!done) {
                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) throw new TimeoutException();
                lock.wait(remain);
            }
            if (error != null) throw new ExecutionException(error);
            return value;
        }
    }
}
