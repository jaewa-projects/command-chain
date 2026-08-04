package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AbstractLoop;
import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.AsyncFailureHandler;
import com.jaewa.commandchain.Command;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Commands;
import com.jaewa.commandchain.Context;
import com.jaewa.commandchain.FailureHandler;
import com.jaewa.commandchain.Loop;
import java.util.function.Predicate;

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
     * Adds a command to the command executor with the specified name and returns the current builder instance.
     * The provided command is executed asynchronously.
     *
     * @param name the name identifying the command
     * @param cmd  the command to be executed asynchronously
     * @return the current builder instance for chaining method calls
     */
    public B exec(String name, Command cmd) {
        getCommandExecutor().add(name, Commands.async(cmd));
        return self();
    }

    /**
     * Adds an asynchronous command to the command executor with the specified name and returns the current builder instance.
     * The provided command is executed asynchronously as part of the command execution flow.
     *
     * @param name the name identifying the command
     * @param cmd  the asynchronous command to be executed
     * @return the current builder instance for chaining method calls
     */
    public B exec(String name, AsyncCommand cmd) {
        getCommandExecutor().add(name, cmd);
        return self();
    }

    /**
     * Adds a wiretap to the command executor with the specified name and returns the current builder instance.
     * The provided {@code Runnable} is executed each time the associated command is processed,
     * allowing for side-effect operations such as logging or monitoring.
     *
     * @param name    the name identifying the wiretap
     * @param runnable the {@code Runnable} to be executed as part of the wiretap
     * @return the current builder instance for chaining method calls
     */
    public B wiretap(String name, Runnable runnable) {
        getCommandExecutor().add(name, Commands.wireTap(runnable));
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
     * Adds a looping executor to the command executor with the specified name.
     * The looping executor repeatedly executes commands as defined by the {@link Loop} interface.
     *
     * @param name the name identifying the loop executor
     * @param loop the {@code Loop} implementation defining the initialization, iteration,
     *             and termination logic of the loop
     * @return a builder to further configure the loop executor
     */
    public LoopExecutorBuilder<B> loop(String name, AbstractLoop loop) {
        LoopExecutorBuilder<B> result = new LoopExecutorBuilder<>(loop, self());
        getCommandExecutor().add(name, result.build());
        return result;
    }

    public IfExecutorBuilder<B> ifCondition(String name, Predicate<Context> condition) {
        IfExecutorBuilder<B> result = new IfExecutorBuilder<>(condition, self());
        getCommandExecutor().add(name, result.build());
        return result;
    }

    protected abstract CommandExecutor getCommandExecutor();
}
