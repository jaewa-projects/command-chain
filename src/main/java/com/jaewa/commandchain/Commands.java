package com.jaewa.commandchain;

import com.jaewa.commandchain.service.ExecutorService;
import java.awt.EventQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;


/**
 * <h2>Commands</h2>
 * <p>
 * Utility class that provides decorator methods for wrapping and enhancing commands.
 * This class does not directly generate commands but instead provides decorators that
 * add capabilities such as asynchronous execution, event queue dispatch, interruption
 * handling, and error safety to existing commands.
 * </p>
 * <p>
 * The decorators enable the composition of enhanced command behaviors into command chains
 * and provide helper methods to handle thread context and error propagation.
 * </p>
 * <p>
 * This class cannot be instantiated and only provides static methods.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Wraps <code>Command</code> instances into <code>AsyncCommand</code> for asynchronous execution.</li>
 *   <li>Wraps <code>Runnable</code> instances into <code>AsyncCommand</code> for integration into command chains.</li>
 *   <li>Ensures commands and failure handlers execute on the AWT Event Dispatch Thread.</li>
 *   <li>Wraps <code>FailureHandler</code> instances into <code>AsyncFailureHandler</code> for asynchronous error handling.</li>
 *   <li>Provides internal decorators for error safety and interruption handling.</li>
 * </ul>
 */
public class Commands {

    private Commands() {

    }

    /**
     * Creates an asynchronous wrapper around a given <code>Command</code> instance, enabling it
     * to participate as an <code>AsyncCommand</code> in an asynchronous command chain.
     * The returned <code>AsyncCommand</code> executes the provided <code>Command</code> and then
     * progresses or terminates the chain based on the execution outcome.
     * If the command executes successfully, the chain's <code>CommandChain.next()</code>
     * method is called to proceed to the next command in the chain. If the command throws
     * an exception, the <code>CommandChain.fail(Throwable)</code> method is called with
     * the exception, failing the chain execution.
     *
     * @param cmd the <code>Command</code> to be adapted and executed as part of an asynchronous chain
     * @return an <code>AsyncCommand</code> that executes the given <code>Command</code> within an
     * asynchronous command chain
     */
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

    /**
     * Creates an {@code AsyncCommand} that wraps the provided {@code CompletableFuture}.
     * The returned asynchronous command participates in the asynchronous command execution chain.
     * Upon completion of the given future, the chain either progresses to the next command or fails,
     * depending on whether the future completed successfully or exceptionally.
     * If the future completes normally, the chain's {@code CommandChain.next()} method is invoked
     * to proceed to the next command. If the future completes with an exception, the chain's
     * {@code CommandChain.fail(Throwable)} method is invoked with the exception.
     *
     * @param future the {@code CompletableFuture} whose completion determines the execution flow
     *               in the asynchronous command chain
     * @return an {@code AsyncCommand} that integrates the given {@code CompletableFuture} into
     * the asynchronous command chain
     */
    public static AsyncCommand async(CompletableFuture<?> future) {
        return (ctx, chain) -> future.whenComplete((v, e) -> {
            if (e != null) {
                chain.fail(e);
            } else {
                chain.next();
            }
        });
    }

    /**
     * Creates an asynchronous wrapper around a given <code>Runnable</code> instance, enabling it
     * to participate as an <code>AsyncCommand</code> in an asynchronous command chain.
     * The returned <code>AsyncCommand</code> executes the provided <code>Runnable</code> and then
     * progresses or terminates the chain based on the execution outcome.
     * The <code>Runnable.run()</code> method is executed within the asynchronous chain execution
     * context. If the execution completes without throwing exceptions, the chain's
     * <code>CommandChain.next()</code> method is invoked to proceed to the next command.
     * If an exception is thrown during execution, the <code>CommandChain.fail(Throwable)</code>
     * method is called with the exception, thereby failing the chain execution.
     *
     * @param runnable the <code>Runnable</code> to be adapted and executed as part of an asynchronous chain
     * @return an <code>AsyncCommand</code> that executes the given <code>Runnable</code> within an
     * asynchronous command chain
     */
    public static AsyncCommand async(Runnable runnable) {
        return async((Command) ctx -> runnable.run());
    }

    /**
     * Ensures that the given <code>AsyncCommand</code> is executed on the AWT Event Dispatch Thread.
     * If the current thread is the Event Dispatch Thread, the command is executed immediately.
     * Otherwise, it schedules the command to be executed asynchronously on the Event Dispatch Thread.
     *
     * @param cmd the <code>AsyncCommand</code> to be executed on the Event Dispatch Thread, ensuring
     *            proper thread safety when interacting with UI components or other event-thread-specific
     *            operations
     * @return a new <code>AsyncCommand</code> that wraps the given <code>AsyncCommand</code> and ensures
     * its execution on the Event Dispatch Thread
     */
    public static AsyncCommand onEventQueue(AsyncCommand cmd) {
        return (ctx, chain) -> {
            if (EventQueue.isDispatchThread()) {
                cmd.execute(ctx, chain);
            } else {
                SwingUtilities.invokeLater(() -> cmd.execute(ctx, chain));
            }
        };
    }

    /**
     * Ensures that the provided <code>AsyncFailureHandler</code> is executed on the AWT Event Dispatch Thread.
     * If the current thread is the Event Dispatch Thread, the handler is executed immediately.
     * Otherwise, the handler is scheduled to execute asynchronously on the Event Dispatch Thread.
     *
     * @param h the <code>AsyncFailureHandler</code> to be executed on the Event Dispatch Thread, ensuring
     *          thread safety when interacting with UI components or other event-thread-specific operations
     * @return a new <code>AsyncFailureHandler</code> that wraps the given <code>AsyncFailureHandler</code> and ensures its
     * execution on the Event Dispatch Thread
     */
    public static AsyncFailureHandler onEventQueue(AsyncFailureHandler h) {
        return (e, chain) -> {
            if (EventQueue.isDispatchThread()) {
                h.execute(e, chain);
            } else {
                SwingUtilities.invokeLater(() -> h.execute(e, chain));
            }
        };
    }

    public static AsyncFailureHandler onEventQueue(FailureHandler h) {
        return onEventQueue(async(h));
    }

    /**
     * Ensures that the given <code>Command</code> is adapted into an <code>AsyncCommand</code>
     * and scheduled to execute on the AWT Event Dispatch Thread.
     * <p>
     * The method converts the provided <code>Command</code> into an asynchronous wrapper
     * using <code>async(Command)</code>, and then guarantees its execution on the
     * Event Dispatch Thread by passing it to the overloaded method <code>onEventQueue(AsyncCommand)</code>.
     * This ensures thread safety for operations that interact with UI components
     * or require event-thread-specific handling.
     *
     * @param cmd the <code>Command</code> to be adapted and executed on the Event Dispatch Thread
     * @return an <code>AsyncCommand</code> that wraps the given <code>Command</code> and
     * ensures its execution on the Event Dispatch Thread
     */
    public static AsyncCommand onEventQueue(Command cmd) {
        return onEventQueue(async(cmd));
    }

    /**
     * Creates an asynchronous wrapper around a given <code>FailureHandler</code> instance, enabling it
     * to participate as an <code>AsyncFailureHandler</code> in an asynchronous command chain.
     * The returned <code>AsyncFailureHandler</code> executes the provided <code>FailureHandler</code> and then
     * progresses or terminates the chain based on the execution outcome.
     * If the failure handler executes successfully, the chain's <code>CommandChain.next()</code>
     * method is called to proceed to the next command in the chain. If the failure handler throws
     * an exception, the <code>CommandChain.fail(Throwable)</code> method is called with the exception,
     * failing the chain execution.
     *
     * @param cmd the <code>FailureHandler</code> to be adapted and executed as part of an asynchronous chain
     * @return an <code>AsyncFailureHandler</code> that executes the given <code>FailureHandler</code> within an
     * asynchronous command chain
     */
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

    /**
     * Creates an asynchronous command that executes the given {@code Runnable} without
     * interrupting the progression of the asynchronous command chain.
     * This method is typically used to "wiretap" the chain, allowing the provided
     * {@code Runnable} to execute in parallel with the chain's flow.
     * The {@code Runnable.run()} method is executed asynchronously, and the chain
     * invocation proceeds immediately regardless of the completion of the {@code Runnable}.
     *
     * @param runnable the {@code Runnable} to be executed alongside the command chain
     * @return an {@code AsyncCommand} that executes the given {@code Runnable} asynchronously
     * without altering the chain's progression
     */
    public static AsyncCommand wireTap(Runnable runnable) {
        return (ctx, chain) -> {
            ExecutorService.execute(runnable);
            chain.next();
        };
    }

    public static AsyncCommand conditional(Predicate<Context> condition, AsyncCommand trueCommand) {
        return conditional(condition, trueCommand, (ctx, chain) -> chain.next());
    }

    public static AsyncCommand conditional(Predicate<Context> condition, AsyncCommand trueCommand, AsyncCommand falseCommand) {
        return (ctx, chain) -> {
            if (condition.test(ctx)) {
                trueCommand.execute(ctx, chain);
            } else {
                falseCommand.execute(ctx, chain);
            }
        };
    }

    /**
     * Creates a new {@code AsyncCommand} with the specified name and delegate command.
     *
     * @param name the name to associate with the command. This will be used as the command's {@code toString()} representation.
     * @param cmd  the {@code AsyncCommand} to be executed when the resulting command is invoked.
     * @return a new {@code AsyncCommand} that delegates execution to the provided {@code cmd}
     *         and overrides {@code toString()} to return the specified name.
     */
    public static AsyncCommand named(String name, AsyncCommand cmd) {
        return new AsyncCommand() {
            @Override
            public void execute(Context ctx, CommandChain chain) {
                cmd.execute(ctx, chain);
            }

            @Override
            public String toString() {
                return StringUtils.isNotBlank(name) ? name : cmd.toString();
            }
        };
    }

    /**
     * Creates and returns a new Command instance with a customized name.
     * The returned Command execution delegates to the provided Command's execute method,
     * while overriding its toString method to return the specified name.
     *
     * @param name the name to associate with the new Command
     * @param cmd  the original Command whose execute behavior will be used
     * @return a new Command instance with the provided name and delegated behavior
     */
    public static Command named(String name, Command cmd){
        return new Command() {
            @Override
            public void execute(Context ctx) throws Exception {
                cmd.execute(ctx);
            }

            @Override
            public String toString() {
                return StringUtils.isNotBlank(name) ? name : cmd.toString();
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

}
