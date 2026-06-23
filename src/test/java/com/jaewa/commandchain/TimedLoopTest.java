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

        // Almeno una iterazione dovrebbe esserci se la durata è positiva
        assertTrue(timedLoop.hasNext());
        timedLoop.next();
        assertEquals(1, timedLoop.getCycle());

        // Aspettiamo che il tempo passi
        await().atMost(duration + 10, TimeUnit.MILLISECONDS).until(() -> timedLoop.getElapsedTime() >= duration);

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

        // Con 200ms di durata e ~50ms per iterazione, ci aspettiamo circa 4 iterazioni
        // (dipende dallo scheduling, ma sicuramente più di zero)
        int finalCount = executionCount.get();
        System.out.println("[DEBUG_LOG] Execution count: " + finalCount);
        assertTrue(finalCount > 0, "Dovrebbe essere stata eseguita almeno un'iterazione");
        assertEquals(finalCount, timedLoop.getCycle());
    }
}
