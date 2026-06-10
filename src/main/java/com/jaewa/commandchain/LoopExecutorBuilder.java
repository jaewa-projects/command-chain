package com.jaewa.commandchain;

public class LoopExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<LoopExecutorBuilder<P>, P> {
    public LoopExecutorBuilder(Context ctx, P parentBuilder) {
        super(ctx, parentBuilder);
    }
}
