package com.jaewa.commandchain;

/**
 * <h2>CommandTimeoutException</h2>
 * <p>
 * Exception thrown or passed to {@link CommandChain#fail(Throwable)} when a command
 * decorated with a timeout does not complete (i.e. does not call {@code next()} or
 * {@code fail()}) within the specified timeout duration.
 * </p>
 *
 * @see Commands#withTimeout(long, java.util.concurrent.TimeUnit, AsyncCommand)
 */
public class CommandTimeoutException extends Exception {

    /**
     * Constructs a new {@code CommandTimeoutException} with a default detail message.
     */
    public CommandTimeoutException() {
        super("Command execution timed out");
    }

    /**
     * Constructs a new {@code CommandTimeoutException} with the specified detail message.
     *
     * @param message the detail message
     */
    public CommandTimeoutException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code CommandTimeoutException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public CommandTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code CommandTimeoutException} with the specified cause.
     *
     * @param cause the cause
     */
    public CommandTimeoutException(Throwable cause) {
        super(cause);
    }
}
