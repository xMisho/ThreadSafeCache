package ThreadSafeCache;

public class Main {

    private static final long STANDARD_TTL = 5_000;
    private static final long STRESS_TTL = 60_000;

    public static void main(String[] args) throws InterruptedException {
        runFunctionalTests();
        runMultithreadedStressTest();

        System.out.println("\n=== ALL TESTS PASSED ===");
    }

    private static void runFunctionalTests() throws InterruptedException {
        System.out.println("=== FUNCTIONAL TESTS ===");

        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>(3);

        try {
            testBasicPutAndGet(cache);
            testRemoval(cache);
            testExpiration(cache);
            testCapacityAndEviction(cache);
            printCacheStats(cache);
        } finally {
            cache.shutdown();
        }
    }

    private static void testBasicPutAndGet(ThreadSafeCache<String, String> cache) {
        System.out.println("\n--- Basic Put / Get ---");

        cache.put("name", "Misho", STANDARD_TTL);
        cache.put("language", "Java", STANDARD_TTL);
        cache.put("city", "Dubai", STANDARD_TTL);

        assertEquals("Misho", cache.get("name"), "Existing key should return its value");
        assertEquals(null, cache.get("country"), "Missing key should return null");

        assertEquals(3, cache.size(), "Cache size should be 3");
        assertEquals(3, cache.getMaxCapacity(), "Maximum capacity should be 3");
        assertEquals(1, cache.getHitCount(), "Expected one cache hit");
        assertEquals(1, cache.getMissCount(), "Expected one cache miss");

        System.out.println("Passed.");
    }

    private static void testRemoval(ThreadSafeCache<String, String> cache) {
        System.out.println("\n--- Removal ---");

        cache.remove("city");

        assertFalse(
                cache.containsKey("city"),
                "Removed key should no longer exist"
        );

        System.out.println("Passed.");
    }

    private static void testExpiration(ThreadSafeCache<String, String> cache)
            throws InterruptedException {

        System.out.println("\n--- TTL Expiration ---");

        cache.put("temporary", "Hello", 1_000);

        assertEquals(
                "Hello",
                cache.get("temporary"),
                "Entry should exist before TTL expires"
        );

        Thread.sleep(1_500);

        assertEquals(
                null,
                cache.get("temporary"),
                "Entry should be unavailable after TTL expires"
        );

        System.out.println("Passed.");
    }

    private static void testCapacityAndEviction(ThreadSafeCache<String, String> cache) {
        System.out.println("\n--- Capacity / Eviction ---");

        int evictionsBefore = cache.getEvictionCount();

        for (int i = 1; i <= 4; i++) {
            cache.put("key" + i, "value" + i, STANDARD_TTL);

            assertTrue(
                    cache.size() <= cache.getMaxCapacity(),
                    "Cache exceeded maximum capacity"
            );
        }

        assertTrue(
                cache.getEvictionCount() > evictionsBefore,
                "Expected at least one eviction"
        );

        assertEquals(
                cache.getMaxCapacity(),
                cache.size(),
                "Cache should finish at maximum capacity"
        );

        System.out.println("Passed.");
    }

    private static void printCacheStats(ThreadSafeCache<?, ?> cache) {
        System.out.println("\n--- Functional Test Statistics ---");
        System.out.println("Size:      " + cache.size());
        System.out.println("Capacity:  " + cache.getMaxCapacity());
        System.out.println("Hits:      " + cache.getHitCount());
        System.out.println("Misses:    " + cache.getMissCount());
        System.out.println("Evictions: " + cache.getEvictionCount());
    }

    private static void runMultithreadedStressTest() throws InterruptedException {
        System.out.println("\n=== MULTITHREADED STRESS TEST ===");

        final int maxCapacity = 100;
        final int writesPerThread = 10_000;
        final int readsPerThread = 10_000;

        ThreadSafeCache<Integer, String> cache =
                new ThreadSafeCache<>(maxCapacity);

        try {
            Thread writer1 = new Thread(() -> {
                for (int i = 0; i < writesPerThread; i++) {
                    cache.put(i, "Value-" + i, STRESS_TTL);
                }
            }, "Writer-1");

            Thread writer2 = new Thread(() -> {
                for (int i = 10_000; i < 20_000; i++) {
                    cache.put(i, "Value-" + i, STRESS_TTL);
                }
            }, "Writer-2");

            Thread reader1 = new Thread(() -> {
                for (int i = 0; i < readsPerThread; i++) {
                    cache.get(i % 20_000);
                }
            }, "Reader-1");

            Thread reader2 = new Thread(() -> {
                for (int i = 0; i < readsPerThread; i++) {
                    cache.get((i + 10_000) % 20_000);
                }
            }, "Reader-2");

            Thread remover = new Thread(() -> {
                for (int i = 0; i < 5_000; i++) {
                    cache.remove((i * 4) % 20_000);
                }
            }, "Remover");

            writer1.start();
            writer2.start();
            reader1.start();
            reader2.start();
            remover.start();

            writer1.join();
            writer2.join();
            reader1.join();
            reader2.join();
            remover.join();

            validateStressTestResults(cache, readsPerThread * 2);

        } finally {
            cache.shutdown();
        }
    }

    private static void validateStressTestResults(
            ThreadSafeCache<Integer, String> cache,
            int expectedReads) {

        assertTrue(
                cache.size() <= cache.getMaxCapacity(),
                "Stress test cache exceeded maximum capacity"
        );

        int totalReads =
                cache.getHitCount() + cache.getMissCount();

        assertEquals(
                expectedReads,
                totalReads,
                "Hit/miss accounting does not match total reads"
        );

        System.out.println("Final size: " + cache.size());
        System.out.println("Max capacity: " + cache.getMaxCapacity());
        System.out.println("Hits: " + cache.getHitCount());
        System.out.println("Misses: " + cache.getMissCount());
        System.out.println("Evictions: " + cache.getEvictionCount());
        System.out.println("Stress test passed.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(
                    message +
                    " | expected: " + expected +
                    ", actual: " + actual
            );
        }
    }
}