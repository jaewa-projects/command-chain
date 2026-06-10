package com.jaewa.commandchain;

public interface AsyncFailureHandler {
    void execute(Throwable e, CommandChain chain);
}
