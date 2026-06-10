package com.jaewa.commandchain;

public class LoopExecutorBuilder<R extends AbstractExecutorBuilder<?>> extends AbstractSubBuilder<LoopExecutorBuilder<R>, R>{
    public LoopExecutorBuilder(Context ctx, R returnBuilder) {
        super(ctx, returnBuilder);
    }
}
