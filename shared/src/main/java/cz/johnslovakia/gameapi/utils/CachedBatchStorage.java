package cz.johnslovakia.gameapi.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class CachedBatchStorage<K, V> {

    private static final Logger LOGGER = Logger.getLogger(CachedBatchStorage.class.getName());

    private final String name;
    private final BatchConfig config;

    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Map<K, Long> lastAccessTime = new ConcurrentHashMap<>();

    private final Map<K, PendingChange<K, V>> pendingChanges = new ConcurrentHashMap<>();

    private final DataLoader<K, V> dataLoader;
    private final DataSaver<K, V> dataSaver;
    private final ValueMerger<V> valueMerger;

    private final ScheduledExecutorService scheduler;
    private volatile boolean isShutdown = false;

    public CachedBatchStorage(
            @NotNull String name,
            @NotNull BatchConfig config,
            @NotNull DataLoader<K, V> dataLoader,
            @NotNull DataSaver<K, V> dataSaver,
            @NotNull ValueMerger<V> valueMerger) {

        this.name = name;
        this.config = config;
        this.dataLoader = dataLoader;
        this.dataSaver = dataSaver;
        this.valueMerger = valueMerger;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CachedBatch-" + name);
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
                this::flushIfNeeded,
                config.getFlushInterval().toMillis(),
                config.getFlushInterval().toMillis(),
                TimeUnit.MILLISECONDS
        );

        if (config.isCacheEvictionEnabled()) {
            scheduler.scheduleAtFixedRate(
                    this::evictStaleEntries,
                    config.getCacheMaxAge().toMillis(),
                    config.getCacheMaxAge().toMillis(),
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public CompletableFuture<V> get(@NotNull K key) {
        V cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<K, V> loaded = dataLoader.load(Collections.singleton(key));
                V value = loaded.get(key);

                if (value != null) {
                    cache.put(key, value);

                    if (config.isDebugEnabled()) {
                        LOGGER.info(String.format("[%s] Loaded %s from DB", name, key));
                    }
                }

                return value;
            } catch (Exception e) {
                LOGGER.severe(String.format("[%s] Failed to load %s: %s", name, key, e.getMessage()));
                throw new RuntimeException(e);
            }
        });
    }

    public V getCached(@NotNull K key) {
        return cache.get(key);
    }

    public void modify(@NotNull K key, @NotNull V delta) {
        if (isShutdown) {
            throw new IllegalStateException("CachedBatchStorage is shutdown");
        }

        cache.merge(key, delta, valueMerger::merge);

        pendingChanges.merge(key,
                new PendingChange<>(key, delta, System.currentTimeMillis()),
                (existing, newChange) -> {
                    V merged = valueMerger.merge(existing.delta, newChange.delta);
                    return new PendingChange<>(key, merged, newChange.timestamp);
                }
        );

        if (config.isDebugEnabled()) {
            LOGGER.fine(String.format("[%s] Modified %s with delta %s (cache updated)", name, key, delta));
        }

        if (pendingChanges.size() >= config.getMaxBatchSize()) {
            scheduler.execute(this::flushIfNeeded);
        }
    }

    public void set(@NotNull K key, @NotNull V value) {
        if (isShutdown) {
            throw new IllegalStateException("CachedBatchStorage is shutdown");
        }

        cache.put(key, value);

        pendingChanges.put(key, new PendingChange<>(key, value, System.currentTimeMillis(), true));

        if (config.isDebugEnabled()) {
            LOGGER.fine(String.format("[%s] Set %s to %s (cache updated)", name, key, value));
        }

        if (pendingChanges.size() >= config.getMaxBatchSize()) {
            scheduler.execute(this::flushIfNeeded);
        }
    }

    public CompletableFuture<Void> preload(@NotNull Collection<K> keys) {
        return CompletableFuture.runAsync(() -> {
            try {
                Set<K> toLoad = new HashSet<>();
                for (K key : keys) {
                    if (!cache.containsKey(key)) {
                        toLoad.add(key);
                    }
                }

                if (!toLoad.isEmpty()) {
                    Map<K, V> loaded = dataLoader.load(toLoad);
                    cache.putAll(loaded);

                    long now = System.currentTimeMillis();
                    for (K key : loaded.keySet()) {
                        lastAccessTime.put(key, now);
                    }

                    if (config.isDebugEnabled()) {
                        LOGGER.info(String.format("[%s] Preloaded %d entries", name, loaded.size()));
                    }
                }
            } catch (Exception e) {
                LOGGER.severe(String.format("[%s] Preload failed: %s", name, e.getMessage()));
            }
        });
    }

    public void invalidate(@NotNull K key) {
        cache.remove(key);
    }

    public void clearCache() {
        cache.clear();
    }

    public int getCacheSize() {
        return cache.size();
    }

    public int getPendingChangesCount() {
        return pendingChanges.size();
    }

    private void evictStaleEntries() {
        if (!config.isCacheEvictionEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxAge = config.getCacheMaxAge().toMillis();

        List<K> toEvict = new ArrayList<>();

        for (Map.Entry<K, Long> entry : lastAccessTime.entrySet()) {
            K key = entry.getKey();
            long lastAccess = entry.getValue();

            if ((now - lastAccess) > maxAge && !pendingChanges.containsKey(key)) {
                toEvict.add(key);
            }
        }

        for (K key : toEvict) {
            cache.remove(key);
            lastAccessTime.remove(key);
        }

        if (!toEvict.isEmpty() && config.isDebugEnabled()) {
            LOGGER.info(String.format(
                    "[%s] Evicted %d stale entries from cache (unused for %d minutes)",
                    name, toEvict.size(), config.getCacheMaxAge().toMinutes()
            ));
        }
    }

    public void evictStale() {
        evictStaleEntries();
    }

    public CacheStats getStats() {
        long now = System.currentTimeMillis();

        int totalEntries = cache.size();
        int pendingCount = pendingChanges.size();

        long oldestAccess = lastAccessTime.values().stream()
                .min(Long::compare)
                .orElse(now);

        long oldestAgeMinutes = (now - oldestAccess) / 60000;

        return new CacheStats(totalEntries, pendingCount, oldestAgeMinutes);
    }

    public record CacheStats(
            int cachedEntries,
            int pendingChanges,
            long oldestEntryAgeMinutes
    ) {
        @Override
        public String toString() {
            return String.format(
                    "Cache: %d entries, %d pending, oldest: %d min",
                    cachedEntries, pendingChanges, oldestEntryAgeMinutes
            );
        }
    }

    private void flushIfNeeded() {
        if (pendingChanges.isEmpty()) {
            return;
        }

        Map<K, PendingChange<K, V>> toSave = new HashMap<>(pendingChanges);
        pendingChanges.clear();

        try {
            long startTime = System.currentTimeMillis();

            dataSaver.save(toSave);

            long duration = System.currentTimeMillis() - startTime;

            if (config.isDebugEnabled()) {
                LOGGER.info(String.format(
                        "[%s] Flushed %d changes to DB in %dms",
                        name, toSave.size(), duration
                ));
            }

        } catch (Exception e) {
            LOGGER.severe(String.format(
                    "[%s] Flush failed: %s - Changes will be retried",
                    name, e.getMessage()
            ));

            pendingChanges.putAll(toSave);
        }
    }

    public void flush() {
        flushIfNeeded();
    }

    public void shutdown() {
        isShutdown = true;

        LOGGER.info(String.format("[%s] Shutting down... Flushing %d changes", name, pendingChanges.size()));

        flush();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info(String.format("[%s] Shutdown complete", name));
    }

    @Getter
    public static class PendingChange<K, V> {
        private final K key;
        private final V delta;
        private final long timestamp;
        private final boolean isSet; // true = SET, false = MERGE/DELTA

        public PendingChange(K key, V delta, long timestamp) {
            this(key, delta, timestamp, false);
        }

        public PendingChange(K key, V delta, long timestamp, boolean isSet) {
            this.key = key;
            this.delta = delta;
            this.timestamp = timestamp;
            this.isSet = isSet;
        }
    }

    @FunctionalInterface
    public interface DataLoader<K, V> {
        Map<K, V> load(Set<K> keys) throws Exception;
    }

    @FunctionalInterface
    public interface DataSaver<K, V> {
        void save(Map<K, PendingChange<K, V>> changes) throws Exception;
    }

    @FunctionalInterface
    public interface ValueMerger<V> {
        V merge(V existing, V delta);
    }
}