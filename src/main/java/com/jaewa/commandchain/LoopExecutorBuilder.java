package com.jaewa.commandchain;

public class LoopExecutorBuilder<P extends AbstractExecutorBuilder<?>> extends AbstractOngoingBuilder<LoopExecutorBuilder<P>, P> {


    private static class LoopCommandAdapter implements AsyncCommand {
        private final CommandExecutor cmd;
        private final Loop loop;

        private LoopCommandAdapter(CommandExecutor cmd, Loop loop) {
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

    private final Loop loop;

    public LoopExecutorBuilder(Loop loop, Context ctx, P parentBuilder) {
        super(ctx, parentBuilder);
        this.loop = loop;
    }

    public AsyncCommand build() {
        return new LoopCommandAdapter(getCommandExecutor(), loop);
    }
}
