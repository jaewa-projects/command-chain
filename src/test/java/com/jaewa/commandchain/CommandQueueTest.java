package com.jaewa.commandchain;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class CommandQueueTest {

    private CommandQueue commandQueue;

    @Mock
    private AsyncCommand command1;

    @Mock
    private AsyncCommand command2;

    @BeforeEach
    void setUp() {
        commandQueue = new CommandQueue();
    }

    @Test
    void testAddAndNext() {
        String name = "testCommand";
        commandQueue.add(name, command1);

        Pair<String, AsyncCommand> result = commandQueue.next();

        assertNotNull(result);
        assertEquals(name, result.getLeft());
        assertEquals(command1, result.getRight());

        assertNull(commandQueue.next());
    }

    @Test
    void testNextOnEmptyQueue() {
        assertNull(commandQueue.next());
    }

    @Test
    void testFifoOrder() {
        commandQueue.add("cmd1", command1);
        commandQueue.add("cmd2", command2);

        Pair<String, AsyncCommand> first = commandQueue.next();
        Pair<String, AsyncCommand> second = commandQueue.next();
        Pair<String, AsyncCommand> third = commandQueue.next();

        assertNotNull(first);
        assertEquals("cmd1", first.getLeft());
        assertEquals(command1, first.getRight());

        assertNotNull(second);
        assertEquals("cmd2", second.getLeft());
        assertEquals(command2, second.getRight());

        assertNull(third);
    }

    @Test
    void testInitDoesNothing() {
        // init non dovrebbe lanciare eccezioni o alterare lo stato visibile
        assertDoesNotThrow(() -> commandQueue.init());
        assertNull(commandQueue.next());
    }
}