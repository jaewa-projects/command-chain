package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Context;
import com.jaewa.commandchain.IfCommand;
import java.util.function.Predicate;

public class IfExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<CommandBlockBuilder<P>, P> {

    private final IfCommand ifCommand;

    public IfExecutorBuilder(P parentBuilder) {
        super(parentBuilder);
        ifCommand = new IfCommand();
    }

    public CommandBlockBuilder<IfExecutorBuilder<P>> when(Predicate<Context> condition) {
        ifCommand.when(condition);
        return new CommandBlockBuilder<>(ifCommand, this);
    }

    public CommandBlockBuilder<IfExecutorBuilder<P>> otherwise() {
        ifCommand.when(c -> true);
        return new CommandBlockBuilder<>(ifCommand, this);
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return ifCommand.getBlockCommandExecutor();
    }

    public AsyncCommand build() {
        return ifCommand;
    }
}
