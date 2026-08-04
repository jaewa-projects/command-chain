package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Commands;
import com.jaewa.commandchain.Context;
import java.util.function.Predicate;

public class IfExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<LoopExecutorBuilder<P>, P> {

    private final CommandExecutor mainExecutor;

    private CommandExecutor currentExecutor;

    public IfExecutorBuilder(Predicate<Context> condition, P parentBuilder) {
        super(parentBuilder);
        mainExecutor = new CommandExecutor();
    }

    private void addBranch(String name, Predicate<Context> condition) {
        currentExecutor = new CommandExecutor();
        mainExecutor.add(name, Commands.conditional(condition, currentExecutor));
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return currentExecutor;
    }

    public AsyncCommand build() {
        return mainExecutor;
    }
}
