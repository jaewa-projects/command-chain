package com.jaewa.commandchain;

public interface CommandBlock extends AsyncCommand {
    CommandExecutor getBlockCommandExecutor();
}
