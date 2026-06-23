package com.jaewa.commandchain;

/**
 * <p>
 * Represents a chain-like execution control mechanism that manages the
 * flow of asynchronous or synchronous commands in a sequence. Typically,
 * this interface is used to coordinate the execution of tasks where each
 * task has the ability to move the chain forward or to terminate it due to
 * an error.
 * </p>
 * <p>
 * Implementations of this interface are designed to be used within larger
 * command chain architectures, enabling structured workflows with well-defined
 * error handling and sequencing of operations.
 * </p>
 */
public interface CommandChain {

    /**
     * <p>
     * Advances the execution to the next step in a chain of commands or tasks.
     * This method is typically called to signal the continuation of the workflow
     * within a command chain. Implementations may use this to coordinate the
     * sequencing of operations or tasks in synchronous or asynchronous contexts.
     * </p>
     * <p>
     * When invoked, the control moves to the following element in the chain,
     * allowing the designed flow to progress as intended. If there are no more
     * tasks or commands in the chain, the specific behavior of the implementation
     * will dictate whether the flow concludes or an error is raised.
     * </p>
     * <p>
     * This method is commonly used in conjunction with mechanisms for error handling
     * (e.g., {@code fail(Throwable)}) and allows dynamic coordination of dependent
     * tasks in a modular and reusable manner.
     * </p>
     *
     */
    void next();

    /**
     * <p>
     * Signals a failure in the command chain and halts further execution of the chain.
     * This method is typically used to propagate an encountered exception or error
     * to terminate the chain execution prematurely.
     * </p>
     * <p>
     * Once this method is called, no subsequent commands in the chain will be executed,
     * unless the specific implementation allows error recovery or fallback mechanisms.
     * </p>
     *
     * @param e the throwable representing the failure or error that caused the chain to stop
     */
    void fail(Throwable e);
}
