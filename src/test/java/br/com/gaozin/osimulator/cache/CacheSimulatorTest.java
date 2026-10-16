package br.com.gaozin.osimulator.cache;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CacheSimulatorTest {
    @Test void secondAccessHits() {
        CacheSimulator cache = new CacheSimulator(2);
        assertFalse(cache.access(100));
        assertTrue(cache.access(100));
        assertEquals(1, cache.getMisses());
        assertEquals(1, cache.getHits());
        assertEquals(50.0, cache.getHitRate());
    }
    @Test void evictsLeastRecentlyUsed() {
        CacheSimulator cache = new CacheSimulator(2);
        cache.access(1); cache.access(2); cache.access(1); cache.access(3);
        assertTrue(cache.getCachedAddresses().contains(1L));
        assertFalse(cache.getCachedAddresses().contains(2L));
        assertTrue(cache.getCachedAddresses().contains(3L));
    }
}
