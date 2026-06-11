package com.jaewa.commandchain;

import java.util.concurrent.CompletableFuture;


/**
 * Exception that causes the {@link CompletableFuture} returned from the 
 * {@link CommandExecutor#start()} method to complete exceptionally when 
 * the command chain execution is interrupted.
 * <p>
 * This exception is not thrown directly by the CompletableFuture, but rather
 * serves as the underlying cause of CompletableFuture's standard exception types
 * (such as {@link java.util.concurrent.CompletionException} or 
 * {@link java.util.concurrent.ExecutionException}). When the execution context 
 * is interrupted by calling {@link CommandExecutor#interrupt()}, which sets the 
 * {@link Context} to an interrupted state, and a command detects this interruption 
 * state, the CompletableFuture completes exceptionally with this exception as the cause.
 * </p>
 *
 * @see CommandExecutor#start()
 * @see CommandExecutor#interrupt()
 * @see Context#isInterrupted()
 */
public class CommandInterruptedException extends Exception {

}
