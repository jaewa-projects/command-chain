package com.jaewa.commandchain;

public interface FailureCommand {
    void execute(Throwable e);
}
