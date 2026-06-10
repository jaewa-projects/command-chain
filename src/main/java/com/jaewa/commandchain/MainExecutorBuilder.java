package com.jaewa.commandchain;

public class MainExecutorBuilder extends AbstractExecutorBuilder<MainExecutorBuilder> {
    protected MainExecutorBuilder(Context context) {
        super(context);
    }

    public CommandExecutor build() {
        return getCommandExecutor();
    }
}
