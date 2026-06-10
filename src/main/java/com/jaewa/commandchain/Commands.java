package com.jaewa.commandchain;

import java.awt.EventQueue;
import javax.swing.SwingUtilities;
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

    private static class AsyncFailureCommandAdapter implements AsyncFailureCommand {
        private final FailureCommand cmd;

        public AsyncFailureCommandAdapter(FailureCommand cmd) {
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

    private static class EventQueueCommand implements AsyncCommand {
        private final AsyncCommand cmd;

        public EventQueueCommand(AsyncCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            if (EventQueue.isDispatchThread()) {
                executeImpl(ctx, chain);
            } else {
                SwingUtilities.invokeLater(() -> executeImpl(ctx, chain));
            }
        }

        private void executeImpl(Context ctx, CommandChain chain) {
            cmd.execute(ctx, chain);
        }
    }

    private static class EventQueueFailureCommand implements AsyncFailureCommand {
        private final AsyncFailureCommand cmd;

        public EventQueueFailureCommand(AsyncFailureCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Throwable e, CommandChain chain) {
            if (EventQueue.isDispatchThread()) {
                executeImpl(e, chain);
            } else {
                SwingUtilities.invokeLater(() -> {
                    executeImpl(e, chain);
                });
            }
        }

        private void executeImpl(Throwable e, CommandChain chain) {
            cmd.execute(e, chain);
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
        private final AsyncCommand cmd;

        public WireTapCommand(AsyncCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public void execute(Context ctx, CommandChain chain) {
            cmd.execute(ctx, new CommandChain() {
                @Override
                public void next() {

                }

                @Override
                public void fail(Throwable e) {
                    log.error("wiretap failed", e);
                }
            });
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

    private Commands() {

    }

    public static AsyncCommand async(Command cmd) {
        return new AsyncCommandAdapter(cmd);
    }

    public static AsyncCommand onEventQueue(AsyncCommand cmd) {
        return new EventQueueCommand(cmd);
    }

        public static AsyncCommand onEventQueue(Command cmd) {
            return new EventQueueCommand(async(cmd));
        }

        public static AsyncCommand wireTap(AsyncCommand cmd) {
            return new WireTapCommand(cmd);
        }

        public static AsyncCommand wireTap(Command cmd) {
            return new WireTapCommand(async(cmd));
        }

        public static AsyncFailureCommand async(FailureCommand cmd) {
        return new AsyncFailureCommandAdapter(cmd);
    }

    public static AsyncFailureCommand onEventQueue(FailureCommand cmd) {
        return new EventQueueFailureCommand(async(cmd));
    }

    public static AsyncFailureCommand onEventQueue(AsyncFailureCommand cmd) {
        return new EventQueueFailureCommand(cmd);
    }

    public static AsyncCommand loop(CommandExecutor cmd, Loop loop) {
        return new LoopCommandDecorator(cmd, loop);
    }

    public static AsyncCommand interruptible(AsyncCommand cmd) {
        return new InterruptibleCommand(cmd);
    }

}
