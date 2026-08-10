package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.TryCatchCommand;

public class TryCatchFinallyBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<TryCatchFinallyBuilder<P>, P>{

    private final TryCatchCommand tryCatchCommand;

    protected TryCatchFinallyBuilder(P parentBuilder) {
        super(parentBuilder);
        tryCatchCommand = new TryCatchCommand();
    }

    public TryCatchFinallyBuilder<P> doCatch(Class<? extends Throwable> exceptionType) {
        tryCatchCommand.doCatch(exceptionType);
        return this;
    }

    public TryCatchFinallyBuilder<P> doFinally() {
        tryCatchCommand.doFinally();
        return this;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return tryCatchCommand.getBlockCommandExecutor();
    }

    AsyncCommand build() {
        return tryCatchCommand;
    }
}
