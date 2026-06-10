package com.jaewa.commandchain;

public interface Context {
    void set(String variableName, Object value);
    <E> E get(String variableName, Class<E> type);
    boolean isInterrupted();
    void interrupt();
}
