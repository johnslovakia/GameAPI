package cz.johnslovakia.gameapi.modules.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.johnslovakia.gameapi.database.JSConfigs;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class MainSettingsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public interface LocalConfigProvider {
        FileConfiguration getConfig();
        void saveConfig();
    }

    static final Map<String, MainSettingsConfig> CACHE = new HashMap<>();
    private static final Object CACHE_LOCK = new Object();

    public static MainSettingsConfig get(String dbKey,
                                         List<MinigameSettingDefinition> definitions,
                                         LocalConfigProvider localCfg,
                                         String localKeysPath) {
        synchronized (CACHE_LOCK) {
            MainSettingsConfig cached = CACHE.get(dbKey);
            if (cached != null) return cached;
            return loadOrCreate(dbKey, definitions, localCfg, localKeysPath);
        }
    }

    public static void invalidate(String dbKey) {
        synchronized (CACHE_LOCK) {
            CACHE.remove(dbKey);
        }
    }

    private Map<String, Integer> integers = new LinkedHashMap<>();
    private Map<String, Boolean> booleans = new LinkedHashMap<>();

    private transient String dbKey;
    private transient String localKeysPath;
    private transient LocalConfigProvider localCfg;

    private static MainSettingsConfig loadOrCreate(String dbKey, List<MinigameSettingDefinition> definitions, LocalConfigProvider localCfg, String localKeysPath) {
        String json = new JSConfigs().loadConfig(dbKey);

        MainSettingsConfig cfg;
        if (json != null) {
            cfg = GSON.fromJson(json, MainSettingsConfig.class);
            if (cfg.integers == null) cfg.integers = new LinkedHashMap<>();
            if (cfg.booleans == null) cfg.booleans = new LinkedHashMap<>();
        } else {
            cfg = new MainSettingsConfig();
        }

        cfg.dbKey = dbKey;
        cfg.localKeysPath = localKeysPath;
        cfg.localCfg = localCfg;
        cfg.fillMissingDefaults(definitions);
        registerSource(cfg);
        save(cfg);
        return cfg;
    }

    public static void save(MainSettingsConfig cfg) {
        registerSource(cfg);
        if (SettingsEditSession.deferSave(cfg.sourceId())) {
            synchronized (CACHE_LOCK) {
                CACHE.put(cfg.dbKey, cfg);
            }
            return;
        }
        saveNow(cfg);
    }

    private static void saveNow(MainSettingsConfig cfg) {
        synchronized (CACHE_LOCK) {
            CACHE.put(cfg.dbKey, cfg);
        }
        new JSConfigs().saveConfig(cfg.dbKey, GSON.toJson(cfg));
    }

    public static void resetToDefault(String dbKey, List<MinigameSettingDefinition> definitions, LocalConfigProvider localCfg, String localKeysPath) {
        FileConfiguration config = localCfg.getConfig();
        List<String> localKeys = new ArrayList<>(config.getStringList(localKeysPath));
        definitions.forEach(d -> localKeys.add(d.getKey()));
        new LinkedHashSet<>(localKeys).forEach(k -> config.set(k, null));
        config.set(localKeysPath, null);

        MainSettingsConfig fresh = new MainSettingsConfig();
        fresh.dbKey = dbKey;
        fresh.localKeysPath = localKeysPath;
        fresh.localCfg = localCfg;
        fresh.fillMissingDefaults(definitions);

        MainSettingsConfig target;
        synchronized (CACHE_LOCK) {
            target = CACHE.get(dbKey);
            if (target == null) {
                target = fresh;
            } else {
                target.copyFrom(fresh);
                target.dbKey = dbKey;
                target.localKeysPath = localKeysPath;
                target.localCfg = localCfg;
            }
            CACHE.put(dbKey, target);
        }

        registerSource(target);
        target.saveLocal();
        save(target);
    }

    public int getInt(String key, int def) {
        int global = integers.getOrDefault(key, def);
        return isLocal(key) ? localCfg.getConfig().getInt(key, global) : global;
    }

    public boolean getBoolean(String key, boolean def) {
        boolean global = booleans.getOrDefault(key, def);
        return isLocal(key) ? localCfg.getConfig().getBoolean(key, global) : global;
    }

    public void setInt(String key, int value) {
        if (isLocal(key)) { localCfg.getConfig().set(key, value); saveLocal(); }
        else integers.put(key, value);
    }

    public void setBoolean(String key, boolean value) {
        if (isLocal(key)) { localCfg.getConfig().set(key, value); saveLocal(); }
        else booleans.put(key, value);
    }

    public boolean isLocal(String key) {
        return getLocalKeys().contains(key);
    }

    public void setLocalStorage(String key, boolean local) {
        List<String> localKeys = getLocalKeys();
        if (local && !localKeys.contains(key)) {
            localKeys.add(key);
            if (integers.containsKey(key)) localCfg.getConfig().set(key, integers.get(key));
            else if (booleans.containsKey(key)) localCfg.getConfig().set(key, booleans.get(key));
        } else if (!local && localKeys.contains(key)) {
            localKeys.remove(key);
            localCfg.getConfig().set(key, null);
        }
        localCfg.getConfig().set(localKeysPath, localKeys);
        saveLocal();
    }

    private static void registerSource(MainSettingsConfig cfg) {
        if (cfg == null || cfg.dbKey == null) {
            return;
        }
        SettingsEditSession.registerSource(
                cfg.sourceId(),
                "Game Settings",
                cfg::snapshotJson,
                cfg::restoreSnapshot,
                cfg::saveAllNow
        );
    }

    private String sourceId() {
        return "main-settings:" + dbKey;
    }

    private String snapshotJson() {
        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.integers = new LinkedHashMap<>(integers);
        snapshot.booleans = new LinkedHashMap<>(booleans);
        snapshot.localKeys = getLocalKeys();
        snapshot.localIntegers = new LinkedHashMap<>();
        snapshot.localBooleans = new LinkedHashMap<>();

        FileConfiguration config = localCfg.getConfig();
        for (String key : integers.keySet()) {
            if (config.contains(key)) {
                snapshot.localIntegers.put(key, config.getInt(key));
            }
        }
        for (String key : booleans.keySet()) {
            if (config.contains(key)) {
                snapshot.localBooleans.put(key, config.getBoolean(key));
            }
        }
        return GSON.toJson(snapshot);
    }

    private void restoreSnapshot(String json) {
        ConfigSnapshot snapshot = GSON.fromJson(json, ConfigSnapshot.class);
        if (snapshot == null) {
            return;
        }

        this.integers = snapshot.integers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(snapshot.integers);
        this.booleans = snapshot.booleans == null ? new LinkedHashMap<>() : new LinkedHashMap<>(snapshot.booleans);

        FileConfiguration config = localCfg.getConfig();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(integers.keySet());
        keys.addAll(booleans.keySet());
        keys.addAll(getLocalKeys());
        if (snapshot.localKeys != null) {
            keys.addAll(snapshot.localKeys);
        }

        keys.forEach(k -> config.set(k, null));
        if (snapshot.localIntegers != null) {
            snapshot.localIntegers.forEach(config::set);
        }
        if (snapshot.localBooleans != null) {
            snapshot.localBooleans.forEach(config::set);
        }
        config.set(localKeysPath, snapshot.localKeys == null ? new ArrayList<>() : new ArrayList<>(snapshot.localKeys));

        synchronized (CACHE_LOCK) {
            CACHE.put(dbKey, this);
        }
    }

    private void copyFrom(MainSettingsConfig other) {
        this.integers = new LinkedHashMap<>(other.integers);
        this.booleans = new LinkedHashMap<>(other.booleans);
    }

    private void saveLocal() {
        registerSource(this);
        if (SettingsEditSession.deferSave(sourceId())) {
            return;
        }
        saveLocalNow();
    }

    private void saveAllNow() {
        saveNow(this);
        saveLocalNow();
    }

    private void saveLocalNow() {
        localCfg.saveConfig();
    }

    private void fillMissingDefaults(List<MinigameSettingDefinition> definitions) {
        for (MinigameSettingDefinition def : definitions) {
            if (def.getType() == MinigameSettingDefinition.Type.INT)
                integers.putIfAbsent(def.getKey(), def.getDefaultInt());
            else
                booleans.putIfAbsent(def.getKey(), def.getDefaultBool());
        }
    }

    private List<String> getLocalKeys() {
        return new ArrayList<>(localCfg.getConfig().getStringList(localKeysPath));
    }

    private static final class ConfigSnapshot {
        private Map<String, Integer> integers = new LinkedHashMap<>();
        private Map<String, Boolean> booleans = new LinkedHashMap<>();
        private List<String> localKeys = new ArrayList<>();
        private Map<String, Integer> localIntegers = new LinkedHashMap<>();
        private Map<String, Boolean> localBooleans = new LinkedHashMap<>();
    }
}
