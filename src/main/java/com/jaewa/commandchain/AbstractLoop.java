package com.jaewa.commandchain;

public abstract class AbstractLoop implements AsyncCommand, Loop {
    private final String varName;
    protected CommandExecutor loopExecutor;

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

    protected abstract void init();

    @Override
    public void add(String name, AsyncCommand cmd) {
        loopExecutor.add(name, cmd);
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

    public CommandExecutor getLoopExecutor() {
        return loopExecutor;
    }
}
