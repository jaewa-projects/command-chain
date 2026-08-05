package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class IfCommandTest {
    @Mock
    private AsyncCommand command1;
    @Mock
    private AsyncCommand command2;
    @Mock
    private AsyncCommand command3;

    private final MainExecutorBuilder builder = CommandExecutor.pipelineBuilder();
    private Context context;

    @BeforeEach
    void setUp() {

        context = new DefaultContext();
    }

    @Test
    void testWhen() throws ExecutionException, InterruptedException, TimeoutException {

        //@formatter:off
        CommandExecutor executor = builder
                .choice("choice")
                    .when(ctx -> true)
                        .exec("command1", command1)
                    .end()
                    .when(ctx -> true)
                        .exec("command2", command2)
                    .end()
                    .otherwise()
                        .exec("command3", command3)
                    .end()
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, times(1)).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, never()).execute(any(), any());
    }

    @Test
    void testWhenSecondOption() throws ExecutionException, InterruptedException, TimeoutException {

        //@formatter:off
        CommandExecutor executor = builder
                .choice("choice")
                    .when(ctx -> false)
                        .exec("command1", command1)
                    .end()
                    .when(ctx -> true)
                        .exec("command2", command2)
                    .end()
                    .otherwise()
                        .exec("command3", command3)
                    .end()
                .end()
                .build();
        //@formatter:on

        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command2).execute(any(), any());


        executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, never()).execute(any(), any());
        verify(command2, times(1)).execute(any(), any());
        verify(command3, never()).execute(any(), any());
    }

       @Test
    void testOtherwise() throws ExecutionException, InterruptedException, TimeoutException {

        CommandExecutor executor = builder
                .choice("choice")
                    .when(ctx -> false)
                        .exec("command1", command1)
                    .end()
                    .when(ctx -> false)
                        .exec("command2", command2)
                    .end()
                    .otherwise()
                        .exec("command3", command3)
                    .end()
                .end()
                .build();

           doAnswer(invocation -> {
               ((CommandChain)invocation.getArgument(1)).next();
               return null;
           }).when(command3).execute(any(), any());

           executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, never()).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, times(1)).execute(any(), any());
    }

    @Test
    void testNoOptionsNoOtherwise() throws ExecutionException, InterruptedException, TimeoutException {

        CommandExecutor executor = builder
                .choice("choice")
                    .when(ctx -> false)
                        .exec("command1", command1)
                    .end()
                    .when(ctx -> false)
                        .exec("command2", command2)
                    .end()
                .end()
                .build();

           executor.start(context).get(5, TimeUnit.SECONDS);

        verify(command1, never()).execute(any(), any());
        verify(command2, never()).execute(any(), any());
        verify(command3, never()).execute(any(), any());
    }


}
