package br.com.gaozin.osimulator.paging;

public enum PageReplacementPolicy {
    FIFO("FIFO"), LRU("LRU");
    private final String label;
    PageReplacementPolicy(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
