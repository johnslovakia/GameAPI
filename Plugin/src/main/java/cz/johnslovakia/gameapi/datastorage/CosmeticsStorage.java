package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.GameAPI;
import cz.johnslovakia.gameapi.Minigame;
import cz.johnslovakia.gameapi.game.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.game.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.users.GamePlayer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CosmeticsStorage {


    public static JSONObject toJSON(GamePlayer gamePlayer){
        List<Cosmetic> emptyList = Collections.emptyList();

        JSONArray purchasedCosmetics = convertListToJsonArray(gamePlayer.getPlayerData().getPurchasedCosmetics());
        JSONArray selectedCosmetics;
        if (gamePlayer.getPlayerData().getSelectedCosmetics() == null){
            selectedCosmetics = convertListToJsonArray(emptyList);
        }else{
            selectedCosmetics = convertListToJsonArray(gamePlayer.getPlayerData().getSelectedCosmetics());
        }

        JSONObject cosmetics = new JSONObject();
        cosmetics.put("purchased", purchasedCosmetics);
        cosmetics.put("selected", selectedCosmetics);

        return cosmetics;
    }

    public static JSONArray convertListToJsonArray(List<Cosmetic> cosmetics) {
        JSONArray jsonArray = new JSONArray();
        for (Cosmetic cosmetic : cosmetics) {
            JSONObject cosmeticJson = new JSONObject();
            cosmeticJson.put("name", cosmetic.getName());
            cosmeticJson.put("category", Minigame.getInstance().getCosmeticsManager().getCategory(cosmetic).getName());
            jsonArray.put(cosmeticJson);
        }
        return jsonArray;
    }

    public static JSONArray convertListToJsonArray(Map<CosmeticsCategory, Cosmetic> cosmetics) {
        JSONArray jsonArray = new JSONArray();
        for (CosmeticsCategory category : cosmetics.keySet()) {
            Cosmetic cosmetic = cosmetics.get(category);

            JSONObject cosmeticJson = new JSONObject();
            cosmeticJson.put("name", cosmetic.getName());
            cosmeticJson.put("category", category.getName());
            jsonArray.put(cosmeticJson);
        }
        return jsonArray;
    }

    public static List<Cosmetic> parseJsonArrayToList(JSONArray jsonArray) {
        List<Cosmetic> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject cosmeticJson = jsonArray.getJSONObject(i);
            String name = cosmeticJson.getString("name");
            String category = cosmeticJson.getString("category");
            Cosmetic cosmetic = Minigame.getInstance().getCosmeticsManager().getCosmetic(category, name);
            list.add(cosmetic);
        }
        return list;
    }
}
