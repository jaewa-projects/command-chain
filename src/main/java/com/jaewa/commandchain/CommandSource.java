package com.jaewa.commandchain;

import org.apache.commons.lang3.tuple.Pair;

public interface CommandSource {
    void add(String commandName, AsyncCommand command);
    void init();

    Pair<String, AsyncCommand> next();
}
