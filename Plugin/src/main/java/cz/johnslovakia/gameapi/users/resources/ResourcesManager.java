package cz.johnslovakia.gameapi.users.resources;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;
import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;

import java.util.HashMap;
import java.util.Map;

public class ResourcesManager {

    public static Integer getEarned(GamePlayer gamePlayer, Resource rewardType){
        int earned = 0;

        for (PlayerScore score : PlayerManager.getScoresByPlayer(gamePlayer)) {
            earned = earned + score.getEarned(rewardType);
        }
        return earned;
    }

    public static boolean earnedSomething(GamePlayer gamePlayer){
        for (PlayerScore score : PlayerManager.getScoresByPlayer(gamePlayer)) {
            for (Resource resource : score.getEarned().keySet()){
                if (score.getEarned(resource) != 0){
                    return true;
                }
            }
        }
        return false;
    }

    public static Map<Resource, Integer> rewardToHashMap(Resource rewardType, Integer reward){
        Map<Resource, Integer> map = new HashMap<>();
        map.put(rewardType, reward);
        return map;
    }
}