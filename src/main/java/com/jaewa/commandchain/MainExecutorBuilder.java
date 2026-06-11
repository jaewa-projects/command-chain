package com.jaewa.commandchain;

public class MainExecutorBuilder extends AbstractExecutorBuilder<MainExecutorBuilder> {
    protected MainExecutorBuilder(Context context) {
        super(context);
    }

    /**
     * Builds and returns a {@link CommandExecutor} instance.
     * This method finalizes the builder configuration and provides
     * the constructed {@code CommandExecutor}, which can be used to
     * execute a chain of asynchronous commands.
     *
     * @return an instance of {@link CommandExecutor} configured by this builder
     */
    public CommandExecutor build() {
        return getCommandExecutor();
    }
}
