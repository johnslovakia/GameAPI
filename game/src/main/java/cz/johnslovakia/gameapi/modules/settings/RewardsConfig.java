package cz.johnslovakia.gameapi.modules.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.johnslovakia.gameapi.database.JSConfigs;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

public class RewardsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final Map<String, RewardsConfig> CACHE = new HashMap<>();
    private static final Object CACHE_LOCK = new Object();

    public static RewardsConfig get(String dbKey, DefaultsConfigurer defaultsConfigurer) {
        synchronized (CACHE_LOCK) {
            RewardsConfig cached = CACHE.get(dbKey);
            if (cached != null) return cached;
            return loadOrCreate(dbKey, defaultsConfigurer);
        }
    }

    public static void invalidate(String dbKey) {
        synchronized (CACHE_LOCK) {
            CACHE.remove(dbKey);
        }
    }

    private Map<String, List<ResourceReward>> rewards = new LinkedHashMap<>();
    private transient String dbKey;
    private transient DefaultsConfigurer defaultsConfigurer;

    @FunctionalInterface
    public interface DefaultsConfigurer {
        void configure(DefaultsBuilder builder);
    }

    private static RewardsConfig loadOrCreate(String dbKey, DefaultsConfigurer defaultsConfigurer) {
        String json = new JSConfigs().loadConfig(dbKey);

        RewardsConfig cfg;
        if (json != null) {
            cfg = GSON.fromJson(json, RewardsConfig.class);
            if (cfg.rewards == null) cfg.rewards = new LinkedHashMap<>();
            cfg.rewards.replaceAll((k, v) -> v == null ? new ArrayList<>() : new ArrayList<>(v));
        } else {
            cfg = new RewardsConfig();
        }

        cfg.dbKey = dbKey;
        cfg.defaultsConfigurer = defaultsConfigurer;
        cfg.fillMissingDefaults(defaultsConfigurer);
        registerSource(cfg);
        save(cfg);
        return cfg;
    }

    public static void save(RewardsConfig cfg) {
        registerSource(cfg);
        if (SettingsEditSession.deferSave(cfg.sourceId())) {
            synchronized (CACHE_LOCK) {
                CACHE.put(cfg.dbKey, cfg);
            }
            return;
        }
        saveNow(cfg);
    }

    private static void saveNow(RewardsConfig cfg) {
        synchronized (CACHE_LOCK) {
            CACHE.put(cfg.dbKey, cfg);
        }
        new JSConfigs().saveConfig(cfg.dbKey, GSON.toJson(cfg));
    }

    public List<ResourceReward> getRewards(String eventKey) {
        return rewards.computeIfAbsent(eventKey, k -> new ArrayList<>());
    }

    public void addReward(String eventKey, String resourceName, int amount) {
        getRewards(eventKey).add(new ResourceReward(resourceName, amount));
    }

    public void removeReward(String eventKey, int index) {
        List<ResourceReward> list = rewards.get(eventKey);
        if (list != null && index >= 0 && index < list.size()) list.remove(index);
    }

    public void adjustAmount(String eventKey, int index, int delta) {
        List<ResourceReward> list = rewards.get(eventKey);
        if (list == null || index < 0 || index >= list.size()) return;
        ResourceReward old = list.get(index);
        list.set(index, new ResourceReward(old.getResourceName(), Math.max(0, old.getAmount() + delta)));
    }

    public void setRewards(String eventKey, List<ResourceReward> list) {
        rewards.put(eventKey, new ArrayList<>(list));
    }

    public void resetMode(String prefix) {
        RewardsConfig def = buildDefaults(defaultsConfigurer);
        rewards.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        def.rewards.forEach((k, v) -> { if (k.startsWith(prefix)) rewards.put(k, new ArrayList<>(v)); });
    }

    public void resetEventToDefault(String eventKey) {
        RewardsConfig def = buildDefaults(defaultsConfigurer);
        setRewards(eventKey, def.getRewards(eventKey));
    }

    private static void registerSource(RewardsConfig cfg) {
        if (cfg == null || cfg.dbKey == null) {
            return;
        }
        SettingsEditSession.registerSource(
                cfg.sourceId(),
                "Game Rewards",
                cfg::snapshotJson,
                cfg::restoreSnapshot,
                () -> saveNow(cfg)
        );
    }

    private String sourceId() {
        return "rewards-settings:" + dbKey;
    }

    private String snapshotJson() {
        return GSON.toJson(this);
    }

    private void restoreSnapshot(String json) {
        RewardsConfig restored = GSON.fromJson(json, RewardsConfig.class);
        if (restored == null) {
            return;
        }

        this.rewards = new LinkedHashMap<>();
        if (restored.rewards != null) {
            restored.rewards.forEach((key, value) ->
                    this.rewards.put(key, value == null ? new ArrayList<>() : new ArrayList<>(value)));
        }

        synchronized (CACHE_LOCK) {
            CACHE.put(dbKey, this);
        }
    }

    private void fillMissingDefaults(DefaultsConfigurer configurer) {
        buildDefaults(configurer).rewards.forEach((k, v) -> rewards.putIfAbsent(k, new ArrayList<>(v)));
    }

    private static RewardsConfig buildDefaults(DefaultsConfigurer configurer) {
        RewardsConfig cfg = new RewardsConfig();
        configurer.configure(new DefaultsBuilder(cfg));
        return cfg;
    }
    public static final class DefaultsBuilder {
        private final RewardsConfig cfg;

        DefaultsBuilder(RewardsConfig cfg) { this.cfg = cfg; }

        public DefaultsBuilder put(String eventKey, Object... pairs) {
            List<ResourceReward> list = new ArrayList<>();
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                list.add(new ResourceReward((String) pairs[i], ((Number) pairs[i + 1]).intValue()));
            }
            cfg.rewards.put(eventKey, list);
            return this;
        }
    }

    @Getter @NoArgsConstructor
    public static class ResourceReward {
        private String resourceName;
        @Setter private int amount;

        public ResourceReward(String resourceName, int amount) {
            this.resourceName = resourceName;
            this.amount = amount;
        }

        @Override
        public String toString() { return resourceName + "×" + amount; }
    }
}
