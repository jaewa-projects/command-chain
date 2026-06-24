package com.jaewa.commandchain;

import java.util.LinkedList;
import java.util.Queue;
import org.apache.commons.lang3.tuple.Pair;


/**
 * CommandQueue is a concrete implementation of the {@link CommandSource} interface.
 * It provides a queue-based mechanism to manage and retrieve asynchronous commands
 * for execution in a sequential or specific order.
 * This class maintains an internal queue of command entries, where each entry consists
 * of a pair containing the command's name and its associated {@link AsyncCommand} instance.
 * Commands can be added to the queue, and then retrieved one at a time in the order they
 * were added. When a command is retrieved using the {@link #next()} method, it is removed
 * from the queue.
 *
 */
public class CommandQueue implements CommandSource{

    private final Queue<Pair<String, AsyncCommand>> queue = new LinkedList<>();

    @Override
    public void add(String commandName, AsyncCommand command) {
        queue.add(Pair.of(commandName, command));
    }

    @Override
    public void init() {
        //do nothing
    }

    /**
     * Retrieves and removes the next command from the queue.
     * Once a command is retrieved, it is permanently removed from the queue.
     *
     * @return a pair containing the command name and the associated {@link AsyncCommand},
     * or null if the queue is empty
     */
    @Override
    public Pair<String, AsyncCommand> next() {
        return queue.poll();
    }
}
