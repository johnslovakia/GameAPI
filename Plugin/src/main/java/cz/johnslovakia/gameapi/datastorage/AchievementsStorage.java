package cz.johnslovakia.gameapi.datastorage;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.achievements.PlayerAchievementData;
import cz.johnslovakia.gameapi.users.quests.PlayerQuestData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;

public class AchievementsStorage {


    public static JSONObject toJSON(GamePlayer gamePlayer){
        JSONArray achievementsArray = convertListToJsonArray(gamePlayer.getPlayerData().getAchievementData());

        JSONObject achievementsObject = new JSONObject();
        achievementsObject.put("achievemnets", achievementsArray);

        return achievementsObject;
    }

    public static JSONArray convertListToJsonArray(List<PlayerAchievementData> list) {
        JSONArray jsonArray = new JSONArray();

        int i = 0;
        for (PlayerAchievementData achievementData : list) {
            JSONObject achievementJson = new JSONObject();

            achievementJson.put("name", achievementData.getAchievement().getName());
            achievementJson.put("status", achievementData.getStatus().name());
            achievementJson.put("progress", achievementData.getProgress());

            jsonArray.put(achievementJson);
            i++;
        }
        return jsonArray;
    }
}
