package com.jaewa.commandchain;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


@ExtendWith(MockitoExtension.class)
class CommandExecutorTest {

    private Context context;

    @Mock
    private AsyncCommand command1;

    @Mock
    private AsyncCommand command2;

    @Mock
    private AsyncCommand command3;

    @Mock
    private AsyncFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        context = new DefaultContext();
    }

    @Test
    void testExecutePipeline() throws Exception {

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();


        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testExecuteQueue() throws Exception {

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.queueBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();


        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testFailureWithoutHandler() {
        RuntimeException testException = new RuntimeException("Test error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(testException);
            return null;
        }).when(command1).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .build();

        ExecutionException ee = assertThrows(ExecutionException.class,
                () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));
        assertEquals(testException, ee.getCause());

        InOrder inOrder = inOrder(command1, command2);
        inOrder.verify(command1).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2);
    }

    @Test
    void testFailureWithHandlerThatFails() {
        RuntimeException testException = new RuntimeException("Test error");
        RuntimeException handlerException = new RuntimeException("Handler error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(testException);
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            assertEquals(testException, invocation.getArgument(0));
            ((CommandChain) invocation.getArgument(1)).fail(handlerException);
            return null;
        }).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        ExecutionException ee = assertThrows(ExecutionException.class,
                () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));
        assertEquals(handlerException, ee.getCause());

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(testException, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testFailureWithHandlerThatRecovers() throws Exception {
        RuntimeException testException = new RuntimeException("Test error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(testException);
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            assertEquals(testException, invocation.getArgument(0));
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(testException, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }


    @Test
    void testInterrupt() {

        doAnswer(invocation -> {
            ((Context)invocation.getArgument(0)).interrupt();
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        assertThrows(ExecutionException.class, () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testTimeoutWhenNextNotCalled() {
        doAnswer(invocation -> null).when(command1).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .build();

        assertThrows(TimeoutException.class, () -> commandExecutor.start(context).get(1, TimeUnit.SECONDS));
    }

    @Test
    void testTimeoutWhenNextNotCalledInFailureHandler() {
        // Il comando fallisce
        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).fail(new RuntimeException("Error"));
            return null;
        }).when(command1).execute(any(), any());

        // Il failureHandler non chiama né next() né fail()
        doAnswer(invocation -> null).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .onFailure(failureHandler)
                .build();

        assertThrows(TimeoutException.class, () -> commandExecutor.start(context).get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExecuteWithDynamicAddition() throws Exception {
        doAnswer(invocation -> {
            CommandExecutor executor = invocation.getArgument(1);
            executor.add(command3);
            executor.next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .build();

        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, command3);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);
        inOrder.verify(command3).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, command3);
    }

    @Test
    void testStartContinuous() throws ExecutionException, InterruptedException, TimeoutException {
        doAnswer(invocation -> {
            CommandExecutor executor = invocation.getArgument(1);
            executor.next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .build();

        Future<Void> future = commandExecutor.startContinuous(context);
        future.get(5, TimeUnit.SECONDS);

        commandExecutor.add(command3);
        future.get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, command3);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);
        inOrder.verify(command3).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, command3);

    }

    @Test
    void testStartContinuousEmpty() throws ExecutionException, InterruptedException, TimeoutException {
        doAnswer(invocation -> {
            CommandExecutor executor = invocation.getArgument(1);
            executor.next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder().build();
        Future<Void> future = commandExecutor.startContinuous(context);
        future.get(5, TimeUnit.SECONDS);

        commandExecutor.add(command1);
        commandExecutor.add(command2);
        future.get(5, TimeUnit.SECONDS);

        commandExecutor.add(command3);
        future.get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, command3);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);
        inOrder.verify(command3).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, command3);

    }

    @Test
    void testPipelineBuilderReexecution() throws Exception {

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                        .exec(command1)
                        .exec(command2)
                        .build();

        // First execution
        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        // Second execution
        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2);
        // First execution
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);
        // Second execution
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2);
    }

    @Test
    void testQueueBuilderNoReexecution() throws Exception {

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.queueBuilder()
                        .exec(command1)
                        .exec(command2)
                        .build();

        // First execution
        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        // Second execution
        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2);
        // Only first execution
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2);
    }

    @Test
    void testConstructors() {
        CommandExecutor executor1 = new CommandExecutor();
        assertNotNull(executor1);
        
        CommandSource source = mock(CommandSource.class);
        CommandExecutor executor2 = new CommandExecutor(source);
        assertNotNull(executor2);
    }

    @Test
    void testStartWithNullContext() {
        CommandExecutor executor = new CommandExecutor();
        assertThrows(IllegalArgumentException.class, () -> executor.start(null));
    }

    @Test
    void testFailTwice() {
        CommandExecutor executor = CommandExecutor.pipelineBuilder()
                .exec((ctx, chain) -> {
                    // non chiamando next() o fail() teniamo il comando "appeso"
                })
                .build();
        CompletableFuture<Void> future = executor.start(context);
        
        Exception ex1 = new RuntimeException("Error 1");
        Exception ex2 = new RuntimeException("Error 2");
        
        executor.fail(ex1);
        executor.fail(ex2);
        
        // Verifichiamo direttamente il future restituito da start
        ExecutionException ee = assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertEquals(ex1, ee.getCause());
    }

    @Test
    void testInterruptMethod() {
        Context mockContext = mock(Context.class);
        CommandExecutor executor = new CommandExecutor();
        
        // Test interrupt with null context
        executor.interrupt(); // Should not throw exception
        
        executor.start(mockContext);
        
        executor.interrupt();
        verify(mockContext).interrupt();
    }

    @Test
    void testGetCurrentContext() {
        CommandExecutor executor = new CommandExecutor();
        assertNull(executor.getCurrentContext());
        
        executor.start(context);
        assertEquals(context, executor.getCurrentContext());
    }

    @Test
    void testExecuteAsAsyncCommand() {
        CommandChain mockChain = mock(CommandChain.class);
        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        CommandExecutor executor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .build();
        
        executor.execute(context, mockChain);
        
        verify(mockChain, org.mockito.Mockito.timeout(1000)).next();
    }

    @Test
    void testExecuteAsAsyncCommandFailure() {
        CommandChain mockChain = mock(CommandChain.class);
        Exception ex = new RuntimeException("Fail");
        
        // Creiamo un executor manuale che fallisce
        CommandExecutor executor = new CommandExecutor() {
            @Override
            public CompletableFuture<Void> start(Context ctx) {
                CompletableFuture<Void> f = new CompletableFuture<>();
                // Il completamento eccezionale con ex (non avvolto) 
                // fa sì che e.getCause() in execute sia null
                f.completeExceptionally(ex);
                return f;
            }
            
            @Override
            public void fail(Throwable t) {
                // Il metodo execute chiama fail(e.getCause() != null ? e.getCause() : e)
                mockChain.fail(t);
            }
        };
        
        executor.execute(context, mockChain);
        
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(mockChain).fail(ex));
    }


}
