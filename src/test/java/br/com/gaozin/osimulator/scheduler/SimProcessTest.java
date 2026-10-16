package br.com.gaozin.osimulator.scheduler;

import br.com.gaozin.osimulator.process.ProcessState;
import br.com.gaozin.osimulator.process.SimProcess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimProcessTest {
    @Test void consumesCpuAndCompletes() {
        SimProcess p = new SimProcess(1, "Teste", 0, 2, 1, 64);
        p.markFirstRun(0);
        p.setState(ProcessState.RUNNING);
        p.consumeCpuTick(); p.consumeCpuTick();
        p.completeAt(2);
        assertEquals(0, p.getRemainingTime());
        assertEquals(2, p.turnaroundTime());
        assertEquals(0, p.responseTime());
        assertEquals(ProcessState.TERMINATED, p.getState());
    }
    @Test void rejectsInvalidBurst() {
        assertThrows(IllegalArgumentException.class, () -> new SimProcess(1, "Teste", 0, 0, 1, 64));
    }
}
