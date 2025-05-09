package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.users.GamePlayer;

import java.util.HashMap;
import java.util.Map;

public class ResourcesManager {

    public static Integer getEarned(GamePlayer gamePlayer, Resource rewardType){
        return gamePlayer.getPlayerData().getScores().stream()
                .mapToInt(score -> score.getEarned(rewardType))
                .sum();
    }

    public static boolean earnedSomething(GamePlayer gamePlayer){
        return gamePlayer.getPlayerData().getScores().stream()
                .anyMatch(score -> score.getEarned().values().stream().anyMatch(v -> v != 0));
    }

    public static Map<Resource, Integer> rewardToHashMap(Resource rewardType, Integer reward){
        Map<Resource, Integer> map = new HashMap<>();
        map.put(rewardType, reward);
        return map;
    }
}