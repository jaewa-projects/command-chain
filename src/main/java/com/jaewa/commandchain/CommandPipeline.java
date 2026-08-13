package com.jaewa.commandchain;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a pipeline for managing and executing a sequence of asynchronous commands.
 * The CommandPipeline class implements the CommandSource interface and provides mechanisms
 * to add commands, initialize the pipeline, and iterate through the commands sequentially.
 * This class maintains an internal list of commands and allows
 * the commands to be executed in the order they were added.
 *
 * <p><b>Important:</b> Commands remain in the pipeline after execution and are not removed.
 * This behavior differs from {@link CommandQueue}, where commands are removed after execution.
 * The pipeline can be re-initialized using {@code init()} to reset the execution index
 * and iterate through the same commands again.</p>
 *
 * The pipeline is initialized by calling the {@code init()} method, which resets
 * the internal execution index. Commands can then be retrieved one by one using the {@code next()}
 * method, which returns an {@link AsyncCommand} or {@code null} if no more commands are available.
 */
public class CommandPipeline implements CommandSource {

    private final List<AsyncCommand> commands;

    private int executionIndex = -1;

    /**
     * Creates a new CommandPipeline.
     */
    public CommandPipeline() {
        this.commands = new ArrayList<>();
    }

    @Override
    public void add(AsyncCommand command) {
        this.commands.add(command);
    }

    @Override
    public void init() {
        executionIndex = -1;
    }

    @Override
    public AsyncCommand next() {
        if (executionIndex < commands.size() - 1) {
            executionIndex++;
            return commands.get(executionIndex);
        }
        return null;
    }
}
