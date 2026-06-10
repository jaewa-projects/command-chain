package com.jaewa.commandchain;

public interface CommandChain {
    void next();
    void fail(Throwable e);
}
