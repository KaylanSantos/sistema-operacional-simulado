package br.com.gaozin.osimulator.cache;

import java.util.*;

public final class CacheSimulator {
    private final int capacity;
    private final LinkedHashMap<Long, Long> lines = new LinkedHashMap<>();
    private long clock;
    private long hits;
    private long misses;

    public CacheSimulator(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("A capacidade da cache deve ser positiva.");
        this.capacity = capacity;
    }

    public boolean access(long physicalAddress) {
        if (physicalAddress < 0) throw new IllegalArgumentException("Endereço físico inválido.");
        clock++;
        if (lines.containsKey(physicalAddress)) {
            hits++;
            lines.put(physicalAddress, clock); // LRU: atualizar o último acesso
            return true;
        }
        misses++;
        if (lines.size() >= capacity) {
            Long victim = lines.entrySet().stream()
                    .min(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey).orElseThrow();
            lines.remove(victim);
        }
        lines.put(physicalAddress, clock);
        return false;
    }

    public int getCapacity() { return capacity; }
    public int getSize() { return lines.size(); }
    public long getHits() { return hits; }
    public long getMisses() { return misses; }
    public long getAccesses() { return hits + misses; }
    public double getHitRate() { return getAccesses() == 0 ? 0.0 : 100.0 * hits / getAccesses(); }
    public Set<Long> getCachedAddresses() { return Collections.unmodifiableSet(lines.keySet()); }
    public void clear() { lines.clear(); clock = 0; hits = 0; misses = 0; }
}
