package com.jaewa.commandchain;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        await().atMost(1, TimeUnit.SECONDS).untilTrue(executed);
        verify(chain).next();
    }

    @Test
    void testSafeCommand() {
        RuntimeException ex = new RuntimeException("error");
        doThrow(ex).when(mockAsyncCommand).execute(any(), any());

        AsyncCommand safe = Commands.safe(mockAsyncCommand);
        safe.execute(context, chain);

        verify(chain).fail(ex);
    }

    @Test
    void testAsyncCompletableFutureSuccess() {
        CompletableFuture<String> future = new CompletableFuture<>();
        AsyncCommand async = Commands.async(future);
        async.execute(context, chain);

        future.complete("success");

        verify(chain).next();
    }

    @Test
    void testAsyncCompletableFutureFailure() {
        CompletableFuture<String> future = new CompletableFuture<>();
        AsyncCommand async = Commands.async(future);
        async.execute(context, chain);

        Exception ex = new RuntimeException("fail");
        future.completeExceptionally(ex);

        verify(chain).fail(any());
    }

    @Test
    void testAsyncRunnable() {
        AtomicBoolean run = new AtomicBoolean(false);
        Runnable runnable = () -> run.set(true);
        AsyncCommand async = Commands.async(runnable);
        async.execute(context, chain);

        assertTrue(run.get());
        verify(chain).next();
    }

    @Test
    void testOnEventQueueAsyncCommand() throws Exception {
        AsyncCommand onEvent = Commands.onEventQueue(mockAsyncCommand);

        // Test when not in EDT
        onEvent.execute(context, chain);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> verify(mockAsyncCommand).execute(context, chain));

        // Test when in EDT
        SwingUtilities.invokeAndWait(() -> onEvent.execute(context, chain));
        verify(mockAsyncCommand, org.mockito.Mockito.atLeast(2)).execute(context, chain);
    }

    @Test
    void testOnEventQueueAsyncFailureHandler() throws Exception {
        AsyncFailureHandler mockAsyncHandler = mock(AsyncFailureHandler.class);
        AsyncFailureHandler onEvent = Commands.onEventQueue(mockAsyncHandler);
        Exception ex = new RuntimeException("fail");

        // Test when not in EDT
        onEvent.execute(ex, chain);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> verify(mockAsyncHandler).execute(ex, chain));

        // Test when in EDT
        SwingUtilities.invokeAndWait(() -> onEvent.execute(ex, chain));
        verify(mockAsyncHandler, org.mockito.Mockito.atLeast(2)).execute(ex, chain);
    }

    @Test
    void testOnEventQueueFailureHandler() {
        FailureHandler handler = mock(FailureHandler.class);
        AsyncFailureHandler onEvent = Commands.onEventQueue(handler);
        Exception ex = new RuntimeException("fail");

        onEvent.execute(ex, chain);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> verify(handler).execute(ex));
        verify(chain).next();
    }

    @Test
    void testOnEventQueueCommand() {
        Command cmd = mock(Command.class);
        AsyncCommand onEvent = Commands.onEventQueue(cmd);

        onEvent.execute(context, chain);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> verify(cmd).execute(context));
        verify(chain).next();
    }

    @Test
    void testConditionalTrue() {
        AsyncCommand trueCommand = mock(AsyncCommand.class);
        AsyncCommand falseCommand = mock(AsyncCommand.class);
        
        AsyncCommand conditional = Commands.conditional(ctx -> true, trueCommand, falseCommand);
        conditional.execute(context, chain);
        
        verify(trueCommand).execute(context, chain);
        verify(falseCommand, never()).execute(any(), any());
    }

    @Test
    void testConditionalFalse() {
        AsyncCommand trueCommand = mock(AsyncCommand.class);
        AsyncCommand falseCommand = mock(AsyncCommand.class);
        
        AsyncCommand conditional = Commands.conditional(ctx -> false, trueCommand, falseCommand);
        conditional.execute(context, chain);
        
        verify(falseCommand).execute(context, chain);
        verify(trueCommand, never()).execute(any(), any());
    }

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<Commands> constructor = Commands.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Commands instance = constructor.newInstance();
        assertNotNull(instance);
    }

}
