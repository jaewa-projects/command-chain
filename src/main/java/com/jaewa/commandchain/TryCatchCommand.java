package com.jaewa.commandchain;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class TryCatchCommand implements CommandBlock{

    private CommandExecutor tryBlock;
    private List<Pair<Exception, CommandExecutor>> catchBlocks;

    @Override
    public CommandExecutor getBlockCommandExecutor() {
        return null;
    }

    @Override
    public void execute(Context ctx, CommandChain chain) {

    }
}
