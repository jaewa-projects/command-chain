package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.ChoiceCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Context;
import java.util.function.Predicate;

public class ChoiceExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<CommandBlockBuilder<P>, P> {

    private final ChoiceCommand choiceCommand;

    public ChoiceExecutorBuilder(P parentBuilder) {
        super(parentBuilder);
        choiceCommand = new ChoiceCommand();
    }

    public CommandBlockBuilder<ChoiceExecutorBuilder<P>> when(Predicate<Context> condition) {
        choiceCommand.when(condition);
        return new CommandBlockBuilder<>(choiceCommand, this);
    }

    public CommandBlockBuilder<ChoiceExecutorBuilder<P>> otherwise() {
        choiceCommand.when(c -> true);
        return new CommandBlockBuilder<>(choiceCommand, this);
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return choiceCommand.getBlockCommandExecutor();
    }

    public AsyncCommand build() {
        return choiceCommand;
    }
}
