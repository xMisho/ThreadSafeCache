import java.util.concurrent.ConcurrentHashMap;

public class ThreadSafeCache<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<V>> cache;

    public ThreadSafeCache() {
        cache = new ConcurrentHashMap<>();
    }

    public void put(K key, V value, long ttlMillis) {
    long expirationTime = System.currentTimeMillis() + ttlMillis;

    cache.put(
        key,
        new CacheEntry<>(value, expirationTime)
    );
}

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public boolean containsKey(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

}