package br.com.gaozin.osimulator.process;

public final class SimProcess {
    private final int pid;
    private final String name;
    private final int arrivalTime;
    private final int burstTime;
    private final int priority;
    private final int memoryUnits;
    private int remainingTime;
    private int state;
    private int firstRunTime = -1;
    private int completionTime = -1;
    private int cpuTimeUsed;
    private int blockedTicks;
    private int pageFaultWait;
    private boolean admitted;

    public SimProcess(int pid, String name, int arrivalTime, int burstTime, int priority, int memoryUnits) {
        if (pid <= 0) throw new IllegalArgumentException("PID deve ser positivo.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome do processo é obrigatório.");
        if (arrivalTime < 0) throw new IllegalArgumentException("Chegada não pode ser negativa.");
        if (burstTime <= 0) throw new IllegalArgumentException("Duração de CPU deve ser maior que zero.");
        if (memoryUnits <= 0) throw new IllegalArgumentException("Memória solicitada deve ser maior que zero.");
        this.pid = pid;
        this.name = name.trim();
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.memoryUnits = memoryUnits;
        this.remainingTime = burstTime;
        this.state = ProcessState.NEW.ordinal();
    }

    public int getPid() { return pid; }
    public String getName() { return name; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getPriority() { return priority; }
    public int getMemoryUnits() { return memoryUnits; }
    public int getRemainingTime() { return remainingTime; }
    public ProcessState getState() { return ProcessState.values()[state]; }
    public int getFirstRunTime() { return firstRunTime; }
    public int getCompletionTime() { return completionTime; }
    public int getCpuTimeUsed() { return cpuTimeUsed; }
    public int getBlockedTicks() { return blockedTicks; }
    public int getPageFaultWait() { return pageFaultWait; }
    public boolean isAdmitted() { return admitted; }

    public void setState(ProcessState newState) { this.state = newState.ordinal(); }
    public void markAdmitted() { this.admitted = true; }
    public void markFirstRun(int time) { if (firstRunTime < 0) firstRunTime = time; }
    public void consumeCpuTick() {
        if (remainingTime <= 0) throw new IllegalStateException("Processo já terminou.");
        remainingTime--;
        cpuTimeUsed++;
    }
    public void incrementBlockedTick() { blockedTicks++; }
    public void incrementPageFaultWait() { pageFaultWait++; }
    public void completeAt(int time) {
        if (remainingTime != 0) throw new IllegalStateException("Não é possível concluir um processo com CPU restante.");
        completionTime = time;
        setState(ProcessState.TERMINATED);
    }
    public int turnaroundTime() { return completionTime < 0 ? -1 : completionTime - arrivalTime; }
    public int responseTime() { return firstRunTime < 0 ? -1 : firstRunTime - arrivalTime; }
    public int waitingTime() {
        if (completionTime < 0) return -1;
        return Math.max(0, turnaroundTime() - cpuTimeUsed - blockedTicks);
    }
}
