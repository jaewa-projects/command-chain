package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandBlock;
import com.jaewa.commandchain.CommandExecutor;

public class CommandBlockBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<CommandBlockBuilder<P>, P> {

    private final CommandBlock commandBlock;

    public CommandBlockBuilder(CommandBlock commandBlock, P parentBuilder) {
        super(parentBuilder);
        this.commandBlock = commandBlock;
    }

    AsyncCommand build() {
        return commandBlock;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return commandBlock.getBlockCommandExecutor();
    }
}
