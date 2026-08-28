package com.jaewa.commandchain;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testFailureWithoutHandler() {
        RuntimeException testException = new RuntimeException("Test error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(testException);
            return null;
        }).when(command1).execute(any(Context.class), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .build();

        ExecutionException ee = assertThrows(ExecutionException.class,
                () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));
        assertEquals(testException, ee.getCause());

        InOrder inOrder = inOrder(command1, command2);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));

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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(testException), any(CommandChain.class));

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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(testException), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testFailureWithHandlerThatRecoversButLaterCommandFails() throws Exception {
        RuntimeException firstException = new RuntimeException("First error");
        RuntimeException secondException = new RuntimeException("Second error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(firstException);
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(secondException);
            return null;
        }).when(command3).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .exec(command3)
                .onFailure(failureHandler)
                .build();

        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, command3, failureHandler);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(firstException), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command3).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(secondException), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, command3, failureHandler);
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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testInterruptWithFailureHandlerNotInvoked() {
        doAnswer(invocation -> {
            Context ctx = invocation.getArgument(0);
            ctx.interrupt();
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        assertThrows(ExecutionException.class, () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testInterruptCommandExecutor() {
        CommandExecutor executor = new CommandExecutor();

        // Test interrupt with null context
        executor.interrupt(); // Should not throw exception

        executor.start(context);

        executor.interrupt();
        assertTrue(context.isInterrupted());
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
            CommandChain chain = invocation.getArgument(1);
            chain.add(command3);
            chain.next();
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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command3).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, command3);
    }

    @Test
    void testStartContinuous() throws ExecutionException, InterruptedException, TimeoutException {
        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.next();
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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command3).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, command3);

    }

    @Test
    void testStartContinuousEmpty() throws ExecutionException, InterruptedException, TimeoutException {
        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.next();
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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command3).execute(any(Context.class), any(CommandChain.class));

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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        // Second execution
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

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
    void testGetCurrentContext() {
        CommandExecutor executor = new CommandExecutor();
        assertNull(executor.getCurrentContext());
        
        executor.start(context);
        assertNotNull(executor.getCurrentContext());
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
    void testNextCalledMultipleTimesBySameCommandIsIgnored() throws Exception {
        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.next();
            chain.next();
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

        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2);
    }

    @Test
    void testFailCalledAfterNextBySameCommandIsIgnored() throws Exception {
        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.next();
            chain.fail(new RuntimeException("Delayed or duplicate error"));
            return null;
        }).when(command1).execute(any(), any());

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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testNextCalledAfterFailBySameCommandIsIgnored() {
        RuntimeException testException = new RuntimeException("Initial failure");

        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.fail(testException);
            chain.next();
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
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2);
    }

    @Test
    void testFailCalledMultipleTimesBySameCommandIsIgnored() {
        RuntimeException firstException = new RuntimeException("First error");
        RuntimeException secondException = new RuntimeException("Second error");

        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.fail(firstException);
            chain.fail(secondException);
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(invocation.getArgument(0));
            return null;
        }).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        ExecutionException ee = assertThrows(ExecutionException.class,
                () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));
        assertEquals(firstException, ee.getCause());

        InOrder inOrder = inOrder(command1, failureHandler, command2);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(firstException), any(CommandChain.class));

        verifyNoMoreInteractions(command1, failureHandler, command2);
    }

    @Test
    void testCallNextOrFailWhenNoLongerActiveCommandIsIgnored() throws Exception {
        AtomicReference<CommandChain> command1ChainRef = new AtomicReference<>();

        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            command1ChainRef.set(chain);
            chain.next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            // command1 tries to call next and fail while command2 is running
            command1ChainRef.get().next();
            command1ChainRef.get().fail(new RuntimeException("Late fail from command1"));

            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        // Now pipeline is finished, calling next or fail on command1's chain should still be ignored
        command1ChainRef.get().next();
        command1ChainRef.get().fail(new RuntimeException("Late fail after finish"));

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testFailureHandlerCallsNextMultipleTimesIsIgnored() throws Exception {
        RuntimeException testException = new RuntimeException("Test error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(testException);
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.next();
            chain.next();
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

        InOrder inOrder = inOrder(command1, failureHandler, command2);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(testException), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, failureHandler, command2);
    }

    @Test
    void testFailureHandlerCallsFailMultipleTimesIsIgnored() {
        RuntimeException initialException = new RuntimeException("Initial error");
        RuntimeException handlerFirstException = new RuntimeException("Handler first error");
        RuntimeException handlerSecondException = new RuntimeException("Handler second error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(initialException);
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            CommandChain chain = invocation.getArgument(1);
            chain.fail(handlerFirstException);
            chain.fail(handlerSecondException);
            return null;
        }).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(command2)
                .onFailure(failureHandler)
                .build();

        ExecutionException ee = assertThrows(ExecutionException.class,
                () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));
        assertEquals(handlerFirstException, ee.getCause());

        InOrder inOrder = inOrder(command1, failureHandler, command2);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(initialException), any(CommandChain.class));

        verifyNoMoreInteractions(command1, failureHandler, command2);
    }

    @Test
    void testExecuteWithInnerCommandExecutorSuccess() throws Exception {
        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
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

        CommandExecutor innerExecutor = CommandExecutor.pipelineBuilder()
                .exec(command2)
                .build();

        CommandExecutor mainExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(innerExecutor)
                .exec(command3)
                .build();

        mainExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, command3);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command3).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, command3);
    }

    @Test
    void testExecuteWithInnerCommandExecutorFailure() throws Exception {
        RuntimeException innerException = new RuntimeException("Inner error");

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(innerException);
            return null;
        }).when(command2).execute(any(), any());

        doAnswer(invocation -> {
            assertEquals(innerException, invocation.getArgument(0));
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        CommandExecutor innerExecutor = CommandExecutor.pipelineBuilder()
                .exec(command2)
                .build();

        CommandExecutor mainExecutor = CommandExecutor.pipelineBuilder()
                .exec(command1)
                .exec(innerExecutor)
                .exec(command3)
                .onFailure(failureHandler)
                .build();

        mainExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, failureHandler, command3);
        inOrder.verify(command1).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(command2).execute(any(Context.class), any(CommandChain.class));
        inOrder.verify(failureHandler).execute(eq(innerException), any(CommandChain.class));
        inOrder.verify(command3).execute(any(Context.class), any(CommandChain.class));

        verifyNoMoreInteractions(command1, command2, failureHandler, command3);
    }

}
