package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandBlock;
import com.jaewa.commandchain.CommandExecutor;

/**
 * Builder for a block of commands.
 *
 * @param <P> the type of the parent builder
 */
public class CommandBlockBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<CommandBlockBuilder<P>, P> {

    private final CommandBlock commandBlock;

    /**
     * Creates a new CommandBlockBuilder.
     *
     * @param commandBlock  the command block to build
     * @param parentBuilder the parent builder
     */
    public CommandBlockBuilder(CommandBlock commandBlock, P parentBuilder) {
        super(parentBuilder);
        this.commandBlock = commandBlock;
    }

    /**
     * Builds the command block.
     *
     * @return the constructed {@link AsyncCommand} representing the block
     */
    AsyncCommand build() {
        return commandBlock;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return commandBlock.getBlockCommandExecutor();
    }
}
