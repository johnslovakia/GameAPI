package cz.johnslovakia.gameapi.levelSystem;

import cz.johnslovakia.gameapi.utils.rewards.Reward;

public record LevelRange(int startLevel, int endLevel, int neededXP, Reward reward) {
    public int getLength() {
        return endLevel - startLevel + 1;
    }
}