package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AbstractLoop;
import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandExecutor;

public class LoopExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<LoopExecutorBuilder<P>, P> {

    private final AbstractLoop loop;

    public LoopExecutorBuilder(AbstractLoop loop, P parentBuilder) {
        super(parentBuilder);
        this.loop = loop;
    }

    AsyncCommand build() {
        return loop;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return loop.getLoopExecutor();
    }
}
