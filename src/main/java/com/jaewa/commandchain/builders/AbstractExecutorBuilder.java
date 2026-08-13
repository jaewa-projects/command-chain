package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AbstractLoop;
import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.AsyncFailureHandler;
import com.jaewa.commandchain.Command;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Commands;
import com.jaewa.commandchain.FailureHandler;

/**
 * An abstract builder class that provides a fluent API for constructing and configuring
 * command executors. This class is designed to serve as a base class for specific
 * executor builder implementations.
 *
 * @param <B> the type of the concrete builder extending this class
 */
public abstract class AbstractExecutorBuilder<B extends AbstractExecutorBuilder<?>> {

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    /**
     * Adds a command to the command executor and returns the current builder instance.
     * The provided command is executed asynchronously.
     *
     * @param cmd  the command to be executed asynchronously
     * @return the current builder instance for chaining method calls
     */
    public B exec(Command cmd) {
        getCommandExecutor().add(Commands.async(cmd));
        return self();
    }

    /**
     * Adds an asynchronous command to the command executor and returns the current builder instance.
     * The provided command is executed asynchronously as part of the command execution flow.
     *
     * @param cmd  the asynchronous command to be executed
     * @return the current builder instance for chaining method calls
     */
    public B exec(AsyncCommand cmd) {
        getCommandExecutor().add(cmd);
        return self();
    }

    /**
     * Adds a wiretap to the command executor and returns the current builder instance.
     * The provided {@code Runnable} is executed each time the associated command is processed,
     * allowing for side-effect operations such as logging or monitoring.
     *
     * @param runnable the {@code Runnable} to be executed as part of the wiretap
     * @return the current builder instance for chaining method calls
     */
    public B wiretap(Runnable runnable) {
        getCommandExecutor().add(Commands.wireTap(runnable));
        return self();
    }

    /**
     * Sets a failure handler to be invoked when an error occurs during the execution
     * of the command chain. This method allows specifying custom failure-handling logic
     * by providing a {@code FailureHandler} implementation.
     * The failure handler is executed asynchronously in response to exceptions or
     * errors encountered in the execution flow.
     *
     * @param cmd the {@code FailureHandler} implementation that defines the custom
     *            error-handling logic; must not be {@code null}
     * @return the current builder instance for chaining method calls
     */
    public B onFailure(FailureHandler cmd) {
        getCommandExecutor().setFailureHandler(Commands.async(cmd));
        return self();
    }

    /**
     * Sets an asynchronous failure handler to be invoked when a failure occurs during
     * the execution of the command chain. The provided handler defines custom logic
     * for managing and resolving errors in the asynchronous context. Such logic can
     * include logging the error, cleaning up resources, or other error-handling operations.
     *
     * @param cmd the {@code AsyncFailureHandler} implementation that defines the
     *            custom failure-handling logic; must not be {@code null}.
     * @return the current builder instance for chaining method calls.
     */
    public B onFailure(AsyncFailureHandler cmd) {
        getCommandExecutor().setFailureHandler(cmd);
        return self();
    }

    /**
     * Adds a looping executor to the command executor.
     * The looping executor repeatedly executes commands as defined by the {@link AbstractLoop} interface.
     *
     * @param loop the {@code AbstractLoop} implementation defining the initialization, iteration,
     *             and termination logic of the loop
     * @return a builder to further configure the loop executor
     */
    public CommandBlockBuilder<B> loop(AbstractLoop loop) {
        CommandBlockBuilder<B> result = new CommandBlockBuilder<>(loop, self());
        getCommandExecutor().add(result.build());
        return result;
    }

    /**
     * Creates and returns a new {@link ChoiceBuilder} instance associated with
     * the current builder. The {@code ChoiceExecutorBuilder} facilitates the configuration
     * of conditional command execution logic, allowing for branching execution flows
     * based on specific conditions.
     * The created {@code ChoiceExecutorBuilder} is added to the current
     * {@link CommandExecutor} as part of the ongoing command configuration.
     *
     * @return a {@link ChoiceBuilder} instance for configuring conditional
     *         execution logic
     */
    public ChoiceBuilder<B> choice() {
        ChoiceBuilder<B> result = new ChoiceBuilder<>(self());
        getCommandExecutor().add(result.build());
        return result;
    }

    /**
     * Creates and returns a new {@link TryCatchFinallyBuilder} instance associated with
     * the current builder. This facilitates the configuration of try-catch-finally
     * execution logic.
     *
     * @return a {@link TryCatchFinallyBuilder} instance for configuring error handling
     *         and finalization logic
     */
    public TryCatchFinallyBuilder<B> doTry() {
        TryCatchFinallyBuilder<B> result = new TryCatchFinallyBuilder<>(self());
        getCommandExecutor().add(result.build());
        return result;
    }

    /**
     * Creates a new AbstractExecutorBuilder.
     */
    protected AbstractExecutorBuilder() {
    }

    /**
     * Retrieves the {@link CommandExecutor} managed by this builder.
     *
     * @return the command executor instance
     */
    protected abstract CommandExecutor getCommandExecutor();
}
