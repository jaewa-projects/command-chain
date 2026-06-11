package com.jaewa.commandchain;

/**
 * <h1>Command</h1>
 * <p>
 * Represents a generic command in a command chain architecture.
 * Implementations of this interface define a unit of work or behavior
 * that can be executed within the context of a larger sequence of operations.
 * </p>
 * <p>
 * A command is executed in the context of a {@link Context} object, which provides
 * a shared state for communication between commands. Commands may use the
 * context to store, retrieve, or update data during execution.
 * </p>
 *
 * <h2>Execution Flow Control</h2>
 * <p>
 * Unlike {@link AsyncCommand}, a {@code Command} automatically advances the execution 
 * of the {@link CommandChain} when the {@link #execute(Context)} method terminates normally.
 * This means:
 * </p>
 * <ul>
 *   <li>The normal termination of the {@code execute} method is equivalent to calling 
 *       {@link CommandChain#next()} in an {@link AsyncCommand}</li>
 *   <li>An exception thrown by the {@code execute} method is equivalent to calling 
 *       {@link CommandChain#fail(Throwable)} in an {@link AsyncCommand}</li>
 * </ul>
 * <p>
 * This automatic flow control simplifies the implementation of synchronous commands,
 * as developers do not need to explicitly call {@code next()} or {@code fail()} on the chain.
 * </p>
 *
 */
public interface Command {

	/**
	 * Executes the command within the given context. This method defines the behavior
	 * of the command and communicates with the provided {@code Context} object to store
	 * or retrieve shared state. The command execution may modify the context or respond
	 * to its state.
	 *
	 * @param ctx the execution context used to store or retrieve shared state and
	 *            manage execution flow control
	 * @throws Exception if an error occurs during the execution of the command
	 */
	void execute(Context ctx) throws Exception;
}
