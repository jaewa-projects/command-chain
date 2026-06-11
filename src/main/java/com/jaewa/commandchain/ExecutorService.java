package com.jaewa.commandchain;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


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

    private static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    private ExecutorService() {
    }

    /**
     * <p>
     * Executes a given task asynchronously using the underlying virtual thread executor.
     * </p>
     *
     * @param runnable the task to be executed; must not be null
     */
    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }
}
