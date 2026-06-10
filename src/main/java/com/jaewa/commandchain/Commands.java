package com.jaewa.commandchain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Commands {
    private static class AsyncCommandAdapter implements AsyncCommand {
        private final Command cmd;

        public AsyncCommandAdapter(Command cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            try {
                cmd.execute(ctx);
                chain.next();
            } catch (Exception e) {
                chain.fail(e);
            }
        }
    }

    private static class AsyncFailureCommandAdapter implements AsyncFailureHandler {
        private final FailureHandler cmd;

        public AsyncFailureCommandAdapter(FailureHandler cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Throwable e, CommandChain chain) {
            try {
                cmd.execute(e);
                chain.next();
            } catch (Exception ex) {
                chain.fail(ex);
            }
        }
    }

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

    private static class WireTapCommand implements AsyncCommand {
        private final Runnable runnable;

        public WireTapCommand(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            ExecutorService.execute(runnable);
            chain.next();
        }
    }


    private static class InterruptibleCommand implements AsyncCommand {
        private final AsyncCommand cmd;

        public InterruptibleCommand(AsyncCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            if (ctx.isInterrupted()) {
                chain.fail(new CommandInterruptedException());
            } else {
                cmd.execute(ctx, chain);
            }
        }
    }

    private static class SafeCommand implements AsyncCommand {
        private final AsyncCommand cmd;

        public SafeCommand(AsyncCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            try {
                cmd.execute(ctx, chain);
            } catch (Exception e) {
                chain.fail(e);
            }
        }
    }

    private Commands() {

    }

    public static AsyncCommand async(Command cmd) {
        return new AsyncCommandAdapter(cmd);
    }

    public static AsyncCommand wireTap(Runnable runnable) {
        return new WireTapCommand(runnable);
    }

    public static AsyncFailureHandler async(FailureHandler cmd) {
        return new AsyncFailureCommandAdapter(cmd);
    }

    static AsyncCommand loop(CommandExecutor cmd, Loop loop) {
        return new LoopCommandDecorator(cmd, loop);
    }

    static AsyncCommand safe(AsyncCommand cmd) {
        return new SafeCommand(cmd);
    }

    static AsyncCommand interruptible(AsyncCommand cmd) {
        return new InterruptibleCommand(cmd);
    }

}
