package cz.johnslovakia.gameapi.levelSystem;

import cz.johnslovakia.gameapi.users.GamePlayer;
import cz.johnslovakia.gameapi.utils.rewards.Reward;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DailyMeter {

    private final List<DailyMeterTier> tiers = new ArrayList<>();

    @Setter
    private Reward afterMaxTierReward;
    @Setter
    private int maxTier = 7;

    public DailyMeter() {
        //this.afterMaxTierReward = new Reward(new RewardItem(Resource.get))
    }

    public int getXpOnCurrentTier(GamePlayer gamePlayer) {
        int dailyXp = gamePlayer.getPlayerData().getDailyXP();
        int xpSpent = 0;

        for (DailyMeterTier tier : tiers) {
            if (dailyXp < xpSpent + tier.neededXP()) {
                return dailyXp - xpSpent;
            }
            xpSpent += tier.neededXP();
        }

        return tiers.isEmpty() ? 0 : tiers.get(tiers.size() - 1).neededXP();
    }

    public DailyMeterTier getCurrentTier(GamePlayer gamePlayer) {
        int dailyXp = gamePlayer.getPlayerData().getDailyXP();
        int accumulatedXP = 0;
        DailyMeterTier currentTier = null;

        for (DailyMeterTier tier : tiers) {
            accumulatedXP += tier.neededXP();
            if (dailyXp >= accumulatedXP) {
                currentTier = tier;
            } else {
                break;
            }
        }

        return currentTier;
    }

    //TODO: přidat kontrolování jestli je levelů jako maximální počet
    public void addDailyMeterLevel(int neededXP, Reward reward){
        tiers.add(new DailyMeterTier(tiers.size() + 1, neededXP, reward));
    }

    public record DailyMeterTier(int tier, int neededXP, Reward reward) {}

}
