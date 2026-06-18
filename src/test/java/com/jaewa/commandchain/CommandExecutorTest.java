package com.jaewa.commandchain;

import java.util.concurrent.ExecutionException;
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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CommandExecutorTest {

    private CommandExecutor commandExecutor;
    private Context context;

    @Mock
    private AsyncCommand command1;

    @Mock
    private AsyncCommand command2;

    @Mock
    private AsyncFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        context = new ContextImpl();
        commandExecutor = new CommandExecutor(context);
    }

    @Test
    void testExecute() throws Exception {

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());


        commandExecutor.add("cmd1", command1);
        commandExecutor.add("cmd2", command2);
        commandExecutor.setFailureHandler(failureHandler);

        commandExecutor.start().get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(command2).execute(context, commandExecutor);

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testExecuteWithFailure() throws Exception {

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).fail(new IllegalStateException());
            return null;
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            assertEquals(IllegalStateException.class, invocation.getArgument(0).getClass());
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());


        commandExecutor.add("cmd1", command1);
        commandExecutor.add("cmd2", command2);
        commandExecutor.setFailureHandler(failureHandler);

        try {
            commandExecutor.start().get(5, TimeUnit.SECONDS);
            fail();
        } catch (ExecutionException e) {
            assertEquals(IllegalStateException.class, e.getCause().getClass());
        }

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(argThat(IllegalStateException.class::isInstance), eq(commandExecutor));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testExecuteWithUncheckedFailure() throws Exception {

        doAnswer(invocation -> {
            throw new IllegalStateException();
        }).when(command1).execute(any(), any());
        doAnswer(invocation -> {
            assertEquals(IllegalStateException.class, invocation.getArgument(0).getClass());
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(failureHandler).execute(any(), any());


        commandExecutor.add("cmd1", command1);
        commandExecutor.add("cmd2", command2);
        commandExecutor.setFailureHandler(failureHandler);

        try {
            commandExecutor.start().get(5, TimeUnit.SECONDS);
            fail();
        } catch (ExecutionException e) {
            assertEquals(IllegalStateException.class, e.getCause().getClass());
        }

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(argThat(IllegalStateException.class::isInstance), eq(commandExecutor));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testInterrupt() throws Exception {

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


        commandExecutor.add("cmd1", command1);
        commandExecutor.add("cmd2", command2);
        commandExecutor.setFailureHandler(failureHandler);

        try {
            commandExecutor.start().get(5, TimeUnit.SECONDS);
            fail();
        } catch (ExecutionException e) {
            assertEquals(CommandInterruptedException.class, e.getCause().getClass());
        }

        InOrder inOrder = inOrder(command1, command2, failureHandler);
        inOrder.verify(command1).execute(context, commandExecutor);
        inOrder.verify(failureHandler).execute(argThat(CommandInterruptedException.class::isInstance), eq(commandExecutor));

        verifyNoMoreInteractions(command1, command2, failureHandler);
    }

    @Test
    void testTimeoutWhenNextNotCalled() {
        doAnswer(invocation -> null).when(command1).execute(any(), any());

        commandExecutor.add("cmd1", command1);

        assertThrows(TimeoutException.class, () -> commandExecutor.start().get(1, TimeUnit.SECONDS));
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

        commandExecutor.add("cmd1", command1);
        commandExecutor.setFailureHandler(failureHandler);

        assertThrows(TimeoutException.class, () -> commandExecutor.start().get(1, TimeUnit.SECONDS));
    }

}
