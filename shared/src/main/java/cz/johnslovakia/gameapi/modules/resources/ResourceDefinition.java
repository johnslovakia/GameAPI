package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.Core;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ResourceDefinition {

    static final String STORAGE_PLAYER_TABLE_JSON = "PLAYER_TABLE_JSON";
    static final String STORAGE_DEFERRED_VAULT = "DEFERRED_VAULT";

    private final String name;
    private final String displayName;
    private final String color;
    private final int rank;
    private final int firstDailyWinReward;
    private final boolean applicableBonus;
    private final String imgChar;
    private final String storage;
    private final String tableName;
    private final List<String> migrateFromBatchedTables;

    public ResourceDefinition(String name, String displayName, ChatColor color, int rank, int firstDailyWinReward, boolean applicableBonus,
            String imgChar, String storage, String tableName, List<String> migrateFromBatchedTables) {
        this.name = name;
        this.displayName = displayName;
        this.color = color != null ? color.name() : ChatColor.GOLD.name();
        this.rank = rank;
        this.firstDailyWinReward = firstDailyWinReward;
        this.applicableBonus = applicableBonus;
        this.imgChar = imgChar;
        this.storage = storage;
        this.tableName = tableName;
        this.migrateFromBatchedTables = migrateFromBatchedTables == null
                ? Collections.emptyList()
                : List.copyOf(migrateFromBatchedTables);
    }

    public ChatColor getColor() {
        try {
            return ChatColor.valueOf(color);
        } catch (Exception ignored) {
            return ChatColor.GOLD;
        }
    }

    public Resource toResource() {
        Resource.Builder builder = Resource.builder(name)
                .color(getColor())
                .rank(rank)
                .firstDailyWinReward(firstDailyWinReward)
                .applicableBonus(applicableBonus);

        if (displayName != null) {
            builder.displayName(displayName);
        }
        if (imgChar != null) {
            builder.imgChar(imgChar);
        }

        if (STORAGE_PLAYER_TABLE_JSON.equals(storage)) {
            builder.global();
            if (!migrateFromBatchedTables.isEmpty()) {
                builder.migrateFromBatched(migrateFromBatchedTables.toArray(new String[0]));
            }
        } else if (STORAGE_DEFERRED_VAULT.equals(storage)) {
            builder.deferredVault(tableName != null ? tableName : "gameapi_playertable", Core.getInstance().getPlugin());
        } else {
            return null;
        }

        return builder.build();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        if (displayName != null) json.put("displayName", displayName);
        json.put("color", color);
        json.put("rank", rank);
        json.put("firstDailyWinReward", firstDailyWinReward);
        json.put("applicableBonus", applicableBonus);
        if (imgChar != null) json.put("imgChar", imgChar);
        json.put("storage", storage);
        if (tableName != null) json.put("tableName", tableName);

        JSONArray migrations = new JSONArray();
        for (String table : migrateFromBatchedTables) {
            migrations.put(table);
        }
        json.put("migrateFromBatchedTables", migrations);
        return json;
    }

    public static ResourceDefinition fromJson(JSONObject json) {
        if (json == null) return null;

        String name = json.optString("name", null);
        if (name == null || name.isBlank()) return null;

        List<String> migrations = new ArrayList<>();
        JSONArray migrationArray = json.optJSONArray("migrateFromBatchedTables");
        if (migrationArray != null) {
            for (int i = 0; i < migrationArray.length(); i++) {
                String table = migrationArray.optString(i, null);
                if (table != null && !table.isBlank()) {
                    migrations.add(table);
                }
            }
        }

        return new ResourceDefinition(
                name,
                nullableString(json, "displayName"),
                parseColor(json.optString("color", ChatColor.GOLD.name())),
                json.optInt("rank", 0),
                json.optInt("firstDailyWinReward", 0),
                json.optBoolean("applicableBonus", false),
                nullableString(json, "imgChar"),
                json.optString("storage", STORAGE_PLAYER_TABLE_JSON),
                nullableString(json, "tableName"),
                migrations
        );
    }

    private static ChatColor parseColor(String raw) {
        try {
            return ChatColor.valueOf(raw);
        } catch (Exception ignored) {
            return ChatColor.GOLD;
        }
    }

    private static String nullableString(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) return null;
        String value = json.optString(key, null);
        return value == null || value.isBlank() ? null : value;
    }
}
