package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Commands;
import com.jaewa.commandchain.Context;
import com.jaewa.commandchain.IfCommand;
import java.util.function.Predicate;

public class IfExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<LoopExecutorBuilder<P>, P> {

    private final IfCommand ifCommand;

    public IfExecutorBuilder(Predicate<Context> condition, P parentBuilder) {
        super(parentBuilder);
        this.ifCommand = new IfCommand();
        ifCommand.when(condition);
    }

    public IfExecutorBuilder<P> when(Predicate<Context> condition) {
        ifCommand.when(condition);
        return this;
    }

    public IfExecutorBuilder<P> otherwise() {
        ifCommand.when(c -> true);
        return this;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return ifCommand.getCurrentCommandExecutor();
    }

    public AsyncCommand build() {
        return ifCommand;
    }
}
