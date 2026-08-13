package com.jaewa.commandchain.service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


/**
 * <h2>ExecutorService</h2>
 * <p>
 * Provides a simple utility class for executing tasks asynchronously using an underlying
 * {@link Executor}. By default, it uses a cached thread pool.
 * </p>
 * <p>
 * It allows for convenient execution of {@link Runnable} tasks without the need to explicitly manage
 * the lifecycle or configuration of the executor.
 * </p>
 * <p>
 * This class cannot be instantiated directly and only provides static methods for task execution.
 * </p>
 */
public class ExecutorService {

    private static Supplier<Executor> executorSupplier = Executors::newCachedThreadPool;

    private static Executor executor = null;

    private ExecutorService() {
    }

    private static Executor getExecutor() {
        if (executor == null) {
            executor = executorSupplier.get();
        }
        return executor;
    }

    /**
     * Sets the supplier responsible for creating the executor instance used by the service.
     * This allows customization of the executor implementation, ensuring flexibility
     * in how tasks are executed asynchronously.
     * Note: Changing the executor supplier affects all subsequent task executions.
     *
     * @param executorSupplier a {@link Supplier} of {@link Executor} instances; must not be null
     */
    public static void setExecutorSupplier(Supplier<Executor> executorSupplier) {
        ExecutorService.executorSupplier = executorSupplier;
        executor = null;
    }

    /**
     * <p>
     * Executes a given task asynchronously using the underlying executor.
     * </p>
     *
     * @param runnable the task to be executed; must not be null
     */
    public static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }
}
