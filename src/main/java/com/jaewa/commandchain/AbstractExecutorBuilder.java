package com.jaewa.commandchain;

public abstract class AbstractExecutorBuilder<B extends AbstractExecutorBuilder<?>> {

    private final CommandExecutor commandExecutor;

    private final Context context;

    protected AbstractExecutorBuilder(Context context) {
        this.context = context;
        commandExecutor = new CommandExecutor(context);
    }

    @SuppressWarnings("unchecked")
    private B getThis(){
        return (B)this;
    }

    public B exec(String name, Command cmd) {
        commandExecutor.add(name, Commands.async(cmd));
        return getThis();
    }

    public B exec(String name, AsyncCommand cmd) {
        commandExecutor.add(name, cmd);
        return getThis();
    }

    public B onFailure(FailureHandler cmd) {
        commandExecutor.setFailureHandler(Commands.async(cmd));
        return getThis();
    }

    public B onFailure(AsyncFailureHandler cmd) {
        commandExecutor.setFailureHandler(cmd);
        return getThis();
    }

    public LoopExecutorBuilder<B> loop(String name, Loop loop){
        LoopExecutorBuilder<B> result = new LoopExecutorBuilder<>(loop, context, getThis());
        commandExecutor.add(name, result.build());
        return result;
    }

    protected CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }
}
