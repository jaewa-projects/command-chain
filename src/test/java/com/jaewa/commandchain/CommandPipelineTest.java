package com.jaewa.commandchain;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
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

        pipeline.add("cmd1", cmd1);
        pipeline.add("cmd2", cmd2);

        Pair<String, AsyncCommand> next1 = pipeline.next();
        assertNotNull(next1);
        assertEquals("cmd1", next1.getLeft());
        assertEquals(cmd1, next1.getRight());

        Pair<String, AsyncCommand> next2 = pipeline.next();
        assertNotNull(next2);
        assertEquals("cmd2", next2.getLeft());
        assertEquals(cmd2, next2.getRight());

        assertNull(pipeline.next());
    }

    @Test
    void testInitResetsExecution() {
        AsyncCommand cmd1 = mock(AsyncCommand.class);
        pipeline.add("cmd1", cmd1);

        // Consume the command
        assertNotNull(pipeline.next());
        assertNull(pipeline.next());

        // Reset
        pipeline.init();

        // Should be able to get cmd1 again
        Pair<String, AsyncCommand> nextAgain = pipeline.next();
        assertNotNull(nextAgain);
        assertEquals("cmd1", nextAgain.getLeft());
        assertEquals(cmd1, nextAgain.getRight());
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
        pipeline.add("cmd1", cmd1);

        assertEquals("cmd1", pipeline.next().getLeft());
        assertNull(pipeline.next());

        AsyncCommand cmd2 = mock(AsyncCommand.class);
        pipeline.add("cmd2", cmd2);

        Pair<String, AsyncCommand> next2 = pipeline.next();
        assertNotNull(next2);
        assertEquals("cmd2", next2.getLeft());
        assertEquals(cmd2, next2.getRight());
        assertNull(pipeline.next());
    }
}