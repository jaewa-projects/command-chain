package com.jaewa.commandchain;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


/**
 * <h2>ExecutorService</h2>
 * <p>
 * Provides a simple utility class for executing tasks asynchronously using an underlying
 * virtual thread executor. This class utilizes a static {@link Executor} instance with
 * a virtual thread per task, achieved through the {@link Executors#newVirtualThreadPerTaskExecutor()} method.
 * </p>
 * <p>
 * It allows for convenient execution of {@link Runnable} tasks without the need to explicitly manage
 * the lifecycle or configuration of the executor. The singleton design pattern is used to ensure
 * that only one executor instance is utilized throughout the application.
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
     *
     * Note: Changing the executor supplier affects all subsequent task executions, as it
     * dictates the instance of the executor that will be retrieved. Ensure the new supplier
     * provides a valid and functional {@link Executor} implementation.
     *
     * @param executorSupplier a {@link Supplier} of {@link Executor} instances; must not be null
     */
    public static void setExecutorSupplier(Supplier<Executor> executorSupplier) {
        ExecutorService.executorSupplier = executorSupplier;
        executor = null;
    }

    /**
     * <p>
     * Executes a given task asynchronously using the underlying virtual thread executor.
     * </p>
     *
     * @param runnable the task to be executed; must not be null
     */
    public static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }
}
