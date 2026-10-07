package ThreadSafeCache;

public class CacheEntry<V> {
    private final V value;
    private final long expirationTime;

    public CacheEntry(V value, long expirationTime) {
        this.value = value;
        this.expirationTime = expirationTime;
    }

    public V getValue() {
        return value;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
}
