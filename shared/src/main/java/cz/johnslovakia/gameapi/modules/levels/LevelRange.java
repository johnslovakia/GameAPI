package cz.johnslovakia.gameapi.modules.levels;

import cz.johnslovakia.gameapi.rewards.Reward;

public record LevelRange(int startLevel, int endLevel, int neededXP, Reward reward) {
    public int getLength() {
        return endLevel - startLevel + 1;
    }
}