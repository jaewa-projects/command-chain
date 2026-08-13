package com.jaewa.commandchain;

import java.util.LinkedList;
import java.util.Queue;


/**
 * CommandQueue is a concrete implementation of the {@link CommandSource} interface.
 * It provides a queue-based mechanism to manage and retrieve asynchronous commands.
 * Commands can be added to the queue, and then retrieved one at a time in the order they
 * were added. When a command is retrieved using the {@link #next()} method, it is removed
 * from the queue.
 */
public class CommandQueue implements CommandSource{

    /**
     * Creates a new CommandQueue.
     */
    public CommandQueue() {
    }

    private final Queue<AsyncCommand> queue = new LinkedList<>();

    @Override
    public void add(AsyncCommand command) {
        queue.add(command);
    }

    @Override
    public void init() {
        //do nothing
    }

    /**
     * Retrieves and removes the next command from the queue.
     * Once a command is retrieved, it is permanently removed from the queue.
     *
     * @return the associated {@link AsyncCommand}, or null if the queue is empty
     */
    @Override
    public AsyncCommand next() {
        return queue.poll();
    }
}
