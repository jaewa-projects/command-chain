package com.jaewa.commandchain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class CommandPipelineTest {

    private CommandPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new CommandPipeline();
    }

    @Test
    void testAddAndNext() {
        AsyncCommand cmd1 = mock(AsyncCommand.class);
        AsyncCommand cmd2 = mock(AsyncCommand.class);

        pipeline.add(cmd1);
        pipeline.add(cmd2);

        AsyncCommand next1 = pipeline.next();
        assertNotNull(next1);
        assertEquals(cmd1, next1);

        AsyncCommand next2 = pipeline.next();
        assertNotNull(next2);
        assertEquals(cmd2, next2);

        assertNull(pipeline.next());
    }

    @Test
    void testInitResetsExecution() {
        AsyncCommand cmd1 = mock(AsyncCommand.class);
        pipeline.add(cmd1);

        // Consume the command
        assertNotNull(pipeline.next());
        assertNull(pipeline.next());

        // Reset
        pipeline.init();

        // Should be able to get cmd1 again
        AsyncCommand nextAgain = pipeline.next();
        assertNotNull(nextAgain);
        assertEquals(cmd1, nextAgain);
    }

    @Test
    void testEmptyPipeline() {
        assertNull(pipeline.next());
        pipeline.init();
        assertNull(pipeline.next());
    }

    @Test
    void testAddAfterNext() {
        AsyncCommand cmd1 = mock(AsyncCommand.class);
        pipeline.add(cmd1);

        assertNotNull(pipeline.next());
        assertNull(pipeline.next());

        AsyncCommand cmd2 = mock(AsyncCommand.class);
        pipeline.add(cmd2);

        AsyncCommand next2 = pipeline.next();
        assertNotNull(next2);
        assertEquals(cmd2, next2);
        assertNull(pipeline.next());
    }
}