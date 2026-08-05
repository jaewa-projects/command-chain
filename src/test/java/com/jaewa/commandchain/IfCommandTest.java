package com.jaewa.commandchain;

import com.jaewa.commandchain.builders.MainExecutorBuilder;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class IfCommandTest {
    @Mock
    private AsyncCommand command1;
    @Mock
    private AsyncCommand command2;
    @Mock
    private AsyncCommand command3;

    private MainExecutorBuilder builder = CommandExecutor.pipelineBuilder();

    @Test
    void testWhen() {
        doAnswer(invocation -> {
            ((CommandChain)invocation.getArgument(1)).next();
            return null;
        }).when(command1).execute(any(), any());

        CommandExecutor executor = builder
                .choice()
                    .when("myLoop", ctx -> true)
                        .exec("command1", command1)
                    .end()
                    .when("command2", ctx -> false)
                        .exec("command1", command1)
                    .end()
                    .otherwise("otherwise")
                        .exec("command1", command1)
                    .end()
                .end()
                .build();

        Context context = new DefaultContext();
        executor.start(context).get(5, TimeUnit.SECONDS);

    }
}
