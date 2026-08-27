package com.jaewa.commandchain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Represents a command block that branches execution based on conditional predicates.
 * Each branch is associated with a condition and a set of commands to be executed if
 * the condition evaluates to true.
 */
public class ChoiceCommand implements CommandBlock {
    private CommandExecutor currentExecutor;
    private final List<Pair<Predicate<Context>, CommandExecutor>> branches;
    private boolean otherwiseDefined = false;

    /**
     * Creates a new ChoiceCommand.
     */
    public ChoiceCommand() {
        branches = new ArrayList<>();
    }

    /**
     * Defines a conditional branch within the command block. The specified predicate determines
     * whether the commands within this branch will be executed based on the given context
     * during runtime.
     *
     * @param condition the predicate to evaluate against the {@link Context}; if the predicate evaluates
     *                  to {@code true}, the associated commands will be executed. Must not be {@code null}.
     */
    public void when(Predicate<Context> condition) {
        currentExecutor = new CommandExecutor();
        branches.add(Pair.of(condition, currentExecutor));
    }

    /**
     * Defines an "otherwise" branch for the command block, which serves as a fallback option
     * if no other branch conditions are met. The "otherwise" branch is guaranteed to execute
     * when all other conditions return {@code false}.
     *
     * This method ensures that there is at most one "otherwise" branch defined for a given
     * command block. Attempting to define multiple "otherwise" branches will result in an
     * {@link IllegalStateException}.
     *
     * @throws IllegalStateException if the "otherwise" branch has already been defined
     */
    public void otherwise() {
        if(!otherwiseDefined) {
            otherwiseDefined = true;
            when(context -> true);
        }else{
            throw new IllegalStateException("Otherwise branch already defined");
        }
    }

    /**
     * Adds an asynchronous command to the current conditional branch being constructed.
     * This command will be executed as part of the branch if its associated condition evaluates to true.
     *
     * @param cmd the asynchronous command to be added. Must not be {@code null}.
     *            The command encapsulates logic intended for execution within the branch.
     * @throws IllegalStateException if no branch has been initialized using the {@code when} method.
     */
    public void add(AsyncCommand cmd) {
        if (currentExecutor == null) {
            throw new IllegalStateException("IfCommand is not initialized");
        }
        currentExecutor.add(cmd);
    }

    @Override
    public void execute(Context ctx, CommandChain chain) {
        for (Pair<Predicate<Context>, CommandExecutor> branch : branches) {
            if (branch.getKey().test(ctx)) {
                branch.getValue().start(ctx)
                        .thenRun(chain::next)
                        .exceptionally(t -> {
                            chain.fail(t);
                            return null;
                        });
                return;
            }
        }
        chain.next();
    }

    @Override
    public CommandExecutor getBlockCommandExecutor() {
        return currentExecutor;
    }
}
