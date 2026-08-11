package com.jaewa.commandchain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultContextTest {

    @Test
    void testSetAndGet() {
        DefaultContext context = new DefaultContext();
        context.set("key1", "value1");
        context.set("key2", 123);

        assertEquals("value1", context.get("key1", String.class));
        assertEquals(123, context.get("key2", Integer.class));
    }

    @Test
    void testGetNonExistent() {
        DefaultContext context = new DefaultContext();
        assertNull(context.get("nonExistent", String.class));
    }

    @Test
    void testChildCanReadFromParent() {
        DefaultContext parent = new DefaultContext();
        parent.set("parentKey", "parentValue");

        DefaultContext child = new DefaultContext(parent);
        
        assertEquals("parentValue", child.get("parentKey", String.class));
    }

    @Test
    void testParentCannotReadFromChild() {
        DefaultContext parent = new DefaultContext();
        DefaultContext child = new DefaultContext(parent);

        child.set("childKey", "childValue");

        assertNull(parent.get("childKey", String.class));
        assertEquals("childValue", child.get("childKey", String.class));
    }

    @Test
    void testChildShadowsParent() {
        DefaultContext parent = new DefaultContext();
        parent.set("sharedKey", "parentValue");

        DefaultContext child = new DefaultContext(parent);
        child.set("sharedKey", "childValue");

        assertEquals("childValue", child.get("sharedKey", String.class));
        assertEquals("parentValue", parent.get("sharedKey", String.class));
    }

    @Test
    void testInterruptPropagation() {
        DefaultContext parent = new DefaultContext();
        DefaultContext child = new DefaultContext(parent);

        assertFalse(parent.isInterrupted());
        assertFalse(child.isInterrupted());

        child.interrupt();

        assertTrue(child.isInterrupted());
        assertTrue(parent.isInterrupted());
    }
    
    @Test
    void testInterruptFromParent() {
        DefaultContext parent = new DefaultContext();
        DefaultContext child = new DefaultContext(parent);

        parent.interrupt();

        assertTrue(parent.isInterrupted());
        assertTrue(child.isInterrupted());
    }
}