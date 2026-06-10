package com.jaewa.commandchain;

public abstract class AbstractLoop implements Loop {
    private final String varName;
    protected Context ctx;

    public AbstractLoop(String varName) {
        this.varName = varName;
    }

    @Override
    public final void init(Context ctx) {
        this.ctx = ctx;
        ctx.set(varName, this);
        init();
    }

    protected abstract void init();

}
