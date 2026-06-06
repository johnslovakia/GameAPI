package cz.johnslovakia.gameapi.modules.resources;

import cz.johnslovakia.gameapi.database.JSConfigs;
import cz.johnslovakia.gameapi.utils.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ResourceRegistry {

    private static final String CONFIG_KEY = "Resources";

    private ResourceRegistry() {
    }

    public static synchronized List<ResourceDefinition> loadDefinitions() {
        String raw = new JSConfigs().loadConfig(CONFIG_KEY);
        if (raw == null || raw.isBlank()) return List.of();

        try {
            JSONArray resources;
            String trimmed = raw.trim();
            if (trimmed.startsWith("[")) {
                resources = new JSONArray(trimmed);
            } else {
                JSONObject root = new JSONObject(trimmed);
                resources = root.optJSONArray("resources");
                if (resources == null) return List.of();
            }

            List<ResourceDefinition> definitions = new ArrayList<>();
            for (int i = 0; i < resources.length(); i++) {
                ResourceDefinition definition = ResourceDefinition.fromJson(resources.optJSONObject(i));
                if (definition != null) {
                    definitions.add(definition);
                }
            }
            return definitions;
        } catch (Exception e) {
            Logger.log("Failed to load resource registry from JSConfigs.", Logger.LogType.ERROR);
            e.printStackTrace();
            return List.of();
        }
    }

    public static synchronized void saveDefinition(ResourceDefinition definition) {
        if (definition == null) return;

        Map<String, ResourceDefinition> definitionsByName = new LinkedHashMap<>();
        for (ResourceDefinition existing : loadDefinitions()) {
            definitionsByName.put(normalize(existing.getName()), existing);
        }

        definitionsByName.put(normalize(definition.getName()), definition);
        saveDefinitions(new ArrayList<>(definitionsByName.values()));
    }

    private static void saveDefinitions(List<ResourceDefinition> definitions) {
        JSONObject root = new JSONObject();
        JSONArray resources = new JSONArray();
        for (ResourceDefinition definition : definitions) {
            resources.put(definition.toJson());
        }
        root.put("resources", resources);
        new JSConfigs().saveConfig(CONFIG_KEY, root.toString());
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
