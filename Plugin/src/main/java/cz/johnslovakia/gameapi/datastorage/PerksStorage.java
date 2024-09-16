package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.game.kit.Kit;
import cz.johnslovakia.gameapi.game.perk.Perk;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.BukkitSerialization;
import org.bukkit.inventory.Inventory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class PerksStorage {

    public static JSONObject perksToJSON(GamePlayer gamePlayer){
        JSONArray inventories = convertMapToJsonArray(gamePlayer.getPlayerData().getPerksLevel());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("levels", inventories);

        return jsonObject;
    }

    public static JSONArray convertMapToJsonArray(Map<Perk, Integer> levels) {
        JSONArray jsonArray = new JSONArray();
        for (Perk perk : levels.keySet()) {
            Integer level = levels.get(perk);

            JSONObject levelsJson = new JSONObject();
            levelsJson.put("name", perk.getName());
            levelsJson.put("level", level);
            jsonArray.put(levelsJson);
        }
        return jsonArray;
    }
}
