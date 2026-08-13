package com.jaewa.commandchain;

/**
 * Abstract base class for loop implementations.
 * It manages the loop executor and provides the framework for iterative execution.
 */
public abstract class AbstractLoop implements Loop, CommandBlock {
    private final String varName;
    /**
     * The executor used to run the commands within the loop.
     */
    protected CommandExecutor loopExecutor;

    /**
     * Creates a new AbstractLoop.
     *
     * @param varName the name of the variable to store this loop instance in the context
     */
    protected AbstractLoop(String varName) {
        this.varName = varName;
        loopExecutor = new CommandExecutor();
    }

    @Override
    public final void init(Context ctx) {
        if (varName != null) {
            ctx.set(varName, this);
        }
        init();
    }

    /**
     * Initializes the loop state.
     */
    protected abstract void init();

    @Override
    public void add(AsyncCommand cmd) {
        loopExecutor.add(cmd);
    }

    @Override
    public void execute(Context ctx, CommandChain chain) {
        init(ctx);
        doIteration(ctx, chain);
    }

    private void doIteration(Context ctx, CommandChain chain) {
        if (hasNext()) {
            loopExecutor.start(ctx)
                    .thenRun(() -> {
                        next();
                        doIteration(ctx, chain);
                    })
                    .exceptionally(e -> {
                        chain.fail(e);
                        return null;
                    });
        } else {
            chain.next();
        }
    }

    @Override
    public CommandExecutor getBlockCommandExecutor() {
        return loopExecutor;
    }
}
