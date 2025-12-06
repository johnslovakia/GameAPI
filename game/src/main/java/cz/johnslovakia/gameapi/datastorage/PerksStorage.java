package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.modules.perks.Perk;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class PerksStorage {

    public static JSONObject perksToJSON(GamePlayer gamePlayer){
        Map<Perk, Integer> emptyMap = Collections.emptyMap();
        JSONArray levels;
        if (gamePlayer.getPlayerData().getPerksLevel() != null){
            levels = convertMapToJsonArray(gamePlayer.getPlayerData().getPerksLevel());
        }else{
            levels = convertMapToJsonArray(emptyMap);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("levels", levels);

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
