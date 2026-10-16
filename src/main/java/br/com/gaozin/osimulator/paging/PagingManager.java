package br.com.gaozin.osimulator.paging;

import java.util.*;

public final class PagingManager {
    private final int frameCount;
    private final PageReplacementPolicy policy;
    private final LinkedHashMap<String, Long> residentPages = new LinkedHashMap<>();
    private long accessClock;
    private long pageFaults;
    private long pageHits;

    public PagingManager(int frameCount, PageReplacementPolicy policy) {
        if (frameCount <= 0) throw new IllegalArgumentException("É necessário ao menos um quadro.");
        this.frameCount = frameCount;
        this.policy = Objects.requireNonNull(policy);
    }

    public boolean access(int pid, int virtualPage) {
        if (pid <= 0 || virtualPage < 0) throw new IllegalArgumentException("Endereço de página inválido.");
        String key = pid + ":" + virtualPage;
        accessClock++;
        if (residentPages.containsKey(key)) {
            pageHits++;
            if (policy == PageReplacementPolicy.LRU) residentPages.put(key, accessClock);
            return false;
        }
        pageFaults++;
        if (residentPages.size() >= frameCount) {
            String victim;
            if (policy == PageReplacementPolicy.FIFO) {
                victim = residentPages.keySet().iterator().next();
            } else {
                victim = residentPages.entrySet().stream()
                        .min(Comparator.comparingLong(Map.Entry::getValue))
                        .map(Map.Entry::getKey).orElseThrow();
            }
            residentPages.remove(victim);
        }
        residentPages.put(key, accessClock);
        return true;
    }

    public long getPageFaults() { return pageFaults; }
    public long getPageHits() { return pageHits; }
    public int getResidentCount() { return residentPages.size(); }
    public Map<String, Long> getResidentPages() { return Collections.unmodifiableMap(residentPages); }
    public PageReplacementPolicy getPolicy() { return policy; }
    public void clear() { residentPages.clear(); accessClock = 0; pageFaults = 0; pageHits = 0; }
}
