package com.jaewa.commandchain;

import java.awt.EventQueue;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Commands {

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

    public static AsyncCommand onEventQueue(AsyncCommand cmd) {
        return (ctx, chain) -> {
            if (EventQueue.isDispatchThread()) {
                cmd.execute(ctx, chain);
            } else {
                SwingUtilities.invokeLater(() -> cmd.execute(ctx, chain));
            }
        };
    }

    public static AsyncCommand onEventQueue(Command cmd) {
        return onEventQueue(async(cmd));
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
