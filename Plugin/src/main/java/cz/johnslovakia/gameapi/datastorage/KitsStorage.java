package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import org.bukkit.inventory.Inventory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class KitsStorage {

    public static JSONObject inventoriesToJSON(GamePlayer gamePlayer){
        JSONArray inventories = convertMapToJsonArray(gamePlayer.getPlayerData().getKitInventories());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inventories", inventories);

        return jsonObject;
    }

    public static JSONArray convertMapToJsonArray(Map<Kit, Inventory> inventories) {
        JSONArray jsonArray = new JSONArray();
        for (Kit kit : inventories.keySet()) {
            Inventory inventory = inventories.get(kit);

            JSONObject inventoryJson = new JSONObject();
            inventoryJson.put("name", kit.getName());
            inventoryJson.put("inventory", BukkitSerialization.toBase64(inventory));
            jsonArray.put(inventoryJson);
        }
        return jsonArray;
    }
}
