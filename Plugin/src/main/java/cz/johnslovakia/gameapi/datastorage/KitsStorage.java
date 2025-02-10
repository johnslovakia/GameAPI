package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.game.GameManager;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.kit.KitManager;
import cz.johnslovakia.gameapi.game.map.GameMap;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import cz.johnslovakia.gameapi.utils.Logger;
import org.bukkit.inventory.Inventory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitsStorage {

    public static JSONObject inventoriesToJSON(GamePlayer gamePlayer){
        Map<Kit, Inventory> inventories = gamePlayer.getPlayerData().getKitInventories();
        if (KitManager.getKitManagers().size() > 1){
            Map<String, List<Kit>> mapKits = new HashMap<>();
            for (Kit kit : inventories.keySet()) {
                mapKits.computeIfAbsent(kit.getKitManager().getGameMap(), key -> new ArrayList<>()).add(kit);
            }

            JSONObject kitsData = new JSONObject();

            JSONObject mapsJson = new JSONObject();
            for (String map : mapKits.keySet()) {
                JSONObject mapJson = new JSONObject();
                JSONObject kitInventoriesJson = new JSONObject();

                List<Kit> kits = mapKits.get(map);
                for (Kit kit : kits) {
                    String kitName = kit.getName();
                    Inventory inventory = inventories.get(kit);
                    if (inventory != null) {
                        kitInventoriesJson.put(kitName, BukkitSerialization.toBase64(inventory));
                    }
                }

                mapJson.put("inventories", kitInventoriesJson);
                mapsJson.put(map, mapJson);
            }

            kitsData.put("maps", mapsJson);
            return new JSONObject().put("kits_data", kitsData);
        }else {

            JSONArray jsonArray = new JSONArray();
            for (Kit kit : inventories.keySet()) {
                Inventory inventory = inventories.get(kit);

                JSONObject inventoryJson = new JSONObject();
                inventoryJson.put("name", kit.getName());
                inventoryJson.put("inventory", BukkitSerialization.toBase64(inventory));
                jsonArray.put(inventoryJson);
            }


            JSONObject jsonObject = new JSONObject();
            jsonObject.put("inventories", jsonArray);

            return jsonObject;
        }
    }
}
