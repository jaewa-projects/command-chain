package com.jaewa.commandchain;

public abstract class AbstractOngoingBuilder<B extends AbstractExecutorBuilder<?>, P extends AbstractExecutorBuilder<?>> extends AbstractExecutorBuilder<B> {

    private final P parentBuilder;

    public AbstractOngoingBuilder(Context ctx, P parentBuilder) {
        super(ctx);
        this.parentBuilder = parentBuilder;
    }

    public P end() {
        return parentBuilder;
    }
}
