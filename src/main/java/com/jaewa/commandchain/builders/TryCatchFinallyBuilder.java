package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.TryCatchCommand;

/**
 * Builder for try-catch-finally execution flows.
 *
 * @param <P> the type of the parent builder
 */
public class TryCatchFinallyBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<TryCatchFinallyBuilder<P>, P>{

    private final TryCatchCommand tryCatchCommand;

    /**
     * Creates a new TryCatchFinallyBuilder.
     *
     * @param parentBuilder the parent builder
     */
    protected TryCatchFinallyBuilder(P parentBuilder) {
        super(parentBuilder);
        tryCatchCommand = new TryCatchCommand();
    }

    /**
     * Adds a catch block for the specified exception type.
     *
     * @param exceptionType the type of exception to catch
     * @return this builder for further configuration
     */
    public TryCatchFinallyBuilder<P> doCatch(Class<? extends Throwable> exceptionType) {
        tryCatchCommand.doCatch(exceptionType);
        return this;
    }

    /**
     * Signals the start of the finally block configuration.
     *
     * @return this builder for further configuration
     */
    public TryCatchFinallyBuilder<P> doFinally() {
        tryCatchCommand.doFinally();
        return this;
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return tryCatchCommand.getBlockCommandExecutor();
    }

    /**
     * Builds the try-catch-finally command.
     *
     * @return the constructed {@link AsyncCommand}
     */
    AsyncCommand build() {
        return tryCatchCommand;
    }
}
