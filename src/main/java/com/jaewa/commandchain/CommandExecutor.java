package com.jaewa.commandchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

import static com.jaewa.commandchain.Commands.interruptible;

@Slf4j
public class CommandExecutor implements CommandChain, AsyncCommand {

    private final List<ImmutablePair<String, AsyncCommand>> commands;

    @Setter
    private AsyncFailureCommand failureCommand;

    private int executionIndex = -1;

    private Throwable failure = null;

    private CompletableFuture<Void> future;

    private static Executor executor = Executors.newCachedThreadPool();

    @Getter
    private final Context context;

    public static MainExecutorBuilder builder() {
        return new MainExecutorBuilder(new ContextImpl());
    }
    
    CommandExecutor(Context context) {
        this.commands = new ArrayList<>();
        this.context = context;
    }

    public void add(String name, AsyncCommand cmd) {
        commands.add(ImmutablePair.of(name, cmd));
    }

    public CompletableFuture<Void> start() {
        future = new CompletableFuture<>();
        executionIndex = -1;
        failure = null;
        next();
        return future;
    }

    @Override
    public void next() {
        if (failure != null) {
            future.completeExceptionally(failure);
            return;
        }
        executionIndex++;
        if (executionIndex < commands.size()) {
            ImmutablePair<String, AsyncCommand> cmdPair = commands.get(executionIndex);
            log.info("Executing command: {}", cmdPair.left);
            executeCommandAsync(cmdPair.left, cmdPair.right);
        } else {
            future.complete(null);
        }
    }

    @Override
    public void fail(Throwable e) {
        if (failure != null) {
            log.error("Command chain already failed", e);
            future.completeExceptionally(failure);
        } else {
            failure = e;
            if (failureCommand != null) {
                failureCommand.execute(e, this);
            } else {
                future.completeExceptionally(e);
            }
        }
    }

    private void executeCommandAsync(String name, AsyncCommand cmd) {
        executor.execute(() -> {
            interruptible(cmd).execute(context, this);
            log.debug("Command {} executed", name);
        });
    }

    @Override
    public void execute(Context ctx, CommandChain chain) {
        start().thenRun(chain::next)
                .exceptionally(e -> {
                    fail(e.getCause() != null ? e.getCause() : e);
                    return null;
                });
    }

    public void interrupt() {
        context.interrupt();
    }

}
