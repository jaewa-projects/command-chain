package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimedLoopTest {

    @Mock
    private Context context;

    @Test
    void testTimedLoopLogic() {
        long duration = 500; // 500ms
        TimedLoop timedLoop = new TimedLoop("timer", duration);

        timedLoop.init(context);
        verify(context).set("timer", timedLoop);
        
        long start = timedLoop.getStart();
        assertTrue(start <= System.currentTimeMillis());
        assertEquals(0, timedLoop.getCycle());

        assertTrue(timedLoop.hasNext());
        timedLoop.next();
        assertEquals(1, timedLoop.getCycle());

        await().atMost(duration + 1000, TimeUnit.MILLISECONDS).until(() -> timedLoop.getElapsedTime() >= duration);

        assertFalse(timedLoop.hasNext());
        assertTrue(timedLoop.getElapsedTime() >= duration);
    }

    @Test
    void testTimedLoopInChain() throws Exception {
        MainExecutorBuilder builder = new MainExecutorBuilder();

        AtomicInteger executionCount = new AtomicInteger(0);
        long duration = 200; // 200ms

        TimedLoop timedLoop = new TimedLoop("myTimer", duration);

        CommandExecutor executor = builder.loop("timedLoop", timedLoop)
                .exec("increment", (ctx, chain) -> {
                    executionCount.incrementAndGet();
                    chain.next();
                })
                .end()
                .build();

        executor.start(context).get(5, TimeUnit.SECONDS);

        int finalCount = executionCount.get();
        assertTrue(finalCount > 0);
        assertEquals(finalCount, timedLoop.getCycle());
    }
}
