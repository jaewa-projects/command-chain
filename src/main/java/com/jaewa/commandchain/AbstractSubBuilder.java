package com.jaewa.commandchain;

public abstract class AbstractSubBuilder<B extends AbstractExecutorBuilder<?>, R extends AbstractExecutorBuilder<?>> extends AbstractExecutorBuilder<B> {

    private final R returnBuilder;

    public AbstractSubBuilder(Context ctx, R returnBuilder) {
        super(ctx);
        this.returnBuilder = returnBuilder;
    }

    public R end() {
        return returnBuilder;
    }
}
