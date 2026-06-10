package com.jaewa.commandchain;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorService {

    private static final Executor executor = Executors.newCachedThreadPool();

    private ExecutorService() {}

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }
}
