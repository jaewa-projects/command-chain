package com.jaewa.commandchain;

/**
 * <h2>AsyncFailureHandler</h2>
 * <p>
 * Represents a handler specifically designed for managing failures in an asynchronous command chain.
 * Implementations of this interface define how to handle exceptions or errors that occur during the 
 * execution of an asynchronous process within the context of a {@link CommandChain}.
 * </p>
 * <p>
 * Asynchronous failure handlers can perform tasks such as logging error details, cleaning up resources, 
 * or attempting recovery. Due to the asynchronous nature of the execution, the handler must call 
 * {@link CommandChain#next()} when it has finished its error management operations (cleanup, etc.) 
 * to properly terminate the command chain execution. Calling {@code next()} does not continue the 
 * chain, but rather signals the completion of the failure handling process and concludes the chain.
 * </p>
 * <p>
 * The {@link CommandChain} is passed to the failure handler to allow it to terminate the chain 
 * execution after completing the error handling operations.
 * </p>
 */
public interface AsyncFailureHandler {

    /**
     * <p>
     * Executes the failure handling logic when an exception occurs in an asynchronous command chain.
     * This method is responsible for managing the error condition represented by the provided 
     * {@code Throwable} instance.
     * </p>
     * <p>
     * Implementations of this method can perform tasks such as logging the error, cleaning up 
     * resources, or taking corrective actions. After completing these operations, the handler 
     * must call {@link CommandChain#next()} to properly terminate the command chain execution. 
     * The {@link CommandChain} parameter provides the mechanism to signal completion of the 
     * failure handling and conclude the chain.
     * </p>
     *
     * @param e the exception or error that triggered the failure handler
     * @param chain the command chain associated with the current execution flow, used for
     *              terminating the chain after error handling is complete
     */
    void execute(Throwable e, CommandChain chain);
}
