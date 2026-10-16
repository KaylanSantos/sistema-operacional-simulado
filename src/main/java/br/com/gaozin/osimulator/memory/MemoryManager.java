package br.com.gaozin.osimulator.memory;

import java.util.*;

public final class MemoryManager {
    private final int capacity;
    private final int pageSize;
    private final int frameCount;
    private final Integer[] frameOwnerPid;
    private final Map<Integer, LinkedHashMap<Integer, Integer>> pageTables = new LinkedHashMap<>();
    private int allocatedUnits;

    public MemoryManager(int capacity, int pageSize) {
        if (capacity <= 0) throw new IllegalArgumentException("RAM deve ser maior que zero.");
        if (pageSize <= 0 || capacity % pageSize != 0)
            throw new IllegalArgumentException("A RAM deve ser divisível pelo tamanho da página.");
        this.capacity = capacity;
        this.pageSize = pageSize;
        this.frameCount = capacity / pageSize;
        this.frameOwnerPid = new Integer[frameCount];
    }

    public int getCapacity() { return capacity; }
    public int getPageSize() { return pageSize; }
    public int getFrameCount() { return frameCount; }
    public int getAllocatedUnits() { return allocatedUnits; }
    public int getFreeUnits() { return capacity - allocatedUnits; }
    public int getUsedFrames() {
        int n = 0; for (Integer owner : frameOwnerPid) if (owner != null) n++; return n;
    }
    public Integer getFrameOwner(int frame) { return frameOwnerPid[frame]; }
    public Map<Integer, Integer> getPageTable(int pid) {
        return Collections.unmodifiableMap(pageTables.getOrDefault(pid, new LinkedHashMap<>()));
    }
    public boolean hasProcess(int pid) { return pageTables.containsKey(pid); }

    public boolean allocate(int pid, int memoryUnits) {
        if (pid <= 0 || memoryUnits <= 0 || pageTables.containsKey(pid)) return false;
        int framesNeeded = (memoryUnits + pageSize - 1) / pageSize;
        int freeFrames = frameCount - getUsedFrames();
        if (framesNeeded > freeFrames) return false;
        LinkedHashMap<Integer, Integer> table = new LinkedHashMap<>();
        int page = 0;
        for (int frame = 0; frame < frameCount && table.size() < framesNeeded; frame++) {
            if (frameOwnerPid[frame] == null) {
                frameOwnerPid[frame] = pid;
                table.put(page++, frame);
            }
        }
        pageTables.put(pid, table);
        allocatedUnits += framesNeeded * pageSize;
        checkInvariant();
        return true;
    }

    public void release(int pid) {
        LinkedHashMap<Integer, Integer> table = pageTables.remove(pid);
        if (table == null) return;
        for (Integer frame : table.values()) {
            if (frameOwnerPid[frame] != null && frameOwnerPid[frame] == pid) {
                frameOwnerPid[frame] = null;
                allocatedUnits -= pageSize;
            }
        }
        checkInvariant();
    }

    public void checkInvariant() {
        if (allocatedUnits < 0 || allocatedUnits > capacity) throw new IllegalStateException("Uso de RAM inválido.");
        int used = getUsedFrames();
        if (allocatedUnits != used * pageSize) throw new IllegalStateException("Contabilidade de RAM inconsistente.");
        Set<Integer> mappedFrames = new HashSet<>();
        for (Map.Entry<Integer, LinkedHashMap<Integer, Integer>> process : pageTables.entrySet()) {
            for (Integer frame : process.getValue().values()) {
                if (frame < 0 || frame >= frameCount) throw new IllegalStateException("Quadro fora dos limites.");
                if (!mappedFrames.add(frame)) throw new IllegalStateException("Quadro mapeado por mais de uma página.");
                if (!Objects.equals(frameOwnerPid[frame], process.getKey()))
                    throw new IllegalStateException("Proprietário do quadro inconsistente.");
            }
        }
    }
}
