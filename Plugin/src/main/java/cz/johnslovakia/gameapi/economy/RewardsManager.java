package cz.johnslovakia.gameapi.economy;

import cz.johnslovakia.gameapi.users.PlayerManager;
import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.users.PlayerScore;

import java.util.HashMap;
import java.util.Map;

public class RewardsManager {



    public static Integer getEarned(GamePlayer gamePlayer, Economy rewardType){
        int earned = 0;

        for (PlayerScore score : PlayerManager.getScoresByPlayer(gamePlayer)) {
            earned = earned + score.getEarned(rewardType);
        }
        return earned;
    }

    public static boolean earnedSomething(GamePlayer gamePlayer){
        for (PlayerScore score : PlayerManager.getScoresByPlayer(gamePlayer)) {
            if (score.getRewardTypes() != null) {
                if (!score.getRewardTypes().isEmpty()) {
                    for (Economy type : score.getRewardTypes().keySet()) {
                        if (score.getEarned(type) != 0){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Map<Economy, Integer> rewardToHashMap(Economy rewardType, Integer reward){
        Map<Economy, Integer> map = new HashMap<>();
        map.put(rewardType, reward);
        return map;
    }
}