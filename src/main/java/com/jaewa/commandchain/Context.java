package com.jaewa.commandchain;


/**
 * <p>
 * Represents a shared context for storing and retrieving data across different parts
 * of a chain. This interface defines methods to set and get variables, as well as
 * handle interruption flags to coordinate execution flow.
 * </p>
 */
public interface Context {

    /**
     * <p>
     * Stores a value in the context under the specified variable name. This method allows
     * data to be shared across different parts of the chain by associating it with
     * a string-based key.
     * </p>
     *
     * @param variableName the name of the variable to store the value under; must not be {@code null}
     * @param value the value to be stored; can be any object or {@code null}
     */
    void set(String variableName, Object value);

    /**
     * <p>
     * Retrieves the value associated with the specified variable name from the context
     * and casts it to the given type.
     * </p>
     *
     * @param <E> the expected type of the value to be retrieved
     * @param variableName the name of the variable to retrieve; must not be {@code null}
     * @param type the class object representing the expected type of the value; must not be {@code null}
     * @return the value associated with the specified variable name, cast to the provided type,
     *         or {@code null} if no value is associated with the variable name or the value is {@code null}
     * @throws ClassCastException if the value associated with the variable name cannot be cast to the specified type
     */
    <E> E get(String variableName, Class<E> type);

    /**
     * <p>
     * Checks if the current context's execution flow has been interrupted.
     * </p>
     * <p>
     * This method is used to determine whether the context has been flagged
     * as interrupted, which may signal that the execution should terminate
     * early or avoid proceeding further in a chain of operations.
     * </p>
     *
     * @return {@code true} if the context has been interrupted, {@code false} otherwise
     */
    boolean isInterrupted();

    /**
     * <p>
     * Marks the current execution context as interrupted.
     * </p>
     * <p>
     * This method sets an interruption flag within the context, signaling that
     * the execution flow should be halted or terminated prematurely. Once the interrupt
     * flag is set, subsequent operations in the chain can check the interruption status
     * using the {@link #isInterrupted()} method and take necessary actions
     * (e.g., stopping further processing or breaking out of a loop).
     * </p>
     *
     */
    void interrupt();
}
