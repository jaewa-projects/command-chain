package com.jaewa.commandchain;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class CommandPipeline implements CommandSource {

    private final List<ImmutablePair<String, AsyncCommand>> commands;

    private int executionIndex = -1;

    public CommandPipeline() {
        this.commands = new ArrayList<>();
    }

    @Override
    public void add(String commandName, AsyncCommand command) {
        this.commands.add(new ImmutablePair<>(commandName, command));
    }

    @Override
    public void init() {
        executionIndex = -1;
    }

    @Override
    public Pair<String, AsyncCommand> next() {
        if (executionIndex < commands.size() - 1) {
            executionIndex++;
            return commands.get(executionIndex);
        }
        return null;
    }
}
