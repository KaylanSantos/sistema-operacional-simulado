package br.com.gaozin.osimulator.events;

public record SimulationEvent(int tick, String category, String message) {
    @Override public String toString() {
        return String.format("T=%04d  %-10s %s", tick, category, message);
    }
}
