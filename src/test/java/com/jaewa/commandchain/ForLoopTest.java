package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ForLoopTest {

    @Mock
    private AsyncCommand innerCommand;

    @Test
    void testForLoop() throws Exception {
        MainExecutorBuilder builder = CommandExecutor.pipelineBuilder();
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
                .loop(forLoop)
                    .exec(innerCommand)
                .end()
                .build();

        Context context = new DefaultContext();
        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(innerCommand, times(3)).execute(any(Context.class), any());
        
        InOrder inOrder = inOrder(innerCommand);
        inOrder.verify(innerCommand, times(3)).execute(any(Context.class), any());
        
        assertEquals(3, forLoop.getValue());
    }

    @Test
    void testForLoopWithInnerFor() throws Exception {
        MainExecutorBuilder builder = CommandExecutor.pipelineBuilder();

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
                .loop(outerLoop)
                    .loop(innterLoop)
                        .exec(innerCommand)
                    .end()
                .end()
                .build();

        Context context = new DefaultContext();
        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(innerCommand, times(6)).execute(any(Context.class), any());

    }
}
