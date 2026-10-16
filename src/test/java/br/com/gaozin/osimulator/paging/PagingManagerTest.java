package br.com.gaozin.osimulator.paging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PagingManagerTest {
    @Test void countsFaultsAndHits() {
        PagingManager paging = new PagingManager(2, PageReplacementPolicy.FIFO);
        assertTrue(paging.access(1, 0));
        assertFalse(paging.access(1, 0));
        assertEquals(1, paging.getPageFaults());
        assertEquals(1, paging.getPageHits());
    }
    @Test void fifoReplacesOldestPage() {
        PagingManager paging = new PagingManager(2, PageReplacementPolicy.FIFO);
        paging.access(1, 0); paging.access(1, 1); paging.access(1, 2);
        assertFalse(paging.getResidentPages().containsKey("1:0"));
        assertTrue(paging.getResidentPages().containsKey("1:1"));
    }
}
