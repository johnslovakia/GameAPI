package cz.johnslovakia.gameapi.modules.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.utils.ConfigAPI;
import me.zort.containr.ContextClickInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ClickContext {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public final Player player;
    public final ContextClickInfo clickInfo;

    public ClickContext(Player player, ContextClickInfo clickInfo) {
        this.player = player;
        this.clickInfo = clickInfo;
    }

    public int delta(int left, int right, int shiftLeft, int shiftRight) {
        if (clickInfo.getClickType().isShiftClick() && clickInfo.getClickType().isRightClick()) return shiftRight;
        if (clickInfo.getClickType().isShiftClick()) return shiftLeft;
        if (clickInfo.getClickType().isRightClick()) return right;
        return left;
    }

    public void adjustInt(ConfigAPI config, String key, int delta, int min, int max) {
        int next = Math.max(min, Math.min(max, config.getConfig().getInt(key) + delta));
        config.getConfig().set(key, next);
        saveYaml(config);
    }

    public void adjustInt(ConfigAPI config, String key, int delta) {
        adjustInt(config, key, delta, 0, Integer.MAX_VALUE);
    }

    public void toggleBool(ConfigAPI config, String key) {
        config.getConfig().set(key, !config.getConfig().getBoolean(key));
        saveYaml(config);
    }

    public void set(ConfigAPI config, String key, Object value) {
        config.getConfig().set(key, value);
        saveYaml(config);
    }

    public int getInt(ConfigAPI config, String key) { return config.getConfig().getInt(key); }
    public boolean getBool(ConfigAPI config, String key) { return config.getConfig().getBoolean(key); }

    public void cycleString(ConfigAPI config, String key, String[] values) {
        String current = config.getConfig().getString(key, values[0]);
        int idx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) { idx = i; break; }
        }
        config.getConfig().set(key, values[(idx + 1) % values.length]);
        saveYaml(config);
    }

    private void saveYaml(ConfigAPI config) {
        try {
            config.saveConfig();
        } catch (Exception ignored) {}
    }


    public <T> T loadJson(String configName, Class<T> type) {
        String json = new JSConfigs().loadConfig(configName);
        if (json == null) return null;
        return GSON.fromJson(json, type);
    }

    public void saveJson(String configName, Object object) {
        new JSConfigs().saveConfig(configName, GSON.toJson(object));
    }
}