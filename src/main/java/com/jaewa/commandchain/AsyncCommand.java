package com.jaewa.commandchain;

/**
 * <h2>AsyncCommand</h2>
 * <p>
 * Represents an asynchronous command that can be executed as part of a command chain.
 * Implementations of this interface define logic to be performed asynchronously,
 * where the continuation of execution is delegated directly to the command itself
 * through the {@link CommandChain#next()} or {@link CommandChain#fail(Throwable)} methods.
 * </p>
 * <p>
 * The {@code AsyncCommand} interface is typically used to compose and execute
 * sequences of asynchronous operations represented by a {@link CommandChain}.
 * </p>
 * <p>
 * A command implementing this interface must handle its logic within the
 * {@link #execute(Context, CommandChain)} method, interacting with the
 * provided {@code Context} object for shared state and the {@code CommandChain}
 * for progression or termination of the chain.
 * </p>
 * <p>
 * Error handling should be managed internally within the {@code execute} method.
 * If an error occurs, the command can explicitly call {@link CommandChain#fail(Throwable)}
 * to terminate the chain with a failure. Alternatively, commands may throw unchecked
 * exceptions, which are automatically caught by {@link CommandExecutor} and handled
 * as if {@link CommandChain#fail(Throwable)} had been called directly.
 * </p>
 */
public interface AsyncCommand {

    /**
     * Executes the asynchronous command logic as part of a chain.
     * This method is invoked with a context object for managing shared state
     * and a command chain object for controlling execution flow.
     *
     * Implementations of this method should perform their specific logic,
     * and then explicitly decide whether to progress the chain or terminate it
     * by invoking appropriate methods on the {@code CommandChain} instance.
     *
     * Commands must handle errors internally and use {@link CommandChain#fail(Throwable)}
     * if an error occurs to terminate the chain with the provided exception.
     * Alternatively, they can allow unchecked exceptions to propagate, which are treated
     * as a failure by the chain execution mechanism.
     *
     * @param ctx the context object providing shared state for this command execution
     * @param chain the command chain managing the execution flow and allowing progression or termination
     */
    void execute(Context ctx, CommandChain chain);
}
