package com.jaewa.commandchain;

/**
 * An abstract builder class that extends {@link AbstractExecutorBuilder} and provides a fluent API
 * for defining and configuring ongoing builder operations. This class is used as a base for builders
 * that need to construct specific execution flows while allowing a reference to a parent builder for
 * hierarchical configurations.
 *
 * @param <B> the type of the concrete builder extending this class
 * @param <P> the type of the parent builder
 */
public abstract class AbstractOngoingBuilder<B extends AbstractExecutorBuilder<?>, P extends AbstractExecutorBuilder<?>> extends AbstractExecutorBuilder<B> {

    private final P parentBuilder;

    protected AbstractOngoingBuilder(Context ctx, P parentBuilder) {
        super(ctx);
        this.parentBuilder = parentBuilder;
    }

    /**
     * Ends the configuration of the current {@code AbstractOngoingBuilder} and returns
     * the parent builder. This allows the fluent API to return to the context of the
     * parent builder for further configurations.
     *
     * @return the parent builder associated with the current ongoing builder
     */
    public P end() {
        return parentBuilder;
    }
}
