package com.jaewa.commandchain;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
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
                .exec("cmd1", command1)
                .exec("cmd2", command2)
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
                .exec("cmd1", command1)
                .exec("cmd2", command2)
                .onFailure(failureHandler)
                .build();


        commandExecutor.start(context).get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testExecuteWithFailure() {

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).fail(new IllegalStateException());
            return null;
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            assertEquals(IllegalStateException.class, invocation.getArgument(0).getClass());
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec("cmd1", command1)
                .exec("cmd2", command2)
                .onFailure(failureHandler)
                .build();

        assertThrows(ExecutionException.class, () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(argThat(IllegalStateException.class::isInstance), eq(commandExecutor));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testExecuteWithUncheckedFailure() {

        doAnswer(invocation -> {
            throw new IllegalStateException();
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            assertEquals(IllegalStateException.class, invocation.getArgument(0).getClass());
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());


        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec("cmd1", command1)
                .exec("cmd2", command2)
                .onFailure(failureHandler)
                .build();

        assertThrows(ExecutionException.class, () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(argThat(IllegalStateException.class::isInstance), eq(commandExecutor));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testInterrupt() {

        doAnswer(invocation -> {
            ((Context)invocation.getArgument(0)).interrupt();
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            assertEquals(CommandInterruptedException.class, invocation.getArgument(0).getClass());
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec("cmd1", command1)
                .exec("cmd2", command2)
                .onFailure(failureHandler)
                .build();

        assertThrows(ExecutionException.class, () -> commandExecutor.start(context).get(5, TimeUnit.SECONDS));

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(argThat(CommandInterruptedException.class::isInstance), eq(commandExecutor));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testTimeoutWhenNextNotCalled() {
        doAnswer(invocation -> null).when(command1).execute(any(), any());

        CommandExecutor commandExecutor = CommandExecutor.pipelineBuilder()
                .exec("cmd1", command1)
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
                .exec("cmd1", command1)
                .onFailure(failureHandler)
                .build();

        assertThrows(TimeoutException.class, () -> commandExecutor.start(context).get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExecuteWithDynamicAddition() throws Exception {
        doAnswer(invocation -> {
            CommandExecutor executor = invocation.getArgument(1);
            executor.add("cmd3", command3);
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
                .exec("cmd1", command1)
                .exec("cmd2", command2)
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
                .exec("cmd1", command1)
                .exec("cmd2", command2)
                .build();

        Future<Void> future = commandExecutor.startContinuous(context);
        future.get(5, TimeUnit.SECONDS);

        commandExecutor.add("cmd3", command3);
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

        commandExecutor.add("cmd1", command1);
        commandExecutor.add("cmd2", command2);
        future.get(5, TimeUnit.SECONDS);

        commandExecutor.add("cmd3", command3);
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
                        .exec("cmd1", command1)
                        .exec("cmd2", command2)
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
                        .exec("cmd1", command1)
                        .exec("cmd2", command2)
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


}
