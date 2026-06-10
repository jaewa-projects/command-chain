package com.jaewa.commandchain;

public interface AsyncFailureCommand {
    void execute(Throwable e, CommandChain chain);
}
