package com.jaewa.commandchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jaewa.commandchain.Commands.interruptible;
import static com.jaewa.commandchain.Commands.safe;

/**
 * <h2>CommandExecutor</h2>
 * <p>
 * CommandExecutor manages the execution of a chain of asynchronous commands.
 * Each command in the chain is responsible for explicitly continuing the execution
 * by calling {@link CommandChain#next()} upon successful completion, or terminating
 * the chain by calling {@link CommandChain#fail(Throwable)} in case of an error.
 * This design gives each command full control over the execution flow.
 * </p>
 * <p>
 * Command execution is fully asynchronous: each command runs on a separate thread
 * managed by the {@link ExecutorService}. This means that the thread executing a
 * command is different from the thread that invokes the {@link #start()} method,
 * and also different from the thread that calls {@link CommandChain#next()} or
 * {@link CommandChain#fail(Throwable)} on the CommandChain. This ensures non-blocking
 * execution and allows commands to perform long-running operations without blocking
 * the caller or other commands.
 * </p>
 * <p>
 * It supports sequential execution, error handling, and interruption of the command chain.
 * Commands can be added to the executor and executed in the order they were added.
 * It also allows handling failures through an optional failure handler.
 * </p>
 * <p>
 * To create a CommandExecutor instance, use the {@link #builder()} method, which returns
 * a builder with a fluent API for configuring and constructing the executor.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Manages a list of asynchronous commands with names for identification.</li>
 *   <li>Executes commands sequentially, providing a shared {@link Context} object.</li>
 *   <li>Each command controls the chain flow by calling {@code next()} or {@code fail()}.</li>
 *   <li>Commands execute asynchronously on separate threads managed by ExecutorService.</li>
 *   <li>Handles execution failures and supports custom failure handling.</li>
 *   <li>Allows the chain to be interrupted during execution.</li>
 * </ul>
 */
public class CommandExecutor implements CommandChain, AsyncCommand {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutor.class);

    private final List<ImmutablePair<String, AsyncCommand>> commands;

    private AsyncFailureHandler failureHandler;

    private int executionIndex = -1;

    private Throwable failure = null;

    private CompletableFuture<Void> future;

    private final Context context;

    private boolean continuous = false;

    /**
     * Creates and returns a new fluent builder for constructing a {@code CommandExecutor} instance.
     *
     * @return a new fluent builder instance.
     */
    public static MainExecutorBuilder builder() {
        return new MainExecutorBuilder(new ContextImpl());
    }

    CommandExecutor(Context context) {
        this.commands = new ArrayList<>();
        this.context = context;
    }

    /**
     * Adds an asynchronous command to the command queue with the specified name.
     * The command is wrapped to ensure it handles interruptions and exceptions gracefully.
     *
     * @param name the name identifying the command
     * @param cmd  the asynchronous command to be added
     */
    public synchronized void add(String name, AsyncCommand cmd) {
        commands.add(ImmutablePair.of(name, interruptible(safe(cmd))));
        if (continuous && future.isDone()) {
            executionIndex--;
            future = new CompletableFuture<>();
            failure = null;
            next();
        }
    }

    /**
     * Starts the execution of the command chain asynchronously.
     * This method initializes necessary state variables, begins
     * the processing of commands in the queue, and returns a
     * {@link CompletableFuture} that represents the asynchronous
     * execution of the command chain. The future completes each time
     * the CommandExecutor completes, either successfully when all
     * commands have been executed, or exceptionally if any command
     * fails or an error occurs.
     *
     * @return a {@link CompletableFuture} that completes when the
     * command chain execution is finished, either successfully
     * or with an exception.
     */
    public CompletableFuture<Void> start() {
        return startImpl();
    }

    /**
     * Initiates the executor in continuous mode, allowing the command
     * execution process to operate persistently and continuously without stopping.
     * This method sets the internal state for continuous operation and begins the
     * execution workflow by invoking the implementation-specific logic.
     *
     * <p>
     * In continuous mode, when a new command is added via {@link #add(String, AsyncCommand)}
     * after the previous execution has completed (i.e., the future is done), the internal
     * future is reset. This means it is no longer in a done state and becomes a new
     * {@link Future} that can be used to track the completion of the newly
     * added commands. This reset behavior occurs automatically every time a command is
     * added when the previous execution cycle has finished, allowing the executor to
     * seamlessly restart and process the new commands while providing a fresh future
     * for observing the execution state.
     * </p>
     *
     * @return a {@link Future} representing the lifecycle of the continuous execution
     * process, enabling control and observation over its asynchronous behavior,
     * such as checking completion or handling interruptions.
     */
    public Future<Void> startContinuous() {
        continuous = true;
        startImpl();
        return new ContinuousFuture();
    }

    private CompletableFuture<Void> startImpl() {
        future = new CompletableFuture<>();
        executionIndex = -1;
        failure = null;
        next();
        return future;
    }


    @Override
    public synchronized void next() {
        if (failure != null) {
            future.completeExceptionally(failure);
            return;
        }
        ImmutablePair<String, AsyncCommand> cmdPair = null;
        executionIndex++;
        if (executionIndex < commands.size()) {
            cmdPair = commands.get(executionIndex);
        }
        if (cmdPair != null) {
            log.info("Executing command: {}", cmdPair.left);
            executeCommandAsync(cmdPair.left, cmdPair.right);
        } else {
            future.complete(null);
        }
    }

    @Override
    public void fail(Throwable e) {
        if (failure != null) {
            log.error("Command chain already failed", e);
            future.completeExceptionally(failure);
        } else {
            failure = e;
            if (failureHandler != null) {
                failureHandler.execute(e, this);
            } else {
                future.completeExceptionally(e);
            }
        }
    }

    private void executeCommandAsync(String name, AsyncCommand cmd) {
        ExecutorService.execute(() -> {
            cmd.execute(context, this);
            log.debug("Command {} executed", name);
        });
    }

    @Override
    public void execute(Context ctx, CommandChain chain) {
        start().thenRun(chain::next)
                .exceptionally(e -> {
                    fail(e.getCause() != null ? e.getCause() : e);
                    return null;
                });
    }

    /**
     * Interrupts the current execution context by delegating the interrupt call
     * to the associated {@code Context} object. This method is used to signal
     * an interruption in a command execution workflow, which may cause subsequent
     * commands or operations to stop processing based on the interruption state.
     */
    public void interrupt() {
        context.interrupt();
    }

    /**
     * Sets the failure handler that will be invoked when a failure occurs during
     * the execution of the command chain. The provided failure handler defines
     * how to handle exceptions or errors specific to the asynchronous context.
     *
     * @param failureHandler the {@code AsyncFailureHandler} implementation to manage
     *                       errors and exceptions during the execution of the command chain
     */
    public void setFailureHandler(AsyncFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
    }

    /**
     * Retrieves the current execution context associated with this {@code CommandExecutor}.
     * The context provides the shared state and control mechanisms required for managing
     * the command execution workflow. It allows storing, retrieving, and manipulating
     * data while tracking and handling interruptions.
     *
     * @return the {@code Context} object associated with the current command execution
     */
    public Context getContext() {
        return context;
    }

    private class ContinuousFuture implements Future<Void> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }
}
