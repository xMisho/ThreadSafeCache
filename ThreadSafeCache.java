import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeCache<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final AtomicInteger hitCount;
    private final AtomicInteger missCount;
    private final ScheduledExecutorService cleanupExecutor;
    private final int maxCapacity;
    private final ReentrantLock capacityLock;

    public ThreadSafeCache(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        this.maxCapacity = maxCapacity;
        capacityLock = new ReentrantLock();
        cache = new ConcurrentHashMap<>();
        hitCount = new AtomicInteger(0);
        missCount = new AtomicInteger(0);
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                1,
                1,
                TimeUnit.SECONDS
        );
    }

    public int getHitCount() {
        return hitCount.get();
    }

    public int getMissCount() {
        return missCount.get();
    }

    public void put(K key, V value, long ttlMillis) {
        long expirationTime = System.currentTimeMillis() + ttlMillis;

        capacityLock.lock();
        try {
            if (!cache.containsKey(key) && cache.size() >= maxCapacity) {
                evictEntry();
            }
            cache.put(key, new CacheEntry<>(value, expirationTime));
        } finally {
            capacityLock.unlock();
        }
    }

    private void evictEntry() {
        K keyToRemove = cache.keySet().stream().findFirst().orElse(null);
        if (keyToRemove != null) {
            cache.remove(keyToRemove);
        }
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                cache.remove(key, entry);
            }
            missCount.incrementAndGet();
            return null;
        }
        hitCount.incrementAndGet();
        return entry.getValue();
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public boolean containsKey(K key) {
    CacheEntry<V> entry = cache.get(key);

    if (entry == null || entry.isExpired()) {
        if (entry != null) {
            cache.remove(key, entry);
        }
        return false;
    }

    return true;
}

    private void cleanupExpiredEntries() {
        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
    }

}
