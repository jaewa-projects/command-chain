package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.CommandSource;

/**
 * Builder for the main command executor.
 */
public class MainExecutorBuilder extends AbstractExecutorBuilder<MainExecutorBuilder> {

    private final CommandExecutor commandExecutor;

    /**
     * Creates a new MainExecutorBuilder.
     *
     * @param commandSource the source of commands for the executor
     */
    public MainExecutorBuilder(CommandSource commandSource) {
        commandExecutor = new CommandExecutor(commandSource);
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    /**
     * Builds and returns a {@link CommandExecutor} instance.
     * This method finalizes the builder configuration and provides
     * the constructed {@code CommandExecutor}, which can be used to
     * execute a chain of asynchronous commands.
     *
     * @return an instance of {@link CommandExecutor} configured by this builder
     */
    public CommandExecutor build() {
        return commandExecutor;
    }
}
