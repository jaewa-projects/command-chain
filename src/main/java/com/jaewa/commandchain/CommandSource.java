package com.jaewa.commandchain;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Represents a source of commands in a command chain architecture.
 * The CommandSource interface provides methods for managing and retrieving
 * asynchronous commands to be executed in sequence or other specific order.
 */
public interface CommandSource {
    /**
     * Adds a named asynchronous command to the command source. This method is used to
     * register a command that can be later executed as part of a command chain.
     *
     * @param commandName the unique name assigned to the asynchronous command; used to
     *                    identify the command within the command source
     * @param command     the asynchronous command to be added, which defines its execution
     *                    logic and interactions within the command chain
     */
    void add(String commandName, AsyncCommand command);

    /**
     * Initializes the command source and its internal iterator. This method is intended to 
     * perform any necessary setup or configuration required before the command source is 
     * ready to be used. It prepares the internal state so that the source can start providing 
     * data from the first element when {@link #next()} is called. Implementations may use 
     * this method to reset internal iterators, prepare data structures, allocate resources, 
     * or perform other initialization tasks specific to the command source.
     */
    void init();

    /**
     * Retrieves the next asynchronous command from the command source along with its associated name.
     * This method is typically used to iterate over the available commands in a predefined order,
     * as determined by the implementation of the command source.
     *
     * @return a pair consisting of a string representing the command's name and the corresponding
     *         {@link AsyncCommand} instance, or {@code null} if there are no more commands available.
     */
    Pair<String, AsyncCommand> next();
}
