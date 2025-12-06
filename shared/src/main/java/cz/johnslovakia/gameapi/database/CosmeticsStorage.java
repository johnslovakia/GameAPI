package cz.johnslovakia.gameapi.database;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.modules.cosmetics.Cosmetic;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsCategory;
import cz.johnslovakia.gameapi.modules.cosmetics.CosmeticsModule;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CosmeticsStorage {


    public static JSONObject toJSON(PlayerIdentity playerIdentity, Map<CosmeticsCategory, Cosmetic> selected, List<Cosmetic> purchased){
        List<Cosmetic> emptyList = Collections.emptyList();

        JSONArray purchasedCosmetics = convertListToJsonArray(purchased != null ? purchased : emptyList);
        JSONArray selectedCosmetics;
        if (selected == null){
            selectedCosmetics = convertListToJsonArray(emptyList);
        }else{
            selectedCosmetics = convertListToJsonArray(selected);
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
            cosmeticJson.put("category", ModuleManager.getModule(CosmeticsModule.class).getCategory(cosmetic).getName());
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
            Cosmetic cosmetic = ModuleManager.getModule(CosmeticsModule.class).getCosmetic(category, name);
            list.add(cosmetic);
        }
        return list;
    }
}
