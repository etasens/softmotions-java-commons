package com.softmotions.weboot.executor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.softmotions.weboot.WBConfiguration;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TaskExecutorModule extends AbstractModule implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorModule.class);

    final WBConfiguration env;

    ExecutorService executor;

    public TaskExecutorModule(WBConfiguration env) {
        this.env = env;
    }

    @Override
    protected void configure() {
        XMLConfiguration xcfg = env.xcfg();

        int threads;
        if ("allcores".equalsIgnoreCase(xcfg.getString("executor.threads-num"))) {
            threads = Runtime.getRuntime().availableProcessors();
        } else {
            threads = Math.max(1, xcfg.getInt("executor.threads-num",
                                              Runtime.getRuntime().availableProcessors()));
        }

        log.info("Using {} threads for tasks executor", threads);
        executor = new ThreadPoolExecutor(0, threads,
                                          60L, TimeUnit.SECONDS,
                                          new SynchronousQueue<>());
        bind(TaskExecutor.class).toInstance(this);
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }


    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }


    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }


    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }


    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);

    }
}