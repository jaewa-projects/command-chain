package com.jaewa.commandchain;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a command block that simulates a try-catch-finally control flow structure.
 * This class enables executing a series of asynchronous commands and handling exceptions
 * as well as final clean-up tasks within a controlled execution flow.
 * The TryCatchCommand supports the following:
 * - Defining a "try" block using a default executor.
 * - Adding multiple "catch" blocks, each associated with a specific exception type.
 * - Defining an optional "finally" block that executes after the try or catch phase.
 */
public class TryCatchCommand implements CommandBlock {

    private final CommandExecutor tryExecutor;
    private final Map<Class<? extends Throwable>, CommandExecutor> catchExecutors;
    private CommandExecutor finallyExecutor;

    private CommandExecutor currentExecutor;

    /**
     * Creates a new TryCatchCommand.
     */
    public TryCatchCommand() {
        tryExecutor = new CommandExecutor();
        catchExecutors = new HashMap<>();
        this.currentExecutor = tryExecutor;
    }

    /**
     * Adds an asynchronous command to the current block.
     *
     * @param cmb the asynchronous command to be added
     */
    public void add(AsyncCommand cmb) {
        currentExecutor.add(cmb);
    }

    /**
     * Adds a catch block for the specified exception type.
     *
     * @param exceptionType the exception type to catch
     */
    public void doCatch(Class<? extends Throwable> exceptionType) {
        if (catchExecutors.containsKey(exceptionType)) {
            throw new IllegalArgumentException("Catch block already exists for exception type: " + exceptionType);
        }

        CommandExecutor catchBlock = new CommandExecutor();
        catchExecutors.put(exceptionType, catchBlock);
        this.currentExecutor = catchBlock;
    }

    /**
     * Adds a finally block.
     */
    public void doFinally() {
        if(this.finallyExecutor != null){
            throw new IllegalArgumentException("Finally block already exists");
        }
        finallyExecutor = new CommandExecutor();
        currentExecutor = finallyExecutor;
    }

    @Override
    public CommandExecutor getBlockCommandExecutor() {
        return currentExecutor;
    }


    @Override
    public void execute(Context ctx, CommandChain chain) {
        tryExecutor.start(ctx).whenComplete((r, t) -> handleTryComplete(t, ctx, chain));
    }

    /**
     * Handles the completion of the try block.
     *
     * @param t     the exception thrown during try execution, or null if successful
     * @param ctx   the execution context
     * @param chain the command chain
     */
    public void handleTryComplete(Throwable t, Context ctx, CommandChain chain) {
        if (t != null) {
            handleFailure(t, ctx, chain);
        } else {
            handleFinally(null, ctx, chain);
        }
    }

    private void handleFailure(Throwable t, Context ctx, CommandChain chain) {
        CommandExecutor catchExecutor = findCatchExecutor(t);
        if (catchExecutor != null) {
            catchExecutor.start(ctx)
                    //t is absorbed by catchExecutor
                    .whenComplete((r, tx) -> handleFinally(tx, ctx, chain));
        } else {
            handleFinally(t, ctx, chain);
        }
    }

    private void handleFinally(Throwable previousFailure, Context ctx, CommandChain chain) {
        if (finallyExecutor != null) {
            finallyExecutor.start(ctx)
                    .whenComplete((r, t) -> {
                        Throwable propagatedFailure = t != null ? t : previousFailure;
                        if (propagatedFailure != null) {
                            chain.fail(propagatedFailure);
                        } else {
                            chain.next();
                        }
                    });
        } else {
            if (previousFailure != null) {
                chain.fail(previousFailure);
            } else {
                chain.next();
            }
        }
    }

    private CommandExecutor findCatchExecutor(Throwable t) {
        for (Class<?> clazz = t.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            CommandExecutor commandExecutor = catchExecutors.get(clazz);
            if (commandExecutor != null) {
                return commandExecutor;
            }
        }
        return null;
    }

}
