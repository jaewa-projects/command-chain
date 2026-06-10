package com.jaewa.commandchain;

public interface FailureHandler {
    void execute(Throwable e);
}
