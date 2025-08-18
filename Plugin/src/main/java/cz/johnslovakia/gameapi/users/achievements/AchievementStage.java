package cz.johnslovakia.gameapi.users.achievements;

import cz.johnslovakia.gameapi.utils.rewards.Reward;
import cz.johnslovakia.gameapi.utils.rewards.RewardItem;

public record AchievementStage(int stage, int goal, int points) {

    public Reward getReward() {
        return new Reward(new RewardItem("Achievement Points", points));
    }
}