package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TryCatchCommandTest {
    @Mock
    private AsyncCommand command1;
    @Mock
    private AsyncCommand command2;
    @Mock
    private AsyncCommand command3;
    @Mock
    private AsyncCommand command4;

    private final MainExecutorBuilder builder = CommandExecutor.pipelineBuilder();
    private Context context;

    @BeforeEach
    void setUp() {
        context = new DefaultContext();
    }

    @Test
    void testTryCatch() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IllegalArgumentException.class)
                    .exec(command3)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IllegalArgumentException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, times(1)).execute(any(), any());
    }

    @Test
    void testTryCatchFinally() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IllegalArgumentException.class)
                    .exec(command3)
                .doFinally()
                    .exec(command4)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IllegalArgumentException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command4).execute(any(), any());

        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, times(1)).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }

    @Test
    void testTryNoCatch() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IllegalArgumentException.class)
                    .exec(command3)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IOException());
            return null;
        }).when(command1).execute(any(), any());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> executor.start(context).get(5, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, ex.getCause());

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, never()).execute(any(), any());
    }

    @Test
    void testTryNoCatchFinally() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IllegalArgumentException.class)
                    .exec(command3)
                .doFinally()
                    .exec(command4)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IOException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command4).execute(any(), any());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> executor.start(context).get(5, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, ex.getCause());

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, never()).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }

    @Test
    void testTryFinally() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doFinally()
                    .exec(command4)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IOException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command4).execute(any(), any());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> executor.start(context).get(5, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, ex.getCause());

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }

    @Test
    void testTryMultipleCatch() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IllegalArgumentException.class)
                    .exec(command3)
                .doCatch(IOException.class)
                    .exec(command4)
                    .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IOException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command4).execute(any(), any());

        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, never()).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }

        @Test
    void testTryHierarchicalLeafCatch() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IOException.class)
                    .exec(command3)
                .doCatch(FileNotFoundException.class)
                    .exec(command4)
                    .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new FileNotFoundException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command4).execute(any(), any());

        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, never()).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }

        @Test
    void testTryHierarchicalParentCatch() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IOException.class)
                    .exec(command3)
                .doCatch(FileNotFoundException.class)
                    .exec(command4)
                    .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new EOFException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command3).execute(any(), any());

        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, times(1)).execute(any(), any());
        verify(command4, never()).execute(any(), any());
    }

    @Test
    void testTryCatchFinallyWithExceptionInCatch() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IOException.class)
                    .exec(command3)
                .doFinally()
                    .exec(command4)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IOException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IllegalStateException());
            return null;
        }).when(command3).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).next();
            return null;
        }).when(command4).execute(any(), any());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> executor.start(context).get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, ex.getCause());

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, times(1)).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }

    @Test
    void testTryCatchFinallyWithExceptionInCatchAndFinally() throws ExecutionException, InterruptedException, TimeoutException {
        //@formatter:off
        CommandExecutor executor = builder
                .doTry()
                    .exec(command1)
                    .exec(command2)
                .doCatch(IOException.class)
                    .exec(command3)
                .doFinally()
                    .exec(command4)
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IOException());
            return null;
        }).when(command1).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IllegalStateException());
            return null;
        }).when(command3).execute(any(), any());

        doAnswer(invocation -> {
            ((CommandChain) invocation.getArgument(1)).fail(new IllegalArgumentException());
            return null;
        }).when(command4).execute(any(), any());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> executor.start(context).get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, times(1)).execute(any(), any());
        verify(command4, times(1)).execute(any(), any());
    }


}