package br.com.gaozin.osimulator.simulation;

import br.com.gaozin.osimulator.cache.CacheSimulator;
import br.com.gaozin.osimulator.events.SimulationEvent;
import br.com.gaozin.osimulator.memory.MemoryManager;
import br.com.gaozin.osimulator.paging.PageReplacementPolicy;
import br.com.gaozin.osimulator.paging.PagingManager;
import br.com.gaozin.osimulator.process.ProcessState;
import br.com.gaozin.osimulator.process.SimProcess;
import br.com.gaozin.osimulator.scheduler.SchedulingAlgorithm;

import java.util.*;

public final class SimulationEngine {
    private final List<SimProcess> processes = new ArrayList<>();
    private final Deque<SimProcess> readyQueue = new ArrayDeque<>();
    private final List<SimulationEvent> events = new ArrayList<>();
    private MemoryManager memory;
    private PagingManager paging;
    private CacheSimulator cache;
    private SchedulingAlgorithm algorithm = SchedulingAlgorithm.FCFS;
    private int quantum = 2;
    private int tick;
    private int nextPid = 1;
    private int rrSlice;
    private SimProcess running;
    private long cpuTicks;
    private long idleTicks;
    private long contextSwitches;
    private boolean paused = true;
    private boolean finished;
    private final Set<Integer> arrived = new HashSet<>();

    public SimulationEngine() { this(1024, 64, 8, PageReplacementPolicy.LRU, 8); }

    public SimulationEngine(int ramCapacity, int pageSize, int cacheCapacity,
                           PageReplacementPolicy pagePolicy, int quantum) {
        this.memory = new MemoryManager(ramCapacity, pageSize);
        this.paging = new PagingManager(ramCapacity / pageSize, pagePolicy);
        this.cache = new CacheSimulator(cacheCapacity);
        setQuantum(quantum);
        loadDemoProcesses();
    }

    private void loadDemoProcesses() {
        addProcess("Editor", 0, 7, 2, 180);
        addProcess("Compilador", 0, 5, 1, 260);
        addProcess("Navegador", 2, 6, 3, 300);
        addProcess("Terminal", 4, 3, 2, 120);
    }

    public SimProcess addProcess(String name, int arrival, int burst, int priority, int memoryUnits) {
        if (finished || tick > 0) throw new IllegalStateException("Só é possível adicionar processos antes do início ou após reiniciar.");
        if (memoryUnits > memory.getCapacity()) throw new IllegalArgumentException("A memória solicitada não pode superar a capacidade total da RAM.");
        SimProcess process = new SimProcess(nextPid++, name, arrival, burst, priority, memoryUnits);
        processes.add(process);
        addEvent("PROCESSO", process.getName() + " (P" + process.getPid() + ") criado.");
        return process;
    }

    public void setAlgorithm(SchedulingAlgorithm algorithm) {
        Objects.requireNonNull(algorithm);
        if ((tick > 0 || running != null) && this.algorithm != algorithm)
            throw new IllegalStateException("Reinicie antes de trocar o algoritmo.");
        this.algorithm = algorithm;
    }
    public SchedulingAlgorithm getAlgorithm() { return algorithm; }
    public void setQuantum(int quantum) {
        if (quantum <= 0) throw new IllegalArgumentException("Quantum deve ser maior que zero.");
        this.quantum = quantum;
    }
    public int getQuantum() { return quantum; }
    public int getTick() { return tick; }
    public boolean isPaused() { return paused; }
    public boolean isFinished() { return finished; }
    public SimProcess getRunning() { return running; }
    public List<SimProcess> getProcesses() { return Collections.unmodifiableList(processes); }
    public List<SimulationEvent> getEvents() { return Collections.unmodifiableList(events); }
    public List<SimProcess> getReadyQueue() { return List.copyOf(readyQueue); }
    public MemoryManager getMemory() { return memory; }
    public PagingManager getPaging() { return paging; }
    public CacheSimulator getCache() { return cache; }
    public long getCpuTicks() { return cpuTicks; }
    public long getIdleTicks() { return idleTicks; }
    public long getContextSwitches() { return contextSwitches; }
    public double getCpuUtilization() {
        return tick == 0 ? 0.0 : 100.0 * cpuTicks / tick;
    }
    public long getCompletedCount() {
        return processes.stream().filter(p -> p.getState() == ProcessState.TERMINATED).count();
    }

    public void start() { if (!finished) paused = false; }
    public void pause() { paused = true; }

    public void step() {
        if (finished) return;
        paused = true;
        advanceOneTick();
    }

    public void advanceIfRunning() {
        if (!paused && !finished) advanceOneTick();
    }

    private void advanceOneTick() {
        admitArrivals();
        if (running == null) dispatchNext();
        if (running == null) {
            idleTicks++;
            tick++;
            checkFinished();
            return;
        }

        // Modelo didático: cada processo referencia uma página determinística por ciclo.
        int virtualPage = (int) ((cpuTicks + running.getPid()) % Math.max(1, (running.getMemoryUnits() + memory.getPageSize() - 1) / memory.getPageSize()));
        boolean fault = paging.access(running.getPid(), virtualPage);
        long physicalAddress = (long) running.getPid() * 1_000_000L + (long) virtualPage * memory.getPageSize();
        boolean cacheHit = cache.access(physicalAddress);
        if (fault) {
            addEvent("PAGINAÇÃO", "Falta de página em P" + running.getPid() + ", página " + virtualPage + ".");
        }
        addEvent("CACHE", (cacheHit ? "HIT" : "MISS") + " no endereço físico simulado " + physicalAddress + ".");
        running.markFirstRun(tick);
        running.consumeCpuTick();
        cpuTicks++;
        if (algorithm == SchedulingAlgorithm.ROUND_ROBIN) rrSlice++;
        SimProcess justRan = running;
        if (running.getRemainingTime() == 0) {
            running.completeAt(tick + 1);
            memory.release(running.getPid());
            addEvent("PROCESSO", running.getName() + " concluído.");
            running = null;
            rrSlice = 0;
        } else if (algorithm == SchedulingAlgorithm.ROUND_ROBIN && rrSlice >= quantum) {
            justRan.setState(ProcessState.READY);
            readyQueue.addLast(justRan);
            running = null;
            rrSlice = 0;
            addEvent("ESCALONADOR", "Quantum expirou; P" + justRan.getPid() + " voltou à fila.");
        }
        tick++;
        admitArrivals();
        if (running == null) dispatchNext();
        memory.checkInvariant();
        checkFinished();
    }

    private void admitArrivals() {
        for (SimProcess p : processes) {
            if (!arrived.contains(p.getPid()) && p.getArrivalTime() <= tick) {
                arrived.add(p.getPid());
                if (!memory.allocate(p.getPid(), p.getMemoryUnits())) {
                    // Não permitir processo sem memória: mantém-se NEW e tenta novamente em ciclos futuros.
                    addEvent("MEMÓRIA", "RAM insuficiente para P" + p.getPid() + "; tentativa será repetida.");
                    arrived.remove(p.getPid());
                    continue;
                }
                p.markAdmitted();
                p.setState(ProcessState.READY);
                enqueueReady(p);
                addEvent("PROCESSO", p.getName() + " entrou na fila de prontos.");
            }
        }
    }

    private void enqueueReady(SimProcess p) {
        if (algorithm == SchedulingAlgorithm.FCFS || algorithm == SchedulingAlgorithm.ROUND_ROBIN) {
            readyQueue.addLast(p);
        } else {
            List<SimProcess> sorted = new ArrayList<>(readyQueue);
            sorted.add(p);
            Comparator<SimProcess> cmp = algorithm == SchedulingAlgorithm.SJF
                    ? Comparator.comparingInt(SimProcess::getRemainingTime)
                        .thenComparingInt(SimProcess::getArrivalTime).thenComparingInt(SimProcess::getPid)
                    : Comparator.comparingInt(SimProcess::getPriority)
                        .thenComparingInt(SimProcess::getArrivalTime).thenComparingInt(SimProcess::getPid);
            sorted.sort(cmp);
            readyQueue.clear();
            readyQueue.addAll(sorted);
        }
    }

    private void dispatchNext() {
        if (running != null || readyQueue.isEmpty()) return;
        running = readyQueue.removeFirst();
        running.setState(ProcessState.RUNNING);
        rrSlice = 0;
        if (contextSwitches > 0 || cpuTicks > 0) contextSwitches++;
        addEvent("CPU", "P" + running.getPid() + " começou/retomou a execução.");
    }

    private void checkFinished() {
        boolean allDone = processes.stream().allMatch(p -> p.getState() == ProcessState.TERMINATED);
        if (allDone) {
            finished = true;
            paused = true;
            running = null;
            addEvent("SISTEMA", "Todos os processos foram concluídos.");
        }
    }

    private void addEvent(String category, String message) {
        events.add(new SimulationEvent(tick, category, message));
        if (events.size() > 2000) events.remove(0);
    }

    public void reset() {
        SchedulingAlgorithm oldAlgorithm = algorithm;
        int oldQuantum = quantum;
        PageReplacementPolicy policy = paging.getPolicy();
        int ram = memory.getCapacity(), pageSize = memory.getPageSize(), cacheSize = cache.getCapacity();
        processes.clear(); readyQueue.clear(); events.clear(); arrived.clear();
        resetStateFields();
        memory = new MemoryManager(ram, pageSize);
        paging = new PagingManager(ram / pageSize, policy);
        cache = new CacheSimulator(cacheSize);
        algorithm = oldAlgorithm; quantum = oldQuantum;
        nextPid = 1;
        loadDemoProcesses();
    }

    private void resetStateFields() {
        tick = 0; rrSlice = 0; running = null; cpuTicks = 0; idleTicks = 0; contextSwitches = 0;
        paused = true; finished = false;
    }
}
