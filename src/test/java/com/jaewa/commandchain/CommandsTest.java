package com.jaewa.commandchain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandsTest {

    @Mock
    private Context context;

    @Mock
    private CommandChain chain;

    @Mock
    private Command mockCommand;

    @Mock
    private AsyncCommand mockAsyncCommand;

    @Mock
    private FailureHandler mockFailureHandler;

    @Test
    void testAsyncCommandAdapter() throws Exception {
        AsyncCommand async = Commands.async(mockCommand);
        async.execute(context, chain);
        
        verify(mockCommand).execute(context);
        verify(chain).next();
    }

    @Test
    void testAsyncCommandAdapterFailure() throws Exception {
        Exception ex = new RuntimeException("fail");
        doThrow(ex).when(mockCommand).execute(context);
        
        AsyncCommand async = Commands.async(mockCommand);
        async.execute(context, chain);
        
        verify(chain).fail(ex);
    }

    @Test
    void testAsyncFailureCommandAdapter() {
        Exception ex = new RuntimeException("original fail");
        AsyncFailureHandler async = Commands.async(mockFailureHandler);
        async.execute(ex, chain);
        
        verify(mockFailureHandler).execute(ex);
        verify(chain).next();
    }

    @Test
    void testAsyncFailureCommandAdapterFailure() {
        Exception ex = new RuntimeException("original fail");
        Exception exInHandler = new RuntimeException("handler fail");
        doThrow(exInHandler).when(mockFailureHandler).execute(ex);
        
        AsyncFailureHandler async = Commands.async(mockFailureHandler);
        async.execute(ex, chain);
        
        verify(chain).fail(exInHandler);
    }

    @Test
    void testWireTapCommand() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable runnable = () -> executed.set(true);
        
        AsyncCommand wireTap = Commands.wireTap(runnable);
        wireTap.execute(context, chain);
        
        // Aspettiamo un attimo poiché ExecutorService.execute potrebbe essere asincrono
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(executed.get());
        verify(chain).next();
    }

    @Test
    void testInterruptibleCommand() {
        when(context.isInterrupted()).thenReturn(true);

        AsyncCommand interruptible = Commands.interruptible(mockAsyncCommand);
        interruptible.execute(context, chain);

        verify(chain).fail(any(CommandInterruptedException.class));
        verify(mockAsyncCommand, never()).execute(any(), any());
    }

    @Test
    void testInterruptibleCommandNotInterrupted() {
        when(context.isInterrupted()).thenReturn(false);

        AsyncCommand interruptible = Commands.interruptible(mockAsyncCommand);
        interruptible.execute(context, chain);

        verify(mockAsyncCommand).execute(context, chain);
    }

    @Test
    void testSafeCommand() {
        RuntimeException ex = new RuntimeException("error");
        doThrow(ex).when(mockAsyncCommand).execute(any(), any());

        AsyncCommand safe = Commands.safe(mockAsyncCommand);
        safe.execute(context, chain);

        verify(chain).fail(ex);
    }

}
