package com.jaewa.commandchain;

public interface AsyncCommand {
    void execute(Context ctx, CommandChain chain);
}
