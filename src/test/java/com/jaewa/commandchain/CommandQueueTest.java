package com.jaewa.commandchain;

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
        commandQueue.add(command1);

        AsyncCommand result = commandQueue.next();

        assertNotNull(result);
        assertEquals(command1, result);

        assertNull(commandQueue.next());
    }

    @Test
    void testNextOnEmptyQueue() {
        assertNull(commandQueue.next());
    }

    @Test
    void testFifoOrder() {
        commandQueue.add(command1);
        commandQueue.add(command2);

        AsyncCommand first = commandQueue.next();
        AsyncCommand second = commandQueue.next();
        AsyncCommand third = commandQueue.next();

        assertNotNull(first);
        assertEquals(command1, first);

        assertNotNull(second);
        assertEquals(command2, second);

        assertNull(third);
    }

    @Test
    void testInitDoesNothing() {
        // init non dovrebbe lanciare eccezioni o alterare lo stato visibile
        assertDoesNotThrow(() -> commandQueue.init());
        assertNull(commandQueue.next());
    }
}