package br.com.gaozin.osimulator.scheduler;

public enum SchedulingAlgorithm {
    FCFS("FCFS"), SJF("SJF"), PRIORITY("Prioridade"), ROUND_ROBIN("Round Robin");
    private final String label;
    SchedulingAlgorithm(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
