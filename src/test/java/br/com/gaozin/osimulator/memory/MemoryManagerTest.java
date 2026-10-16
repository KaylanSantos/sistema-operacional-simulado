package br.com.gaozin.osimulator.memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryManagerTest {
    @Test void allocatesAndReleasesFrames() {
        MemoryManager memory = new MemoryManager(256, 64);
        assertTrue(memory.allocate(1, 100));
        assertEquals(128, memory.getAllocatedUnits());
        assertEquals(2, memory.getUsedFrames());
        memory.release(1);
        assertEquals(0, memory.getAllocatedUnits());
        assertEquals(0, memory.getUsedFrames());
    }
    @Test void rejectsAllocationWhenFull() {
        MemoryManager memory = new MemoryManager(128, 64);
        assertTrue(memory.allocate(1, 128));
        assertFalse(memory.allocate(2, 64));
        assertEquals(128, memory.getAllocatedUnits());
    }
    @Test void validatesPageSize() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryManager(100, 64));
    }
}
