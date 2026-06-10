package com.jaewa.commandchain;

import java.util.HashMap;
import java.util.Map;

class ContextImpl implements Context {
    private final Map<String, Object> variables = new HashMap<>();
    private boolean interrupted = false;

    @Override
    public void set(String variableName, Object value) {
        variables.put(variableName, value);
    }

    @Override
    public <E> E get(String variableName, Class<E> type) {
        return type.cast(variables.get(variableName));
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    public void interrupt() {
        interrupted = true;
    }

}
