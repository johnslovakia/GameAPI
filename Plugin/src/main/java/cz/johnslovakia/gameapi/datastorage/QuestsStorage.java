package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalTime;
import java.util.List;

public class QuestsStorage {


    public static JSONObject toJSON(GamePlayer gamePlayer){
        JSONArray quests = convertListToJsonArray(gamePlayer.getPlayerData().getQuestData());

        JSONObject cosmetics = new JSONObject();
        cosmetics.put("quests", quests);

        return cosmetics;
    }

    public static JSONArray convertListToJsonArray(List<PlayerQuestData> list) {
        JSONArray jsonArray = new JSONArray();

        int i = 0;
        for (PlayerQuestData questData : list) {
            JSONObject questJson = new JSONObject();

            questJson.put("slot", i + 1);
            questJson.put("name", questData.getQuest().getName());
            questJson.put("type", questData.getQuest().getType().name());
            questJson.put("status", questData.getStatus().name());
            questJson.put("progress", questData.getProgress());

            if (questData.getCompletionDate() != null) {
                questJson.put("completion_date", questData.getCompletionDate().toString());
            } else {
                questJson.put("completion_date", LocalTime.now());
            }

            jsonArray.put(questJson);
            i++;
        }
        return jsonArray;
    }
}
