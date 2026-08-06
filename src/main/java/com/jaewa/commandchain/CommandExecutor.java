package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import com.jaewa.commandchain.service.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * command is different from the thread that invokes the {@link #start(Context)} method,
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
 * To create a CommandExecutor instance, use the {@link #pipelineBuilder()} or {@link #queueBuilder()} methods, which return
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

    private AsyncFailureHandler failureHandler;

    private boolean failureHandlerHasHandled;

    private CompletableFuture<Void> future;

    private boolean continuous = false;

    private Context currentContext;

    private final CommandSource commandSource;


    /**
     * Creates and returns a builder for constructing a {@link MainExecutorBuilder} instance.
     * This method initializes a new {@link CommandPipeline} as the command source for the builder.
     * The returned builder can be used to configure and construct a {@code CommandExecutor}
     * capable of managing and executing a sequence of asynchronous commands.
     *
     * @return an instance of {@link MainExecutorBuilder} initialized with a new {@link CommandPipeline}
     */
    public static MainExecutorBuilder pipelineBuilder() {
        return new MainExecutorBuilder(new CommandPipeline());
    }

    /**
     * Creates and returns a builder for constructing a {@link MainExecutorBuilder} instance.
     * This method initializes a new {@link CommandQueue} as the command source for the builder.
     * The returned builder can be used to configure and construct a {@code CommandExecutor}
     * capable of managing and executing a sequence of asynchronous commands in a queued manner.
     *
     * @return an instance of {@link MainExecutorBuilder} initialized with a new {@link CommandQueue}
     */
    public static MainExecutorBuilder queueBuilder() {
        return new MainExecutorBuilder(new CommandQueue());
    }

    /**
     * Constructs a new instance of {@code CommandExecutor} with the specified context.
     * The provided {@code Context} object will be used to manage shared state,
     * handle interruptions, and facilitate data sharing during the execution
     * of the command chain.
     *
     */
    public CommandExecutor() {
        this.commandSource = new CommandPipeline();
    }

    public CommandExecutor(CommandSource commandSource) {
        this.commandSource = commandSource;
    }

    /**
     * Adds an asynchronous command to the command queue with the specified name.
     * The command is wrapped to ensure it handles interruptions and exceptions gracefully.
     *
     * @param cmd the asynchronous command to be added
     */
    public synchronized void add(AsyncCommand cmd) {
        commandSource.add(safe(cmd));
        if (continuous && future.isDone()) {
            future = new CompletableFuture<>();
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
     * @param ctx the {@code Context} object to be associated with this {@code CommandExecutor};
     *            must not be {@code null}
     * @return a {@link CompletableFuture} that completes when the
     * command chain execution is finished, either successfully
     * or with an exception.
     */
    public CompletableFuture<Void> start(Context ctx) {
        return startImpl(ctx);
    }

    /**
     * Initiates the executor in continuous mode, allowing the command
     * execution process to operate persistently and continuously without stopping.
     * This method sets the internal state for continuous operation and begins the
     * execution workflow by invoking the implementation-specific logic.
     *
     * <p>
     * In continuous mode, when a new command is added via {@link #add(AsyncCommand)}
     * after the previous execution has completed (i.e., the future is done), the internal
     * future is reset. This means it is no longer in a done state and becomes a new
     * {@link Future} that can be used to track the completion of the newly
     * added commands. This reset behavior occurs automatically every time a command is
     * added when the previous execution cycle has finished, allowing the executor to
     * seamlessly restart and process the new commands while providing a fresh future
     * for observing the execution state.
     * </p>
     *
     * @param ctx the {@code Context} object to be associated with this {@code CommandExecutor};
     * @return a {@link Future} representing the lifecycle of the continuous execution
     * process, enabling control and observation over its asynchronous behavior,
     * such as checking completion or handling interruptions.
     */
    public Future<Void> startContinuous(Context ctx) {
        continuous = true;
        startImpl(ctx);
        return new ContinuousFuture();
    }

    private CompletableFuture<Void> startImpl(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        currentContext = ctx;
        future = new CompletableFuture<>();
        commandSource.init();
        failureHandlerHasHandled = false;
        next();
        return future;
    }


    @Override
    public synchronized void next() {
        if (currentContext.isInterrupted()) {
            log.warn("Execution interrupted");
            future.completeExceptionally(new CommandInterruptedException());
        }else {
            AsyncCommand cmd = commandSource.next();
            if (cmd != null) {
                log.info("Executing command: {}", cmd);
                executeCommandAsync(cmd);
            } else {
                future.complete(null);
            }
        }
    }

    @Override
    public void fail(Throwable e) {
        if (!failureHandlerHasHandled && failureHandler != null) {
            failureHandlerHasHandled = true;
            failureHandler.execute(e, this);
        } else {
            future.completeExceptionally(e);
        }
    }

    private void executeCommandAsync(AsyncCommand cmd) {
        ExecutorService.execute(() -> {
            cmd.execute(currentContext, this);
            log.debug("Command {} executed", cmd);
        });
    }

    @Override
    public void execute(Context ctx, CommandChain chain) {
        start(ctx).thenRun(chain::next)
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
        if (currentContext != null) {
            currentContext.interrupt();
        }
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
     * The context provides a shared state for managing data, tracking execution flow,
     * and handling interruptions across the command chain execution process.
     *
     * @return the current {@code Context} instance associated with this {@code CommandExecutor},
     * or {@code null} if no context is currently set.
     */
    public Context getCurrentContext() {
        return currentContext;
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
