# ThreadSafeCache

A generic concurrent in-memory Java cache with per-entry TTL expiration, periodic cleanup, bounded capacity, arbitrary eviction, and usage statistics.

Java 17+ · Maven · JUnit 5 · No production dependencies

## Features

- Generic keys and values, with put, get, remove, contains, size, and clear operations.
- Per-entry TTL expressed in milliseconds.
- Lazy expiration checks plus scheduled cleanup every second.
- A capacity lock serializes capacity checks, eviction, and insertion.
- Conditional removal protects newer replacements during expiration cleanup.
- Atomic hit, miss, and eviction counters.
- Explicit scheduler shutdown.

## Technical concepts

`ConcurrentHashMap`, immutable entry metadata, `AtomicInteger`, `ScheduledExecutorService`, `TimeUnit`, and `ReentrantLock`.

## Architecture

| Class | Responsibility |
| --- | --- |
| `CacheEntry<V>` | Value, expiration timestamp, and expiration check |
| `ThreadSafeCache<K,V>` | Cache operations, bounded insertion, cleanup scheduler, and counters |

```text
ThreadSafeCache/
├── README.md
├── .gitignore
├── pom.xml
└── src/
    ├── main/java/com/misho/cache/
    │   ├── CacheEntry.java
    │   └── ThreadSafeCache.java
    └── test/java/com/misho/cache/
        └── ThreadSafeCacheTest.java
```

## Build and run

Install JDK 17 or newer and Maven 3.9+. Clone the repository and run its tests:

```bash
git clone https://github.com/xMisho/ThreadSafeCache.git
cd ThreadSafeCache
mvn test
```

Build and verify the library JAR:

```bash
mvn clean verify
```

The artifact is written to `target/thread-safe-cache-1.0.0-SNAPSHOT.jar`. These classes are a library and test suite, so the JAR has no application entry point. Run the JUnit class from an IDE, or run a single suite with:

```bash
mvn -Dtest=ThreadSafeCacheTest test
```

## Testing

Four JUnit tests cover functional operations and expiration, concurrent access, replacement without eviction, clearing without resetting statistics, and invalid capacity.

The concurrent test uses two writers, two readers, and one remover. It verifies final size does not exceed 100 and that **hits + misses = 20,000**. Individual hit and eviction counts vary with scheduling. Worker failures are propagated to JUnit.

Surefire writes detailed results to `target/surefire-reports/`. Concurrency tests validate invariants for the exercised workload; they are not a proof for every possible thread schedule.

## Example behavior

Use these classes from the `com.misho.cache` package (and import `java.math.BigDecimal` for the exchange example):

```java
ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(100);
try {
    cache.put("name", "Misho", 5_000);
    System.out.println(cache.get("name"));    // Misho
    System.out.println(cache.get("missing")); // null
    System.out.println(cache.getHitCount());  // 1
    System.out.println(cache.getMissCount()); // 1
} finally {
    cache.shutdown();
}
```

## Scope and limitations

Eviction is arbitrary, not LRU. Expired entries can occupy capacity until an access or cleanup removes them. `size()` cleans first but is still an observation under concurrent updates, not a transaction across calls. Hits and misses count `get()` calls only; `clear()` does not reset statistics.

TTL uses wall-clock milliseconds. The implementation does not reject nonpositive TTLs. Keys cannot be null, and callers should avoid null values because `get()` uses null for a miss. The stored value itself is not made immutable or thread-safe. Atomic integer counters can eventually overflow.

Always call `shutdown()` when finished: the scheduler uses a non-daemon thread. This project has no persistence or distributed caching.

## What this project taught me

A concurrent map protects individual operations, but a capacity check followed by insertion needs its own lock. Conditional removal avoids deleting a replacement another thread just stored. Expiration cleanup and scheduler lifecycle are part of the cache’s behavior, not just storage details.

## Project history

Extracted from [Java-Projects](https://github.com/xMisho/Java-Projects), preserving commits that changed this project’s Java sources, including original authors, timestamps, and commit messages. Commit hashes changed because unrelated files and directory prefixes were removed. The Maven migration is a subsequent commit. The original monorepo remains available.

The layout follows [Maven’s standard directory conventions](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html); tests use [JUnit 5](https://docs.junit.org/5.11.4/user-guide/).
