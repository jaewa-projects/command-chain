package com.jaewa.commandchain;

/**
 * Represents an iterative process in which specific initialization, condition checking,
 * and iteration logic are implemented. This interface provides the core contract for
 * implementing loops within a context-aware environment.
 */
public interface Loop {

    /**
     * Initializes the iterative process within the given context. This method is responsible
     * for performing any setup or preparation necessary before the loop starts executing.
     *
     * @param ctx the context in which the iterative process operates; used to manage shared state
     *            across different parts of the process and control execution flow
     */
    void init(Context ctx);

    /**
     * Checks whether the iterative process has more elements to process or conditions to satisfy.
     * This method is typically used in loop constructs to determine if the iteration should continue.
     *
     * @return true if there are more elements to process or conditions are satisfied to iterate further;
     *         false otherwise
     */
    boolean hasNext();

    /**
     * Advances the iterative process by updating the current value to the next state.
     * This method typically applies a predefined update or transformation logic to the current state
     * of the iteration, ensuring that the process progresses according to the defined iteration rules.
     * Implementations of this method are expected to modify internal state variables or other
     * relevant properties in a manner consistent with the loop's progression requirements.
     * Must be invoked only when {@link #hasNext()} returns true to avoid undefined behavior.
     */
    void next();

    /**
     * Adds a named asynchronous command to the loop. This method is used to register
     * a command that executes asynchronously and can be invoked as part of the iteration
     * process within the loop.
     *
     * @param name the unique name assigned to the asynchronous command; used to identify
     *             the command within the loop
     * @param cmd  the asynchronous command to be added, which defines its execution logic
     *             and interactions with the loop's context and flow
     */
    void add(String name, AsyncCommand cmd);
}
