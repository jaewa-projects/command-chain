package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForLoopTest {

    @Mock
    private AsyncCommand innerCommand;

    @Test
    void testForLoop() throws Exception {
        MainExecutorBuilder builder = CommandExecutor.builder();
        AtomicInteger counter = new AtomicInteger(0);
        
        ForLoop<Integer> forLoop = new ForLoop<>(
                "counter",
                () -> 0,
                val -> val < 3,
                val -> val + 1
        );

        doAnswer(invocation -> {
            Context ctx = invocation.getArgument(0);
            CommandChain chain = invocation.getArgument(1);
            
            ForLoop<?> loop = ctx.get("counter", ForLoop.class);
            assertEquals(counter.getAndIncrement(), loop.getValue());

            chain.next();
            return null;
        }).when(innerCommand).execute(any(), any());

        CommandExecutor executor = builder
                .loop("myLoop", forLoop)
                    .exec("innerCmd", innerCommand)
                .end()
                .build();

        Context context = new DefaultContext();
        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(innerCommand, times(3)).execute(eq(context), any());
        
        InOrder inOrder = inOrder(innerCommand);
        inOrder.verify(innerCommand, times(3)).execute(eq(context), any());
        
        assertEquals(3, forLoop.getValue());
    }

    @Test
    void testForLoopWithInnerFor() throws Exception {
        MainExecutorBuilder builder = CommandExecutor.builder();

        ForLoop<Integer> outerLoop = new ForLoop<>(
                "counter",
                () -> 0,
                val -> val < 3,
                val -> val + 1
        );
        ForLoop<Integer> innterLoop = new ForLoop<>(
                "counter",
                () -> 0,
                val -> val < 2,
                val -> val + 1
        );

        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.next();
            return null;
        }).when(innerCommand).execute(any(), any());

        CommandExecutor executor = builder
                .loop("outerLoop", outerLoop)
                    .loop("innerLoop", innterLoop)
                        .exec("innerCmd", innerCommand)
                    .end()
                .end()
                .build();

        Context context = new DefaultContext();
        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(innerCommand, times(6)).execute(eq(context), any());

    }

    @Test
    void testContextConsistency() throws Exception {
        MainExecutorBuilder builder = CommandExecutor.builder();

        final Context[] capturedContexts = new Context[2];

        ForLoop<Integer> forLoop = new ForLoop<>(
                "i",
                () -> 0,
                val -> val < 1,
                val -> val + 1
        );

        CommandExecutor executor = builder.exec("beforeLoop", (ctx, chain) -> {
                    capturedContexts[0] = ctx;
                    chain.next();
                })
                .loop("loop", forLoop)
                .exec("insideLoop", (ctx, chain) -> {
                    capturedContexts[1] = ctx;
                    chain.next();
                })
                .end()
                .build();

        Context context = new DefaultContext();

        executor.start(context).get(5, TimeUnit.SECONDS);

        assertSame(context, capturedContexts[0]);
        assertSame(context, capturedContexts[1]);
    }
}
