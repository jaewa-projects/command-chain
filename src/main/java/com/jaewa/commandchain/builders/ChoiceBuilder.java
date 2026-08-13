package com.jaewa.commandchain.builders;

import com.jaewa.commandchain.AsyncCommand;
import com.jaewa.commandchain.ChoiceCommand;
import com.jaewa.commandchain.CommandExecutor;
import com.jaewa.commandchain.Context;
import java.util.function.Predicate;

/**
 * Builder for conditional execution flows.
 *
 * @param <P> the type of the parent builder
 */
public class ChoiceBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<ChoiceBuilder<P>, P> {

    private final ChoiceCommand choiceCommand;

    /**
     * Creates a new ChoiceBuilder.
     *
     * @param parentBuilder the parent builder
     */
    public ChoiceBuilder(P parentBuilder) {
        super(parentBuilder);
        choiceCommand = new ChoiceCommand();
    }

    /**
     * Adds a conditional branch to the choice command.
     *
     * @param condition the condition to evaluate
     * @return a builder to configure the commands for this branch
     */
    public CommandBlockBuilder<ChoiceBuilder<P>> when(Predicate<Context> condition) {
        choiceCommand.when(condition);
        return new CommandBlockBuilder<>(choiceCommand, this);
    }

    /**
     * Adds a default branch to the choice command.
     *
     * @return a builder to configure the commands for the default branch
     */
    public CommandBlockBuilder<ChoiceBuilder<P>> otherwise() {
        choiceCommand.when(c -> true);
        return new CommandBlockBuilder<>(choiceCommand, this);
    }

    @Override
    protected CommandExecutor getCommandExecutor() {
        return choiceCommand.getBlockCommandExecutor();
    }

    /**
     * Builds the choice command.
     *
     * @return the constructed {@link ChoiceCommand}
     */
    public AsyncCommand build() {
        return choiceCommand;
    }
}
