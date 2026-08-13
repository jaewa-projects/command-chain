package com.jaewa.commandchain;

/**
 * A block of commands that can be executed as a single unit within a chain.
 * It provides access to the {@link CommandExecutor} used to manage the commands
 * within the block.
 */
public interface CommandBlock extends AsyncCommand {
    /**
     * Retrieves the {@link CommandExecutor} associated with this block.
     *
     * @return the command executor for the block
     */
    CommandExecutor getBlockCommandExecutor();
}
