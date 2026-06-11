package com.jaewa.commandchain;


/**
 * <p>
 * Represents a handler for managing failures that occur during the execution 
 * of commands or loops within a command chain or related processes.
 * </p>
 * <p>
 * This interface defines a contract for handling exceptions or errors 
 * ({@code Throwable} objects) that may arise during the operation of the 
 * command chain architecture. Implementations of this interface provide 
 * custom logic for reacting to or mitigating such failures.
 * </p>
 * <p>
 * Common use cases for {@code FailureHandler} include:
 * </p>
 * <ul>
 *   <li>Logging errors for debugging or auditing purposes.</li>
 *   <li>Taking corrective actions to recover from failures.</li>
 *   <li>Triggering fallback mechanisms to ensure minimal disruption to the system.</li>
 * </ul>
 * <p>
 * The {@code execute} method is expected to contain the failure-handling logic
 * and is invoked when an unhandled exception or error occurs in the associated
 * process.
 * </p>
 */
public interface FailureHandler {

    /**
     * <p>
     * Executes the failure-handling logic for the provided throwable. This method 
     * is invoked when an exception or error occurs during the execution of a command 
     * chain or related processes, allowing for custom error-handling strategies 
     * such as logging, recovery mechanisms, or triggering fallbacks.
     * </p>
     *
     * @param e the throwable that represents the error or exception to be handled; 
     *          must not be {@code null}
     */
    void execute(Throwable e);
}
