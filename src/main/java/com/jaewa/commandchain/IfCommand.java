package com.jaewa.commandchain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

public class IfCommand implements CommandBlock {
    private CommandExecutor currentExecutor;
    private final List<Pair<Predicate<Context>, CommandExecutor>> branches;

    public IfCommand() {
        branches = new ArrayList<>();
    }

    public void when(Predicate<Context> condition) {
        currentExecutor = new CommandExecutor();
        branches.add(Pair.of(condition, currentExecutor));
    }

    public void add(String name, AsyncCommand cmd) {
        if (currentExecutor == null) {
            throw new IllegalStateException("IfCommand is not initialized");
        }
        currentExecutor.add(name, cmd);
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
