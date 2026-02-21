package cz.johnslovakia.gameapi.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class RefreshableCache<K, V> {

    private static final Logger LOGGER = Logger.getLogger(RefreshableCache.class.getName());

    private final String name;
    private final Duration refreshInterval;
    private final boolean autoRefresh;
    private final boolean debugEnabled;

    private final Map<K, CachedEntry<V>> cache = new ConcurrentHashMap<>();
    private final Map<K, DataSupplier<K, V>> dataSuppliers = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler;
    private volatile boolean isShutdown = false;

    private RefreshableCache(@NotNull Builder<K, V> builder) {
        this.name = builder.name;
        this.refreshInterval = builder.refreshInterval;
        this.autoRefresh = builder.autoRefresh;
        this.debugEnabled = builder.debugEnabled;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RefreshableCache-" + name);
            t.setDaemon(true);
            return t;
        });

        if (autoRefresh) {
            scheduler.scheduleAtFixedRate(
                    this::refreshExpiredEntries,
                    refreshInterval.toMillis(),
                    refreshInterval.toMillis(),
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Nullable
    public V get(@NotNull K key, @NotNull Supplier<V> supplier) {
        CachedEntry<V> entry = cache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            if (debugEnabled) {
                LOGGER.fine(String.format("[%s] Cache HIT for %s", name, key));
            }
            return entry.getValue();
        }

        if (debugEnabled) {
            LOGGER.fine(String.format("[%s] Cache MISS for %s - reloading", name, key));
        }

        return reload(key, supplier);
    }

    @Nullable
    public V getIfPresent(@NotNull K key) {
        CachedEntry<V> entry = cache.get(key);
        return (entry != null && !entry.isExpired()) ? entry.getValue() : null;
    }

    public void register(@NotNull K key, @NotNull DataSupplier<K, V> supplier) {
        dataSuppliers.put(key, supplier);
        
        if (autoRefresh) {
            reload(key, () -> supplier.load(key));
        }
    }

    @Nullable
    public V reload(@NotNull K key, @NotNull Supplier<V> supplier) {
        try {
            long startTime = System.currentTimeMillis();
            V value = supplier.get();
            long duration = System.currentTimeMillis() - startTime;

            cache.put(key, new CachedEntry<>(value, refreshInterval));

            if (debugEnabled) {
                LOGGER.info(String.format("[%s] Reloaded %s in %dms", name, key, duration));
            }

            return value;
        } catch (Exception e) {
            LOGGER.severe(String.format("[%s] Failed to reload %s: %s", name, key, e.getMessage()));
            return null;
        }
    }

    public void refreshAll() {
        for (Map.Entry<K, DataSupplier<K, V>> entry : dataSuppliers.entrySet()) {
            K key = entry.getKey();
            DataSupplier<K, V> supplier = entry.getValue();
            reload(key, () -> supplier.load(key));
        }
    }

    private void refreshExpiredEntries() {
        int refreshed = 0;
        
        for (Map.Entry<K, DataSupplier<K, V>> entry : dataSuppliers.entrySet()) {
            K key = entry.getKey();
            CachedEntry<V> cached = cache.get(key);
            
            if (cached == null || cached.isExpired()) {
                reload(key, () -> entry.getValue().load(key));
                refreshed++;
            }
        }

        if (debugEnabled && refreshed > 0) {
            LOGGER.info(String.format("[%s] Auto-refreshed %d expired entries", name, refreshed));
        }
    }

    @NotNull
    public Optional<Duration> getTimeUntilRefresh(@NotNull K key) {
        CachedEntry<V> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        long remainingMillis = entry.getExpiresAt() - System.currentTimeMillis();
        return Optional.of(Duration.ofMillis(Math.max(0, remainingMillis)));
    }

    public void invalidate(@NotNull K key) {
        cache.remove(key);
        dataSuppliers.remove(key);
    }

    public void clear() {
        cache.clear();
        dataSuppliers.clear();
    }

    public CacheStats getStats() {
        int total = cache.size();
        int expired = 0;
        int registered = dataSuppliers.size();

        for (CachedEntry<V> entry : cache.values()) {
            if (entry.isExpired()) {
                expired++;
            }
        }

        return new CacheStats(total, expired, registered);
    }

    public void shutdown() {
        isShutdown = true;
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info(String.format("[%s] Shutdown complete", name));
    }

    public static <K, V> Builder<K, V> builder(@NotNull String name) {
        return new Builder<>(name);
    }

    public static class Builder<K, V> {
        private final String name;
        private Duration refreshInterval = Duration.ofMinutes(5);
        private boolean autoRefresh = true;
        private boolean debugEnabled = false;

        private Builder(String name) {
            this.name = name;
        }

        public Builder<K, V> refreshInterval(Duration interval) {
            this.refreshInterval = interval;
            return this;
        }

        public Builder<K, V> refreshIntervalMinutes(long minutes) {
            this.refreshInterval = Duration.ofMinutes(minutes);
            return this;
        }

        public Builder<K, V> autoRefresh(boolean enabled) {
            this.autoRefresh = enabled;
            return this;
        }

        public Builder<K, V> debugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
            return this;
        }

        public RefreshableCache<K, V> build() {
            return new RefreshableCache<>(this);
        }
    }

    @Getter
    private static class CachedEntry<V> {
        private final V value;
        private final long expiresAt;

        public CachedEntry(V value, Duration ttl) {
            this.value = value;
            this.expiresAt = System.currentTimeMillis() + ttl.toMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    @FunctionalInterface
    public interface DataSupplier<K, V> {
        V load(K key);
    }

    public record CacheStats(
            int totalEntries,
            int expiredEntries,
            int registeredSuppliers
    ) {
        @Override
        public String toString() {
            return String.format(
                    "Cache: %d entries (%d expired), %d registered suppliers",
                    totalEntries, expiredEntries, registeredSuppliers
            );
        }
    }
}