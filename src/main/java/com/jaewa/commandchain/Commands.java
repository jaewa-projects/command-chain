package com.jaewa.commandchain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Commands {

    private static class LoopCommandDecorator implements AsyncCommand {
        private final CommandExecutor cmd;
        private final Loop loop;

        private LoopCommandDecorator(CommandExecutor cmd, Loop loop) {
            this.cmd = cmd;
            this.loop = loop;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            loop.init(ctx);
            doIteration(chain);
        }

        private void doIteration(CommandChain chain) {
            if (loop.hasNext()) {
                cmd.start()
                        .thenRun(() -> {
                            loop.next();
                            doIteration(chain);
                        })
                        .exceptionally(e -> {
                            chain.fail(e);
                            return null;
                        });
            } else {
                chain.next();
            }
        }
    }

    private Commands() {

    }

    public static AsyncCommand async(Command cmd) {
        return (ctx, chain) -> {
            try {
                cmd.execute(ctx);
                chain.next();
            } catch (Exception e) {
                chain.fail(e);
            }
        };
    }

    public static AsyncCommand async(Runnable runnable) {
        return async((Command) ctx -> runnable.run());
    }

    public static AsyncCommand wireTap(Runnable runnable) {
        return (ctx, chain) -> {
            ExecutorService.execute(runnable);
            chain.next();
        };
    }

    public static AsyncFailureHandler async(FailureHandler cmd) {
        return (e, chain) -> {
            try {
                cmd.execute(e);
                chain.next();
            } catch (Exception ex) {
                chain.fail(ex);
            }
        };
    }

    static AsyncCommand loop(CommandExecutor cmd, Loop loop) {
        return new LoopCommandDecorator(cmd, loop);
    }

    static AsyncCommand safe(AsyncCommand cmd) {
        return (ctx, chain) -> {
            try {
                cmd.execute(ctx, chain);
            } catch (Exception e) {
                chain.fail(e);
            }
        };
    }

    static AsyncCommand interruptible(AsyncCommand cmd) {
        return (ctx, chain) -> {
            if (ctx.isInterrupted()) {
                chain.fail(new CommandInterruptedException());
            } else {
                cmd.execute(ctx, chain);
            }
        };
    }

}
